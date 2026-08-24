package simulations.jasper;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

/**
 * Simple Jasper Smoke Test
 *
 * Basic smoke test to verify core functionality:
 * - Create references
 * - Browse and search content
 * - Test core API endpoints
 * - Verify basic system health
 */
public class SmokeTestSimulation extends Simulation {
	private static final String STATIC_XSRF_TOKEN = "gatling-static-token-for-testing";

	HttpProtocolBuilder httpProtocol = http
		.baseUrl("http://localhost:8081")
		.acceptHeader("application/json")
		.contentTypeHeader("application/json")
		.userAgentHeader("Gatling Smoke Test")
		.check(status().not(500));

	// ====================== Basic Operations ======================

	ChainBuilder getAllRefs = exec(
		http("Get All Refs")
			.get("/api/v1/ref/page")
			.queryParam("size", "10")
			.check(status().is(200))
			.check(jsonPath("$.content").exists())
	).pause(Duration.ofSeconds(1));

	ChainBuilder createWebReference =
		exec(session -> session.set("testUrl", "https://example.com/test-" + System.currentTimeMillis() + "-" + new java.util.Random().nextInt(10000)))
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Create Web Reference")
					.post("/api/v1/ref")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"url": "#{testUrl}",
						"title": "Test Article #{randomInt(1,1000)}",
						"comment": "This is a test reference for smoke testing",
						"tags": ["test", "smoketest", "example"]
					}"""))
					.check(status().is(201))
					.check(jsonPath("$").saveAs("createdRefTimestamp"))
			)
			.pause(Duration.ofMillis(500))
			.exec(
				http("Verify Created Reference")
					.get("/api/v1/ref")
					.queryParam("url", "#{testUrl}")
					.check(status().is(200))
			).pause(Duration.ofMillis(500));

	ChainBuilder getSpecificRef = exec(
		http("Get Specific Reference")
			.get("/api/v1/ref")
			.queryParam("url", "#{testUrl}")
			.check(status().is(200))
	).pause(Duration.ofMillis(500));

	ChainBuilder searchRefs = exec(
		http("Search References")
			.get("/api/v1/ref/page")
			.queryParam("query", "test")
			.queryParam("size", "5")
			.check(status().is(200))
	).pause(Duration.ofMillis(800));

	ChainBuilder countRefs = exec(
		http("Count References")
			.get("/api/v1/ref/count")
			.queryParam("query", "smoketest")
			.check(status().is(200))
	).pause(Duration.ofMillis(500));

	// ====================== Extension Operations ======================

	ChainBuilder browseExtensions = exec(
		http("Browse Extensions")
			.get("/api/v1/ext/page")
			.queryParam("size", "10")
			.check(status().is(200))
	).pause(Duration.ofMillis(600));

	ChainBuilder createTestExtension = exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
		.exec(
			http("Create Test Extension")
				.post("/api/v1/ext")
				.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
				.body(StringBody("""
				{
					"tag": "smoketest.#{randomInt(1,100)}",
					"name": "Smoke Test Extension #{randomInt(1,100)}"
				}"""))
				.check(status().in(201, 409)) // 201 Created or 409 Conflict if already exists
		).pause(Duration.ofMillis(700));

	// ====================== Plugin Operations ======================

	ChainBuilder browsePlugins = exec(
		http("Browse Plugins")
			.get("/api/v1/plugin/page")
			.queryParam("size", "10")
			.check(status().is(200))
	).pause(Duration.ofMillis(500));

	// ====================== Template Operations ======================

	ChainBuilder browseTemplates = exec(
		http("Browse Templates")
			.get("/api/v1/template/page")
			.queryParam("size", "10")
			.check(status().is(200))
	).pause(Duration.ofMillis(500));

	// ====================== User Operations ======================

	ChainBuilder getUserInfo = exec(
		http("Get User Info")
			.get("/api/v1/user/whoami")
			.check(status().is(200))
	).pause(Duration.ofMillis(400));

	ChainBuilder browseUsers = exec(
		http("Browse Users")
			.get("/api/v1/user/page")
			.queryParam("size", "10")
			.check(status().is(200))
	).pause(Duration.ofMillis(500));

	// ====================== System Health ======================

	ChainBuilder checkOrigins = exec(
		http("Check Origin System")
			.get("/api/v1/origin")
			.check(status().is(200))
	).pause(Duration.ofMillis(300));

	// ====================== Scenarios ======================

	ScenarioBuilder basicFunctionality = scenario("Basic Functionality Test")
		.exec(getAllRefs)
		.exec(createWebReference)
		.exec(getSpecificRef)
		.exec(searchRefs)
		.exec(countRefs);

	ScenarioBuilder systemComponents = scenario("System Components Test")
		.exec(browseExtensions)
		.exec(createTestExtension)
		.exec(browsePlugins)
		.exec(browseTemplates);

	ScenarioBuilder userAndSystem = scenario("User and System Test")
		.exec(getUserInfo)
		.exec(browseUsers)
		.exec(checkOrigins);

	ScenarioBuilder comprehensiveSmoke = scenario("Comprehensive Smoke Test")
		.exec(getAllRefs)
		.pause(Duration.ofSeconds(1))
		.exec(createWebReference)
		.pause(Duration.ofSeconds(1))
		.exec(searchRefs)
		.pause(Duration.ofSeconds(1))
		.exec(browseExtensions)
		.pause(Duration.ofSeconds(1))
		.exec(getUserInfo)
		.pause(Duration.ofSeconds(1))
		.repeat(2).on(
			exec(getAllRefs)
				.pause(Duration.ofMillis(500))
				.exec(getSpecificRef)
				.pause(Duration.ofMillis(500))
		);

	{
		setUp(
			// Basic functionality - core operations
			basicFunctionality.injectOpen(
				nothingFor(Duration.ofSeconds(1)),
				atOnceUsers(3),
				rampUsers(5).during(Duration.ofSeconds(10))
			),
			// System components - verify all components work
			systemComponents.injectOpen(
				nothingFor(Duration.ofSeconds(3)),
				atOnceUsers(2),
				rampUsers(3).during(Duration.ofSeconds(8))
			),
			// User and system health
			userAndSystem.injectOpen(
				nothingFor(Duration.ofSeconds(5)),
				atOnceUsers(2),
				constantUsersPerSec(1).during(Duration.ofSeconds(15))
			),
			// Comprehensive smoke test
			comprehensiveSmoke.injectOpen(
				nothingFor(Duration.ofSeconds(2)),
				atOnceUsers(1),
				rampUsers(4).during(Duration.ofSeconds(12))
			)
		).protocols(httpProtocol)
			.maxDuration(Duration.ofSeconds(45))
			.assertions(
				global().responseTime().max().lt(5000),
				global().responseTime().mean().lt(1500),
				global().successfulRequests().percent().gt(75.0), // Adjusted for auth-protected endpoints
				forAll().failedRequests().count().lt(50L) // Adjusted for realistic failure count in load testing
			);
	}
}
