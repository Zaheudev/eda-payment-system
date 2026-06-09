package com.zaheudev.load;

import com.zaheudev.load.support.EdaLoadReportWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EdaReportManager {

    private static final Logger log = LoggerFactory.getLogger(EdaReportManager.class);
    private static final EdaLoadReportWriter writer = new EdaLoadReportWriter();
    private static volatile boolean hookRegistered = false;

    static {
        if (!hookRegistered) {
            hookRegistered = true;
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    writer.write();
                } catch (Exception e) {
                    log.error("Failed to write EDA report", e);
                }
            }, "eda-report-shutdown"));
        }
    }

    private EdaReportManager() {}

    public static EdaLoadReportWriter.ScenarioResult newScenario(String name) {
        return writer.newScenario(name);
    }
}
