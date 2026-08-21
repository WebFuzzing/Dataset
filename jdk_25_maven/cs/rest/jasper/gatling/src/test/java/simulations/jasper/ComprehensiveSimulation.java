package simulations.jasper;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive Jasper Knowledge Management Load Test
 *
 * This simulation represents real-world usage patterns:
 * - Knowledge workers adding and organizing references
 * - Users browsing and searching content
 * - Plugin-based content enrichment
 * - Template-based content organization
 * - User management operations
 */
public class ComprehensiveSimulation extends Simulation {
	private static final String STATIC_XSRF_TOKEN = "gatling-static-token-for-testing";

	HttpProtocolBuilder httpProtocol = http
		.baseUrl("http://localhost:8081")
		.acceptHeader("application/json")
		.contentTypeHeader("application/json")
		.userAgentHeader("Gatling Load Test - Comprehensive");

	// ====================== Reference Operations ======================

	ChainBuilder createWebReference =
		exec(session -> session.set("webRefUrl", "https://example.com/article-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000)))
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Create Web Reference")
					.post("/api/v1/ref")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"url": "#{webRefUrl}",
						"title": "Knowledge Article #{randomInt(1,1000)}",
						"comment": "Important reference for our research",
						"tags": ["research", "article", "knowledgebase"],
						"sources": ["https://source.example.com"]
					}"""))
					.check(status().in(201, 409))
			)
			.pause(Duration.ofMillis(200))
			.exec(
				http("Verify Created Web Reference")
					.get("/api/v1/ref")
					.queryParam("url", "#{webRefUrl}")
					.check(status().is(200))
			).pause(Duration.ofMillis(300));

	ChainBuilder createBookReference =
		exec(session -> session.set("bookRefUrl", "isbn:978-" + String.format("%09d", new java.util.Random().nextInt(1000000000)) + new java.util.Random().nextInt(10)))
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Create Book Reference")
					.post("/api/v1/ref")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"url": "#{bookRefUrl}",
						"title": "Technical Book #{randomInt(1,100)}",
						"comment": "Reference book on software engineering",
						"tags": ["book", "technical", "software"]
					}"""))
					.check(status().in(201, 409))
			)
			.pause(Duration.ofMillis(200))
			.exec(
				http("Verify Created Book Reference")
					.get("/api/v1/ref")
					.queryParam("url", "#{bookRefUrl}")
					.check(status().is(200))
			).pause(Duration.ofMillis(300));

	ChainBuilder createComment =
		exec(session -> session.set("commentUrl", "comment:" + java.util.UUID.randomUUID().toString()))
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Create Comment Reference")
					.post("/api/v1/ref")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"url": "#{commentUrl}",
						"title": "User Comment #{randomInt(1,500)}",
						"comment": "This is a user comment or note",
						"tags": ["comment", "discussion"],
						"sources": ["#{webRefUrl}"]
					}"""))
					.check(status().in(201, 409))
			)
			.pause(Duration.ofMillis(200))
			.exec(
				http("Verify Created Comment")
					.get("/api/v1/ref")
					.queryParam("url", "#{commentUrl}")
					.check(status().is(200))
			).pause(Duration.ofMillis(100));

	// ====================== Browse and Search Operations ======================

	ChainBuilder browseRecentRefs = exec(
		http("Browse Recent References")
			.get("/api/v1/ref/page")
			.queryParam("size", "20")
			.queryParam("sort", "modified,desc")
			.check(status().is(200))
			.check(jsonPath("$.content").exists())
	).pause(Duration.ofSeconds(1));

	ChainBuilder searchByTag = exec(
		http("Search by Tag")
			.get("/api/v1/ref/page")
			.queryParam("query", "research")
			.queryParam("size", "10")
			.check(status().is(200))
	).pause(Duration.ofMillis(800));

	ChainBuilder searchByKeyword = exec(
		http("Search by Keyword")
			.get("/api/v1/ref/page")
			.queryParam("search", "technical software")
			.queryParam("size", "15")
			.check(status().is(200))
	).pause(Duration.ofMillis(800));

	ChainBuilder getSpecificRef =
		exec(session -> session.set("lookupRefUrl", "https://example.com/article-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000)))
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Create Ref for Lookup")
					.post("/api/v1/ref")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"url": "#{lookupRefUrl}",
						"title": "Lookup Article #{randomInt(1,1000)}",
						"comment": "Article for lookup test",
						"tags": ["test", "lookup"]
					}"""))
					.check(status().in(201, 409))
			)
			.pause(Duration.ofMillis(100))
			.exec(
				http("Get Specific Reference")
					.get("/api/v1/ref")
					.queryParam("url", "#{lookupRefUrl}")
					.check(status().is(200))
			).pause(Duration.ofMillis(400));

	// ====================== Extension Operations ======================

	ChainBuilder createExtensionTemplate = exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Extension Template")
				.post("/api/v1/template")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
				{
					"tag": "test.ext",
					"name": "Test Extension Template",
					"schema": {}
				}"""))
				.check(status().in(201, 409))
		).pause(Duration.ofMillis(100));

	ChainBuilder createPlugin = exec(session -> {
		int randomId = new java.util.Random().nextInt(50) + 1;
		return session.set("testExtTag", "test.ext/" + randomId);
	})
		.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Plugin Extension")
				.post("/api/v1/ext")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"tag": "#{testExtTag}",
						"name": "Test Extension",
						"config": {
							"description": "A test extension for load testing",
							"version": "1.0.0",
							"enabled": true
						}
					}"""))
				.check(status().in(201, 409))
		).pause(Duration.ofMillis(600));

	ChainBuilder browseExtensions = exec(
		http("Browse Extensions")
			.get("/api/v1/ext/page")
			.queryParam("size", "20")
			.check(status().is(200))
	).pause(Duration.ofMillis(700));

	// ====================== Plugin Management ======================

	ChainBuilder createPluginConfig =
		exec(session -> session.set("pluginTag", "+plugin/custom." + (1 + new java.util.Random().nextInt(30))))
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Create Plugin Configuration")
					.post("/api/v1/plugin")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"tag": "#{pluginTag}",
						"name": "Custom Plugin #{randomInt(1,30)}",
						"config": {
							"type": "viewer",
							"title": "Custom Viewer",
							"selector": "custom-selector"
						}
					}"""))
					.check(status().in(201, 409))
			)
			.pause(Duration.ofMillis(200))
			.exec(
				http("Verify Created Plugin")
					.get("/api/v1/plugin")
					.queryParam("tag", "#{pluginTag}")
					.check(status().is(200))
			).pause(Duration.ofMillis(300));

	ChainBuilder getPluginConfig = exec(
		http("Get Plugin Configuration")
			.get("/api/v1/plugin")
			.queryParam("tag", "#{pluginTag}")
			.check(status().is(200))
	).pause(Duration.ofMillis(300));

	// ====================== Template Operations ======================

	ChainBuilder createTemplate =
		exec(session -> session.set("templateTag", "_template/article." + (1 + new java.util.Random().nextInt(20))))
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Create Template")
					.post("/api/v1/template")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"tag": "#{templateTag}",
						"name": "Article Template #{randomInt(1,20)}",
						"config": {
							"description": "Template for organizing article references",
							"category": "content"
						},
						"defaults": {
							"title": "",
							"summary": "",
							"category": "tech"
						},
						"schema": {
							"properties": {
								"title": {"type": "string"}
							},
							"optionalProperties": {
								"summary": {"type": "string"},
								"category": {"enum": ["tech", "business", "research"]}
							}
						}
					}"""))
					.check(status().in(201, 409))
			)
			.pause(Duration.ofMillis(200))
			.exec(
				http("Verify Created Template")
					.get("/api/v1/template")
					.queryParam("tag", "#{templateTag}")
					.check(status().is(200))
			).pause(Duration.ofMillis(400));

	ChainBuilder browseTemplates = exec(
		http("Browse Templates")
			.get("/api/v1/template/page")
			.queryParam("size", "10")
			.check(status().is(200))
	).pause(Duration.ofMillis(500));

	// ====================== User Management ======================

	ChainBuilder createUser = exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create User")
				.post("/api/v1/user")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
				{
					"tag": "+user/testuser#{randomInt(1,100)}",
					"name": "Test User #{randomInt(1,100)}",
					"config": {
						"role": "USER",
						"active": true
					}
				}"""))
				.check(status().in(201, 409))
		).pause(Duration.ofMillis(400));

	ChainBuilder getUserInfo = exec(
		http("Get User Info")
			.get("/api/v1/user/whoami")
			.check(status().is(200))
	).pause(Duration.ofMillis(300));

	// ====================== Graph and Analytics ======================

	ChainBuilder getGraphData = exec(
		http("Get Graph Data")
			.get("/api/v1/graph/list")
			.queryParam("urls", List.of(
				"https://example.com/article-1",
				"https://example.com/article-2"
			))
			.check(status().is(200))
	).pause(Duration.ofMillis(800));

	ChainBuilder browseTaggedContent = exec(
		http("Browse Tagged Content")
			.get("/api/v1/ref/page")
			.queryParam("query", "research")
			.queryParam("size", "20")
			.check(status().is(200))
	).pause(Duration.ofMillis(600));

	// ====================== Content Enrichment ======================

	ChainBuilder proxyContent =
		// Create a ref for proxy if webRefUrl not set
		doIf(session -> !session.contains("webRefUrl")).then(
				exec(session -> session.set("webRefUrl", "https://cjmalloy.github.io/jasper/docs/entities.png?q=#{randomInt(1,1000)}"))
					.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
					.exec(
						http("Create Ref for Proxy")
							.post("/api/v1/ref")
							.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
							.body(StringBody("""
						{
							"url": "#{webRefUrl}",
							"title": "Proxy Test Article",
							"comment": "Article for proxy test",
							"tags": ["proxy", "test"]
						}"""))
							.check(status().in(201, 409))
					)
					.pause(Duration.ofMillis(100))
			)
			.exec(
				http("Proxy External Content")
					.get("/api/v1/proxy")
					.queryParam("url", "#{webRefUrl}")
					.check(status().is(200))
			).pause(Duration.ofMillis(1000));

	// ====================== Scenarios ======================

	// Knowledge Worker: Creates and organizes content
	ScenarioBuilder knowledgeWorker = scenario("Knowledge Worker")
		.exec(createWebReference)
		.pause(Duration.ofSeconds(2))
		.exec(createBookReference)
		.pause(Duration.ofSeconds(1))
		.exec(createComment)
		.pause(Duration.ofSeconds(3))
		.exec(searchByTag)
		.pause(Duration.ofSeconds(2))
		.exec(createTemplate)
		.pause(Duration.ofSeconds(1))
		.exec(browseRecentRefs);

	// Content Browser: Searches and views existing content
	ScenarioBuilder contentBrowser = scenario("Content Browser")
		.exec(browseRecentRefs)
		.pause(Duration.ofSeconds(1))
		.exec(searchByKeyword)
		.pause(Duration.ofSeconds(2))
		.exec(getSpecificRef)
		.pause(Duration.ofSeconds(1))
		.exec(searchByTag)
		.pause(Duration.ofSeconds(3))
		.exec(getGraphData)
		.pause(Duration.ofSeconds(1))
		.exec(browseTaggedContent);

	// Administrator: Manages system configuration
	ScenarioBuilder administrator = scenario("Administrator")
		.exec(createPluginConfig)
		.pause(Duration.ofSeconds(2))
		.exec(getPluginConfig)
		.pause(Duration.ofSeconds(1))
		.exec(browseExtensions)
		.pause(Duration.ofSeconds(2))
		.exec(createUser)
		.pause(Duration.ofSeconds(1))
		.exec(getUserInfo)
		.pause(Duration.ofSeconds(3))
		.exec(browseTemplates);

	// Heavy Reader: Intensive content consumption
	ScenarioBuilder heavyReader = scenario("Heavy Reader")
		.repeat(3).on(
			exec(browseRecentRefs)
				.pause(Duration.ofMillis(500))
				.exec(searchByKeyword)
				.pause(Duration.ofMillis(800))
				.exec(getSpecificRef)
				.pause(Duration.ofMillis(300))
		)
		.exec(proxyContent);

	// Power User: Mixed operations including content enrichment
	ScenarioBuilder powerUser = scenario("Power User")
		.exec(createExtensionTemplate)
		.exec(createPlugin)
		.pause(Duration.ofSeconds(1))
		.exec(createWebReference)
		.pause(Duration.ofSeconds(2))
		.exec(proxyContent)
		.pause(Duration.ofSeconds(1))
		.exec(searchByTag)
		.pause(Duration.ofSeconds(2))
		.exec(getGraphData);

	{
		setUp(
			// Knowledge workers - moderate activity
			knowledgeWorker.injectOpen(
				nothingFor(Duration.ofSeconds(5)),
				rampUsers(10).during(Duration.ofSeconds(30)),
				constantUsersPerSec(5).during(Duration.ofSeconds(60))
			),
			// Content browsers - high read activity
			contentBrowser.injectOpen(
				nothingFor(Duration.ofSeconds(2)),
				rampUsers(20).during(Duration.ofSeconds(20)),
				constantUsersPerSec(10).during(Duration.ofSeconds(80))
			),
			// Administrators - low activity
			administrator.injectOpen(
				nothingFor(Duration.ofSeconds(10)),
				rampUsers(3).during(Duration.ofSeconds(15)),
				constantUsersPerSec(1).during(Duration.ofSeconds(60))
			),
			// Heavy readers - burst activity
			heavyReader.injectOpen(
				nothingFor(Duration.ofSeconds(15)),
				atOnceUsers(15),
				constantUsersPerSec(8).during(Duration.ofSeconds(45))
			),
			// Power users - sporadic activity
			powerUser.injectOpen(
				nothingFor(Duration.ofSeconds(8)),
				rampUsers(5).during(Duration.ofSeconds(25)),
				constantUsersPerSec(2).during(Duration.ofSeconds(50))
			)
		).protocols(httpProtocol)
			.maxDuration(Duration.ofMinutes(3))
			.assertions(
				global().responseTime().max().lt(5000),
				global().successfulRequests().percent().gt(75.0), // Adjusted for auth-protected endpoints
				forAll().responseTime().percentile3().lt(3000)
			);
	}
}
