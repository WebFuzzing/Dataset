package simulations.jasper;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import java.time.Duration;

/**
 * Inferno Load Test for Jasper
 *
 * This simulation pushes the system to its absolute limits:
 * - 15 second warmup trickle to let the system initialize
 * - Two cycles of extreme load (ramp huge users for 10s, trickle for 50s)
 * - 2 minute 15 second total duration
 * - >15% success rate required to pass (85% failure tolerance)
 * - Tests extreme concurrent operations and recovery
 */
public class InfernoSimulation extends Simulation {
	private static final String STATIC_XSRF_TOKEN = "gatling-static-token-for-testing";

	HttpProtocolBuilder httpProtocol = http
		.baseUrl("http://localhost:8081")
		.acceptHeader("application/json")
		.contentTypeHeader("application/json")
		.userAgentHeader("Gatling Inferno Test")
		.disableFollowRedirect()
		.check(status().not(500)); // Allow other error codes for extreme stress testing

	// ====================== High Volume Operations ======================

	ChainBuilder rapidRefCreation =
		exec(session -> {
			// Add timestamp to URL for uniqueness to avoid duplicate key violations
			String url = "https://inferno-test.example.com/" +
				System.currentTimeMillis() + "-" +
				(1 + new java.util.Random().nextInt(1000000));
			return session.set("rapidRefUrl", url);
		})
			.exec(addCookie(Cookie("XSRF-TOKEN", STATIC_XSRF_TOKEN).withDomain("localhost").withPath("/").withSecure(false)))
			.exec(
				http("Inferno Ref Creation")
					.post("/api/v1/ref")
					.header("X-XSRF-TOKEN", STATIC_XSRF_TOKEN)
					.body(StringBody("""
					{
						"url": "#{rapidRefUrl}",
						"title": "Inferno Test #{randomInt(1,1000000)}",
						"comment": "InfernoTest#{randomInt(100000,999999)} - Extreme stress test",
						"tags": [
							"inferno",
							"extreme",
							"load.#{randomInt(1,10000)}"
						]
					}"""))
					.check(status().in(429, 503, 201, 409, 400))
			)
			.pause(Duration.ofMillis(10), Duration.ofMillis(50));

	ChainBuilder rapidQuery = exec(
		http("Inferno Query")
			.get("/api/v1/ref/page")
			.queryParam("size", "100")
			.queryParam("query", "inferno")
			.check(status().in(429, 503, 200, 502, 504))
	)
		.pause(Duration.ofMillis(10), Duration.ofMillis(100));

	ChainBuilder searchQuery = exec(
		http("Inferno Search")
			.get("/api/v1/ref/page")
			.queryParam("query", "extreme|load.#{randomInt(1,1000)}")
			.queryParam("size", "50")
			.check(status().in(429, 503, 200, 502, 504))
	)
		.pause(Duration.ofMillis(20), Duration.ofMillis(100));

	// ====================== Scenarios ======================

	ScenarioBuilder infernoLoad = scenario("Inferno Load")
		.randomSwitch().on(
			percent(50.0).then(exec(rapidRefCreation)),
			percent(30.0).then(exec(rapidQuery)),
			percent(20.0).then(exec(searchQuery))
		);

	// ====================== Simulation Setup ======================

	{
		setUp(
			// Warmup: 15 second intro trickle to let the system initialize
			infernoLoad.injectOpen(
				constantUsersPerSec(5).during(Duration.ofSeconds(15)),
				// Cycle 1: Ramp huge number of users for 10 seconds, then trickle for 50 seconds
				rampUsers(20_000).during(Duration.ofSeconds(10)),
				constantUsersPerSec(5).during(Duration.ofSeconds(50)),
				// Cycle 2: Another huge ramp for 10 seconds, then trickle for 50 seconds
				rampUsers(5_000).during(Duration.ofSeconds(10)),
				constantUsersPerSec(5).during(Duration.ofSeconds(50))
			)
		).protocols(httpProtocol)
			.maxDuration(Duration.ofMinutes(3))
			.assertions(
				global().responseTime().max().lt(30000), // Allow very long response times
				global().successfulRequests().percent().gt(15.0) // Only 15% success required
			);
	}
}
