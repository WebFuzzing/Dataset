package simulations.jasper;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;
import java.util.List;

/**
 * Realistic User Journey Load Test for Jasper
 *
 * This simulation models realistic user workflows:
 * - Research Session: User researches a topic, saves references, takes notes
 * - Daily Review: User checks recent updates and organizes content
 * - Content Curation: User creates and manages templates and plugins
 * - Collaborative Work: Multiple users working on shared content
 */
public class UserJourneySimulation extends Simulation {
	private static final String STATIC_XSRF_TOKEN = "gatling-static-token-for-testing";

	HttpProtocolBuilder httpProtocol = http
		.baseUrl("http://localhost:8081")
		.acceptHeader("application/json")
		.contentTypeHeader("application/json")
		.userAgentHeader("Gatling Load Test - User Journey")
		.check(status().not(500), status().not(502), status().not(503));

	// ====================== Feeder Data ======================

	FeederBuilder<String> topicFeeder = csv("data/topics.csv").circular();

	FeederBuilder<String> sourceFeeder = csv("data/sources.csv").circular();

	// ====================== Research Session Journey ======================

	ChainBuilder researchWorkflow = feed(topicFeeder)
		.exec(session -> {
			System.out.println("User researching: " + session.getString("topic"));
			// Normalize category to lowercase for use in tags
			String category = session.getString("category").toLowerCase();
			return session.set("categoryTag", category);
		})
		// Start by searching for existing knowledge
		.exec(
			http("Search Existing Knowledge - #{topic}")
				.get("/api/v1/ref/page")
				.queryParam("search", "#{topic}")
				.queryParam("size", "20")
				.check(status().is(200))
				.check(jsonPath("$.content").saveAs("existingRefs"))
		)
		.pause(Duration.ofSeconds(2, 5))
		// Browse by category tag
		.exec(
			http("Browse by Category - #{category}")
				.get("/api/v1/ref/page")
				.queryParam("query", "#{categoryTag}")
				.queryParam("size", "15")
				.check(status().is(200))
		)
		.pause(Duration.ofSeconds(1, 3))
		// Create a new reference with research findings
		.feed(sourceFeeder)
		.exec(session -> {
			String url = "https://" + session.getString("source") + "/" + session.getString("topic") + "-research-" + System.currentTimeMillis();
			return session.set("researchUrl", url);
		})
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Save Research Reference - #{topic}")
				.post("/api/v1/ref")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"url": "#{researchUrl}",
						"title": "#{topic} Research - Study#{randomInt(1000,9999)}",
						"comment": "Research findings on #{topic} from #{source}",
						"tags": ["research", "#{categoryTag}", "#{type}"]
					}"""))
				.check(status().is(201))
				.check(jsonPath("$").saveAs("createdTimestamp"))
		)
		.pause(Duration.ofMillis(500))
		// Verify the created reference
		.exec(
			http("Verify Created Research Reference")
				.get("/api/v1/ref")
				.queryParam("url", "#{researchUrl}")
				.check(status().is(200))
				.check(jsonPath("$.url").isEL("#{researchUrl}"))
		)
		.pause(Duration.ofMillis(500))
		// Add a comment/note about the research
		.exec(session -> {
			String commentUrl = "comment:" + java.util.UUID.randomUUID().toString();
			return session.set("commentUrl", commentUrl);
		})
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Add Research Note - #{topic}")
				.post("/api/v1/ref")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"url": "#{commentUrl}",
						"title": "Research Notes: #{topic}",
						"comment": "Key insights and takeaways from #{topic} research session",
						"tags": ["note", "#{categoryTag}", "research.summary"],
						"sources": ["#{researchUrl}"]
					}"""))
				.check(status().is(201))
		)
		.pause(Duration.ofSeconds(1, 3))
		// Check if there are related templates
		.exec(
			http("Find Related Templates - #{category}")
				.get("/api/v1/template/page")
				.queryParam("query", "#{categoryTag}")
				.queryParam("size", "10")
				.check(status().is(200))
		);

	// ====================== Daily Review Journey ======================

	ChainBuilder dailyReviewWorkflow = exec(session -> {
		System.out.println("User performing daily review");
		return session;
	})
		// Check recent activity
		.exec(
			http("Check Recent Updates")
				.get("/api/v1/ref/page")
				.queryParam("size", "30")
				.queryParam("sort", "modified,desc")
				.check(status().is(200))
				.check(jsonPath("$.content[*].url").findAll().saveAs("recentUrls"))
		)
		.pause(Duration.ofSeconds(2, 4))
		// Create a ref to review if researchUrl not already set
		.doIf(session -> !session.contains("researchUrl")).then(
			feed(topicFeeder)
				.exec(session -> {
					String url = "https://dailyreview.example.com/item-" + System.currentTimeMillis();
					String topicTag = session.getString("topic").toLowerCase().replaceAll("\\s+", ".");
					return session.set("researchUrl", url)
						.set("topicTag", topicTag);
				})
				.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
				.exec(
					http("Create Review Item")
						.post("/api/v1/ref")
						.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
						.body(StringBody("""
						{
							"url": "#{researchUrl}",
							"title": "Daily Review Item #{randomInt(1,1000)}",
							"comment": "Item for daily review",
							"tags": ["review", "#{topicTag}"]
						}"""))
						.check(status().in(201, 409))
				)
				.pause(Duration.ofMillis(200))
		)
		// Review specific items
		.exec(
			http("Review Specific Item")
				.get("/api/v1/ref")
				.queryParam("url", "#{researchUrl}")
				.check(status().is(200))
		)
		.pause(Duration.ofSeconds(1, 2))
		// Look for untagged content to organize
		.exec(
			http("Find Untagged Content")
				.get("/api/v1/ref/page")
				.queryParam("untagged", "true")
				.queryParam("size", "10")
				.check(status().is(200))
		)
		.pause(Duration.ofSeconds(2, 4))
		// Fetch a ref to update (to get its cursor/modified timestamp)
		.exec(
			http("Fetch Ref for Update")
				.get("/api/v1/ref")
				.queryParam("url", "#{researchUrl}")
				.check(status().is(200))
				.check(jsonPath("$.modified").optional().saveAs("refModified"))
		)
		.pause(Duration.ofMillis(500))
		// Update a reference with additional tags (using merge-patch) - only if we got the modified timestamp
		.doIf(session -> session.contains("refModified")).then(
		exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
				http("Update Reference Tags")
					.patch("/api/v1/ref")
					.queryParam("url", "#{researchUrl}")
					.queryParam("cursor", "#{refModified}")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.header("Content-Type", "application/merge-patch+json")
					.body(StringBody("""
						{
							"tags": ["organized", "daily.review", "review#{randomInt(10000,99999)}"]
						}"""))
					.check(status().is(200))
			)
		);

	// ====================== Content Curation Journey ======================

	ChainBuilder curationWorkflow = feed(topicFeeder)
		.exec(session -> {
			System.out.println("User curating content for: " + session.getString("topic"));
			// Convert topic to lowercase and replace spaces with dots for valid tag
			String topic = session.getString("topic");
			String topicTag = topic.toLowerCase().replaceAll("\\s+", ".");
			String categoryTag = session.getString("category").toLowerCase();
			int randomId = new java.util.Random().nextInt(100) + 1;
			return session.set("topicTag", topicTag)
				.set("categoryTag", categoryTag)
				.set("templateTagValue", "_template/" + topicTag + "." + randomId);
		})
		// Create a specialized template for the topic
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Topic Template - #{topic}")
				.post("/api/v1/template")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"tag": "#{templateTagValue}",
						"name": "#{topic} Resource Template",
						"config": {
							"description": "Template for organizing #{topic} resources",
							"category": "#{category}"
						},
						"defaults": {
							"difficulty": "beginner",
							"resource_type": "tutorial",
							"quality_rating": 5
						},
						"schema": {
							"optionalProperties": {
								"difficulty": {"enum": ["beginner", "intermediate", "advanced"]},
								"resource_type": {"enum": ["tutorial", "reference", "example", "tool"]},
								"quality_rating": {"type": "float64"}
							}
						}
					}"""))
				.check(status().in(201, 409)) // 201 Created or 409 Conflict if already exists
		)
		.pause(Duration.ofSeconds(1, 2))
		// Create a plugin for enhanced functionality
		.exec(session -> {
			String topicTag = session.getString("topicTag");
			int randomId = new java.util.Random().nextInt(100) + 1;
			return session.set("pluginTagValue", "+plugin/" + topicTag + ".enhancer." + randomId);
		})
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Enhancement Plugin - #{topic}")
				.post("/api/v1/plugin")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"tag": "#{pluginTagValue}",
						"name": "#{topic} Content Enhancer",
						"config": {
							"type": "enhancer",
							"description": "Enhances #{topic} content with additional metadata",
							"selector": ".#{category}-content",
							"features": ["auto-tagging", "difficulty-detection", "related-content"]
						}
					}"""))
				.check(status().in(201, 409)) // 201 Created or 409 Conflict if already exists
		)
		.pause(Duration.ofSeconds(1, 3))
		// Check existing extensions for the topic
		.exec(
			http("Browse Topic Extensions - #{category}")
				.get("/api/v1/ext/page")
				.queryParam("query", "#{categoryTag}")
				.queryParam("size", "15")
				.check(status().is(200))
		)
		.pause(Duration.ofSeconds(1, 2))
		// Create a template for curated collections
		.exec(session -> {
			String topicTag = session.getString("topicTag");
			int randomId = new java.util.Random().nextInt(50) + 1;
			return session.set("collectionTag", "collection/" + topicTag + "." + randomId)
				.set("collectionTemplateTag", "collection");
		})
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Collection Template")
				.post("/api/v1/template")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"tag": "#{collectionTemplateTag}",
						"name": "Collection Template",
						"config": {
							"description": "Template for curated collections"
						},
						"defaults": {
							"type": "collection",
							"curator": "system",
							"criteria": {
								"quality_min": 7,
								"verified": true
							}
						},
						"schema": {
							"optionalProperties": {
								"type": {"type": "string"},
								"description": {"type": "string"},
								"curator": {"type": "string"},
								"created": {"type": "string"},
								"criteria": {
									"optionalProperties": {
										"quality_min": {"type": "float64"},
										"categories": {"elements": {"type": "string"}},
										"verified": {"type": "boolean"}
									}
								}
							}
						}
					}"""))
				.check(status().in(201, 409))
		)
		.pause(Duration.ofMillis(500))
		// Create a curated collection
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Curated Collection - #{topic}")
				.post("/api/v1/ext")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"tag": "#{collectionTag}",
						"name": "#{topic} Curated Collection",
						"config": {
							"type": "collection",
							"description": "Carefully curated resources for learning #{topic}",
							"curator": "system",
							"created": "#{randomLong()}",
							"criteria": {
								"quality_min": 7,
								"categories": ["#{category}"],
								"verified": true
							}
						}
					}"""))
				.check(status().in(201, 409)) // 201 Created or 409 Conflict if already exists
		);

	// ====================== Collaborative Work Journey ======================

	ChainBuilder collaborationWorkflow = exec(session -> {
		System.out.println("User engaging in collaborative work");
		return session;
	})
		// Check user information and permissions
		.exec(
			http("Check User Profile")
				.get("/api/v1/user/whoami")
				.check(status().is(200))
		)
		.pause(Duration.ofSeconds(1))
		// Look for shared content
		.exec(
			http("Browse Shared Content")
				.get("/api/v1/ref/page")
				.queryParam("query", "shared")
				.queryParam("size", "20")
				.check(status().is(200))
		)
		.pause(Duration.ofSeconds(1, 3))
		// Add a collaborative comment
		.exec(session -> {
			String commentUrl = "comment:collaboration-" + java.util.UUID.randomUUID().toString();
			return session.set("collabCommentUrl", commentUrl);
		})
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Add Collaborative Comment")
				.post("/api/v1/ref")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"url": "#{collabCommentUrl}",
						"title": "Team Discussion Point",
						"comment": "Adding my thoughts on this topic for team review",
						"tags": ["collaboration", "team.input", "discussion"],
						"sources": ["https://example.com/shared-doc-#{randomInt(1,20)}"]
					}"""))
				.check(status().is(201))
		)
		.pause(Duration.ofSeconds(1, 2))
		// Check graph relationships for collaborative content
		.exec(
			http("Check Collaboration Graph")
				.get("/api/v1/graph/list")
				.multivaluedQueryParam("urls", java.util.Arrays.asList(
					"https://example.com/shared-doc-1",
					"https://example.com/shared-doc-2",
					"comment:collaboration-session1000"
				))
				.check(status().is(200))
		)
		.pause(Duration.ofSeconds(1, 3))
		// Create a shared template
		.exec(session -> {
			String templateTag = "_template/team.standard." + System.currentTimeMillis();
			return session.set("teamTemplateTag", templateTag);
		})
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Shared Template")
				.post("/api/v1/template")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"tag": "#{teamTemplateTag}",
						"name": "Team Standard Template",
						"config": {
							"description": "Standardized template for team collaboration",
							"shared": true,
							"team": "development"
						},
						"defaults": {
							"assignee": "",
							"priority": "medium",
							"status": "draft"
						},
						"schema": {
							"properties": {
								"assignee": {"type": "string"}
							},
							"optionalProperties": {
								"priority": {"enum": ["low", "medium", "high", "urgent"]},
								"status": {"enum": ["draft", "review", "approved", "archived"]}
							}
						}
					}"""))
				.check(status().in(201, 409)) // 201 Created or 409 Conflict if already exists
		);

	// ====================== Scenarios ======================

	ScenarioBuilder researchSession = scenario("Research Session")
		.group("Research Session").on(
			exec(researchWorkflow)
			.pause(Duration.ofSeconds(5, 10))
			.repeat(2).on(
				exec(researchWorkflow).pause(Duration.ofSeconds(10, 20))
			)
		);

	ScenarioBuilder dailyReview = scenario("Daily Review")
		.group("Daily Review").on(
			exec(dailyReviewWorkflow)
			.pause(Duration.ofSeconds(3, 8))
		);

	ScenarioBuilder contentCuration = scenario("Content Curation")
		.exec(curationWorkflow)
		.pause(Duration.ofSeconds(8, 15));

	ScenarioBuilder collaborativeWork = scenario("Collaborative Work")
		.exec(collaborationWorkflow)
		.pause(Duration.ofSeconds(5, 12));

	// Mixed realistic user behavior
	ScenarioBuilder realisticUser = scenario("Realistic User Behavior")
		.randomSwitch().on(
			percent(40.0).then(exec(dailyReviewWorkflow)),
			percent(30.0).then(exec(researchWorkflow)),
			percent(20.0).then(exec(collaborationWorkflow)),
			percent(10.0).then(exec(curationWorkflow))
		)
		.pause(Duration.ofSeconds(10, 30));

	{
		setUp(
			// Research sessions - intensive but focused
			researchSession.injectOpen(
				nothingFor(Duration.ofSeconds(5)),
				rampUsers(8).during(Duration.ofSeconds(30)),
				constantUsersPerSec(3).during(Duration.ofMinutes(2))
			),
			// Daily reviews - regular, moderate activity
			dailyReview.injectOpen(
				nothingFor(Duration.ofSeconds(2)),
				rampUsers(15).during(Duration.ofSeconds(20)),
				constantUsersPerSec(7).during(Duration.ofMinutes(3))
			),
			// Content curation - specialized, lower frequency
			contentCuration.injectOpen(
				nothingFor(Duration.ofSeconds(10)),
				rampUsers(4).during(Duration.ofSeconds(25)),
				constantUsersPerSec(1).during(Duration.ofMinutes(2))
			),
			// Collaborative work - sporadic but important
			collaborativeWork.injectOpen(
				nothingFor(Duration.ofSeconds(8)),
				rampUsers(6).during(Duration.ofSeconds(20)),
				constantUsersPerSec(2).during(Duration.ofMinutes(2))
			),
			// Realistic mixed behavior - most common pattern
			realisticUser.injectOpen(
				nothingFor(Duration.ofSeconds(1)),
				rampUsers(25).during(Duration.ofSeconds(45)),
				constantUsersPerSec(12).during(Duration.ofMinutes(4))
			)
		).protocols(httpProtocol)
			.maxDuration(Duration.ofMinutes(5))
			.assertions(
				global().responseTime().max().lt(8000),
				global().responseTime().mean().lt(2000),
				global().successfulRequests().percent().gt(75.0),
				details("Research Session").responseTime().percentile3().lt(4000),
				details("Daily Review").responseTime().percentile3().lt(3000)
			);
	}
}
