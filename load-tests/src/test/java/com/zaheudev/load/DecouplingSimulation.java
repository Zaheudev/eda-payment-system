package com.zaheudev.load;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import com.zaheudev.load.support.*;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

public class DecouplingSimulation extends Simulation {

    private static final Logger log = LoggerFactory.getLogger(DecouplingSimulation.class);
    private static final EdaObserver observer = new EdaObserver();
    private final EdaLoadReportWriter.ScenarioResult result =
            EdaReportManager.newScenario("Decoupling & Async Throughput");

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(LoadConfig.baseUrl())
            .contentTypeHeader("application/json")
            .acceptHeader("application/json")
            .shareConnections();

    private final Iterator<Map<String, Object>> feeder = new PaymentFeeder();

    private final ScenarioBuilder payments = scenario("Sustained Payment Creation")
            .feed(feeder)
            .exec(http("Create Payment")
                    .post("/api/v1/payments")
                    .body(StringBody("#{jsonBody}"))
                    .check(status().in(201, 202)));

    {
        setUp(
                payments.injectOpen(
                        rampUsersPerSec(1).to(30).during(Duration.ofSeconds(20)),
                        constantUsersPerSec(30).during(Duration.ofSeconds(60))
                )
        ).protocols(httpProtocol)
         .assertions(
                 global().responseTime().percentile(95.0).lt(5000),
                 global().failedRequests().percent().lt(5.0)
         );
    }

    @Override
    public void before() {
        log.info("=== DecouplingSimulation: before hook ===");
        observer.start();
    }

    @Override
    public void after() {
        log.info("=== DecouplingSimulation: after hook — waiting for async drain ===");
        boolean drained = observer.waitForDrain(120_000);

        long ingested = observer.ingestionCount();
        long completed = observer.completionCount();
        var e2e = observer.e2eLatencies();
        Collections.sort(e2e);

        result.status = drained ? "PASSED" : "WARNING";
        result.asyncIngestionCount = ingested;
        result.asyncCompletionCount = completed;
        result.asyncAvgLatency = observer.avg(e2e);
        result.asyncP50 = EdaObserver.percentile(e2e, 50);
        result.asyncP95 = EdaObserver.percentile(e2e, 95);
        result.asyncP99 = EdaObserver.percentile(e2e, 99);
        result.asyncDrainMs = observer.drainTimeMs();
        result.zeroLoss = completed >= ingested && ingested > 0;
        result.lostCount = result.zeroLoss ? 0 : (ingested - completed);

        result.notes.add("Sustained 30 tx/s for 60s after 20s ramp; proved async decoupling: "
                + ingested + " ingested, " + completed + " completed asynchronously.");
        if (!drained) {
            result.notes.add("WARNING: Async drain did not complete within timeout.");
        }
        result.notes.add("Zero-loss: " + (result.zeroLoss ? "YES" : "NO"));

        observer.stop();
        log.info("=== DecouplingSimulation complete. Ingested: {}, Completed: {}, Drain: {} ms ===",
                ingested, completed, result.asyncDrainMs);
    }
}
