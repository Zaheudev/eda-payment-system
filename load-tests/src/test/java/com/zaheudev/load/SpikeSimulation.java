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

public class SpikeSimulation extends Simulation {

    private static final Logger log = LoggerFactory.getLogger(SpikeSimulation.class);
    private static final EdaObserver observer = new EdaObserver();
    private final EdaLoadReportWriter.ScenarioResult result =
            EdaReportManager.newScenario("Spike / Elasticity");

    private final HttpProtocolBuilder httpProtocol = http
            .baseUrl(LoadConfig.baseUrl())
            .contentTypeHeader("application/json")
            .acceptHeader("application/json")
            .shareConnections();

    private final Iterator<Map<String, Object>> feeder = new PaymentFeeder();

    private final ScenarioBuilder payments = scenario("Spike Payment Creation")
            .feed(feeder)
            .exec(http("Create Payment")
                    .post("/api/v1/payments")
                    .body(StringBody("#{jsonBody}"))
                    .check(status().in(201, 202)));

    {
        setUp(
                payments.injectOpen(
                        constantUsersPerSec(10).during(Duration.ofSeconds(20)),
                        constantUsersPerSec(100).during(Duration.ofSeconds(10)),
                        constantUsersPerSec(10).during(Duration.ofSeconds(30))
                )
        ).protocols(httpProtocol)
         .assertions(
                 global().responseTime().percentile(95.0).lt(10000),
                 global().failedRequests().percent().lt(10.0)
         );
    }

    @Override
    public void before() {
        log.info("=== SpikeSimulation: before hook ===");
        observer.start();
    }

    @Override
    public void after() {
        log.info("=== SpikeSimulation: after hook — waiting for async drain ===");
        boolean drained = observer.waitForDrain(300_000);

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

        result.notes.add("Baseline 10 tx/s → spike to 100 tx/s → back to 10 tx/s.");
        result.notes.add("Demonstrates EDA elasticity: Kafka absorbs burst, downstream drains backlog smoothly.");
        result.notes.add("Zero-loss under burst: " + (result.zeroLoss ? "YES" : "NO"));

        observer.stop();
        log.info("=== SpikeSimulation complete. Ingested: {}, Completed: {}, Drain: {} ms ===",
                ingested, completed, result.asyncDrainMs);
    }
}
