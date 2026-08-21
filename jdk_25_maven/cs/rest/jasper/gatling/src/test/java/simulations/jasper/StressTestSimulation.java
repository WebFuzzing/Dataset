package simulations.jasper;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

/**
 * Stress Test and Edge Cases for Jasper
 *
 * This simulation tests system limits and edge cases:
 * - High-volume concurrent operations
 * - Large payloads and complex queries
 * - Error conditions and recovery
 * - Replication and backup scenarios
 * - System administration operations
 */
public class StressTestSimulation extends Simulation {
	private static final String STATIC_XSRF_TOKEN = "gatling-static-token-for-testing";

	HttpProtocolBuilder httpProtocol = http
		.baseUrl("http://localhost:8081")
		.acceptHeader("application/json")
		.contentTypeHeader("application/json")
		.userAgentHeader("Gatling Stress Test")
		.disableFollowRedirect()
		.check(status().not(500)); // Allow other error codes for stress testing

	// ====================== High Volume Operations ======================

	ChainBuilder rapidRefCreation =
		exec(session -> {
			// Add timestamp to URL for uniqueness to avoid duplicate key violations
			String url = "https://stress-test.example.com/" +
				System.currentTimeMillis() + "-" +
				(1 + new java.util.Random().nextInt(100000));
			return session.set("rapidRefUrl", url);
		})
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Rapid Ref Creation")
					.post("/api/v1/ref")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"url": "#{rapidRefUrl}",
						"title": "Stress Test Reference #{randomInt(1,100000)} - Load#{randomInt(10000,99999)}",
						"comment": "StressTest#{randomInt(100000,999999)} - This is a stress test reference with substantial content to test system limits and performance under high load conditions. Content#{randomInt(100000,999999)}",
						"tags": [
							"stresstest",
							"performance",
							"load.#{randomInt(1,1000)}",
							"batch.#{randomInt(1,100)}",
							"category.#{randomInt(1,50)}",
							"priority.#{randomInt(1,10)}"
						],
						"sources": [
							"https://source1.example.com/#{randomInt(1,1000)}",
							"https://source2.example.com/#{randomInt(1,1000)}",
							"https://source3.example.com/#{randomInt(1,1000)}"
						]
					}"""))
					.check(status().in(429, 503, 201, 409, 400))
			)
			.pause(Duration.ofMillis(50), Duration.ofMillis(200));

	ChainBuilder largePageQuery = exec(
		http("Large Page Query")
			.get("/api/v1/ref/page")
			.queryParam("size", "100")
			.queryParam("query", "stresstest|performance|load.#{randomInt(1,100)}")
			.queryParam("sort", "modified,desc")
			.check(status().in(429, 503, 200))
			.check(responseTimeInMillis().lt(5000))
	)
		.pause(Duration.ofMillis(100), Duration.ofMillis(500));

	ChainBuilder complexSearchQuery =
		exec(
			http("Complex Search Query")
				.get("/api/v1/ref/page")
				.queryParam("query", "(stresstest:performance)|load.#{randomInt(1,100)}:!excluded")
				.queryParam("search", "stress test performance load content#{randomInt(1,100)}")
				.queryParam("modifiedAfter", "2024-01-01T00:00:00Z")
				.queryParam("size", "50")
				.check(status().in(429, 503, 200))
		)
			.pause(Duration.ofMillis(200), Duration.ofMillis(800));

	// ====================== Large Payload Operations ======================

	ChainBuilder createLargeExtTemplate = exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Large Extension Template")
				.post("/api/v1/template")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"tag": "large.test",
						"name": "Large Test Extension Template",
						"schema": {
							"properties": {
								"type": {"type": "string"}
							},
							"optionalProperties": {
								"data": {
									"elements": {
										"properties": {
											"id": {"type": "uint32"},
											"name": {"type": "string"},
											"description": {"type": "string"},
											"value": {"type": "float64"}
										}
									}
								}
							}
						}
					}"""))
				.check(status().in(429, 503, 201, 409))
		)
		.pause(Duration.ofMillis(100));

	ChainBuilder createLargeExt = exec(session -> {
		// Build large config JSON
		StringBuilder largeConfig = new StringBuilder("{\"type\":\"large-test\",\"data\":[");
		for (int i = 0; i < 100; i++) {
			if (i > 0) largeConfig.append(",");
			largeConfig.append("{\"id\":").append(i)
				.append(",\"name\":\"item").append(i).append("_").append(System.currentTimeMillis() % 10000)
				.append("\",\"description\":\"desc").append(i).append("_").append(System.currentTimeMillis() % 100000)
				.append("\",\"value\":").append(Math.random() * 1000).append("}");
		}
		largeConfig.append("]}");

		int randomId = (int)(Math.random() * 10000) + 1;

		String body = String.format("""
			{
				"tag": "large.test/%d",
				"name": "Large Test Extension %d",
				"config": %s
			}""",
			randomId,
			randomId,
			largeConfig.toString());

		return session.set("largeExtBody", body);
	})
		.tryMax(3).on(
			exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
				.exec(
					http("Create Large Extension")
						.post("/api/v1/ext")
						.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
						.body(StringBody("#{largeExtBody}"))
						.check(status().in(429, 503, 201, 409))
				)
		).exitHereIfFailed()
		.pause(Duration.ofMillis(300), Duration.ofMillis(1000));

	// ====================== Error Condition Testing ======================

	ChainBuilder testNotFoundErrors = exec(
		http("Test Not Found - Random URL")
			.get("/api/v1/ref")
			.queryParam("url", "https://nonexistent-#{randomInt(1,100000)}.example.com/#{randomUuid()}")
			.check(status().in(429, 503, 404))
	)
		.pause(Duration.ofMillis(100), Duration.ofMillis(300));

	ChainBuilder testInvalidData = exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Test Invalid Data")
				.post("/api/v1/ref")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"url": "invalid-url-test#{randomInt(1,1000)}",
						"title": "",
						"tags": ["invalidtag#{randomInt(100000,999999)}"]
					}"""))
				.check(status().in(429, 503, 400, 422))
		)
		.pause(Duration.ofMillis(100), Duration.ofMillis(300));

	ChainBuilder testMalformedJson = exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Test Malformed JSON")
				.post("/api/v1/ref")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"url": "https://test.com/malformed",
						"title": "Test"
						"invalid": "json",
					}"""))
				.check(status().in(429, 503, 400))
		)
		.pause(Duration.ofMillis(100), Duration.ofMillis(300));

	// ====================== Replication Testing ======================

	ChainBuilder testReplicationEndpoints = exec(
		http("Test Replication - Get Refs")
			.get("/pub/api/v1/repl/ref")
			.queryParam("size", "100")
			.queryParam("modifiedAfter", "2024-01-01T00:00:00Z")
			.check(status().in(429, 503, 200))
	)
		.pause(Duration.ofMillis(200), Duration.ofMillis(500))
		.exec(
			http("Test Replication - Get Cursor")
				.get("/pub/api/v1/repl/ref/cursor")
				.check(status().in(429, 503, 200))
		)
		.pause(Duration.ofMillis(100), Duration.ofMillis(300))
		.exec(
			http("Test Replication - Get Extensions")
				.get("/pub/api/v1/repl/ext")
				.queryParam("size", "50")
				.check(status().in(429, 503, 200))
		);

	// ====================== Backup Operations ======================

	ChainBuilder testBackupOperations = exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Backup")
				.post("/api/v1/backup")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
					{
						"includeRefs": true,
						"includeExts": true,
						"includeUsers": false,
						"maxItems": 1000
					}"""))
				.check(status().in(429, 503, 200))  // BackupController returns 200, not 201 (missing @ResponseStatus annotation)
		)
		.pause(Duration.ofSeconds(2), Duration.ofSeconds(5))
		.exec(
			http("List Backups")
				.get("/api/v1/backup")
				.check(status().in(429, 503, 200))
		);

	// ====================== System Administration ======================

	ChainBuilder testAdminOperations = exec(
		http("Get User Info")
			.get("/api/v1/user/whoami")
			.check(status().in(429, 503, 200))
	)
		.pause(Duration.ofMillis(200), Duration.ofMillis(500))
		.exec(
			http("Browse All Users")
				.get("/api/v1/user/page")
				.queryParam("size", "50")
				.check(status().in(429, 503, 200))
		);

	// ====================== Content Enrichment Stress ======================

	ChainBuilder stressProxyOperations = exec(
		http("Stress Proxy Operation")
			.get("/api/v1/proxy")
			.queryParam("url", "https://cjmalloy.github.io/jasper/docs/entities.png?q=#{randomInt(1,1000)}")
			.check(status().in(429, 503, 200))
			.check(responseTimeInMillis().lt(10000))
	)
		.pause(Duration.ofMillis(500), Duration.ofMillis(2000));

	ChainBuilder stressScrapeOperations = exec(
		http("Stress Web Scraping")
			.get("/api/v1/scrape/web")
			.queryParam("url", "https://httpbin.org/html")
			.check(status().in(429, 503, 200))
	)
		.pause(Duration.ofMillis(1000), Duration.ofMillis(3000));

	// ====================== Concurrent Update Stress ======================

	ChainBuilder concurrentUpdates =
		exec(session -> {
			// Use timestamp + random to make URLs unique per virtual user to avoid duplicate key violations
			String url = "https://stress-test.example.com/shared-" +
				System.currentTimeMillis() + "-" +
				(1 + new java.util.Random().nextInt(10000));
			return session.set("stressUpdateUrl", url);
		})
			// Create the ref for this specific virtual user
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Create Ref for Concurrent Update")
					.post("/api/v1/ref")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
						{
							"url": "#{stressUpdateUrl}",
							"title": "Stress Test Reference",
							"comment": "Ref for testing concurrent updates",
							"tags": ["stresstest", "concurrent"]
						}"""))
					.check(status().in(429, 503, 201))
					.check(jsonPath("$").optional().saveAs("stressRefModified"))
			)
			.doIf(session -> session.contains("stressRefModified")).then(
				exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
					.exec(
						http("Concurrent Update Attempt")
							.patch("/api/v1/ref")
							.queryParam("url", "#{stressUpdateUrl}")
							.queryParam("cursor", "#{stressRefModified}")
							.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
							.header("Content-Type", "application/merge-patch+json")
							.body(StringBody("""
							{
								"tags": ["updated.#{randomInt(1,1000)}", "concurrent.#{randomLong()}", "stressupdate"],
								"comment": "Updated during stress test at #{randomLong()}"
							}"""))
							.check(status().in(429, 503, 200, 409))
					)
			).pause(Duration.ofMillis(50), Duration.ofMillis(200));

	// ====================== Graph Operations Stress ======================

	ChainBuilder stressGraphOperations = exec(session -> {
		// Build list of URLs for graph query
		java.util.List<String> urls = java.util.stream.IntStream.range(1, 21)
			.mapToObj(i -> "https://stress-test.example.com/" + (System.currentTimeMillis() + i))
			.collect(java.util.stream.Collectors.toList());
		return session.set("graphUrls", urls);
	})
		.exec(
			http("Stress Graph Query")
				.get("/api/v1/graph/list")
				.multivaluedQueryParam("urls", "#{graphUrls}")
				.check(status().in(429, 503, 200))
				.check(responseTimeInMillis().lt(8000))
		)
		.pause(Duration.ofMillis(300), Duration.ofMillis(1000));

	// ====================== Scenarios ======================

	ScenarioBuilder volumeStress = scenario("High Volume Operations")
		.group("High Volume Operations").on(
			repeat(20).on(
					exec(rapidRefCreation)
						.pace(Duration.ofMillis(100))
				)
				.exec(largePageQuery)
				.exec(complexSearchQuery)
		);

	ScenarioBuilder errorHandlingTest = scenario("Error Handling Tests")
		.group("Error Handling Tests").on(
			repeat(10).on(
				randomSwitch().on(
					percent(40.0).then(exec(testNotFoundErrors)),
					percent(30.0).then(exec(testInvalidData)),
					percent(30.0).then(exec(testMalformedJson))
				)
			)
		);

	ScenarioBuilder systemLimitsTest = scenario("System Limits Test")
		.exec(createLargeExtTemplate)
		.exec(createLargeExt)
		.pause(Duration.ofSeconds(1), Duration.ofSeconds(3))
		.exec(stressGraphOperations)
		.pause(Duration.ofSeconds(1), Duration.ofSeconds(2))
		.repeat(5).on(
			exec(concurrentUpdates).pace(Duration.ofMillis(200))
		);

	ScenarioBuilder replicationStress = scenario("Replication Stress")
		.repeat(15).on(
			exec(testReplicationEndpoints)
				.pace(Duration.ofSeconds(1))
		);

	ScenarioBuilder adminStress = scenario("Administration Stress")
		.exec(testBackupOperations)
		.pause(Duration.ofSeconds(2), Duration.ofSeconds(5))
		.repeat(8).on(
			exec(testAdminOperations)
				.pace(Duration.ofMillis(500))
		);

	ScenarioBuilder contentEnrichmentStress = scenario("Content Enrichment Stress")
		.repeat(10).on(
			randomSwitch().on(
				percent(60.0).then(exec(stressProxyOperations)),
				percent(40.0).then(exec(stressScrapeOperations))
			)
		);

	{
		setUp(
			// High volume concurrent operations
			volumeStress.injectOpen(
				nothingFor(Duration.ofSeconds(2)),
				rampUsers(150).during(Duration.ofSeconds(30)),
				constantUsersPerSec(60).during(Duration.ofMinutes(2))
			),
			// Error condition testing
			errorHandlingTest.injectOpen(
				nothingFor(Duration.ofSeconds(5)),
				rampUsers(60).during(Duration.ofSeconds(20)),
				constantUsersPerSec(30).during(Duration.ofMinutes(1))
			),
			// System limits testing
			systemLimitsTest.injectOpen(
				nothingFor(Duration.ofSeconds(10)),
				rampUsers(45).during(Duration.ofSeconds(25)),
				constantUsersPerSec(15).during(Duration.ofMinutes(2))
			),
			// Replication stress
			replicationStress.injectOpen(
				nothingFor(Duration.ofSeconds(8)),
				rampUsers(20).during(Duration.ofSeconds(15)),
				constantUsersPerSec(12).during(Duration.ofMinutes(1))
			),
			// Administration operations
			adminStress.injectOpen(
				nothingFor(Duration.ofSeconds(12)),
				rampUsers(15).during(Duration.ofSeconds(20)),
				constantUsersPerSec(6).during(Duration.ofMinutes(1))
			),
			// Content enrichment stress
			contentEnrichmentStress.injectOpen(
				nothingFor(Duration.ofSeconds(15)),
				rampUsers(24).during(Duration.ofSeconds(30)),
				constantUsersPerSec(12).during(Duration.ofMinutes(2))
			)
		).protocols(httpProtocol)
			.maxDuration(Duration.ofMinutes(4))
			.throttle(
				reachRps(150).in(Duration.ofSeconds(30)),
				holdFor(Duration.ofMinutes(2)),
				jumpToRps(120),
				holdFor(Duration.ofMinutes(1))
			)
			.assertions(
				global().responseTime().max().lt(15000),
				global().responseTime().mean().lt(3000),
				global().successfulRequests().percent().gt(70.0), // Lower success rate expected for stress testing
				details("High Volume Operations").failedRequests().percent().lt(40.0),
				details("Error Handling Tests").responseTime().percentile3().lt(5000)
			);
	}
}
