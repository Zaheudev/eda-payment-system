package com.zaheudev.load.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class ChaosController {

    private static final Logger log = LoggerFactory.getLogger(ChaosController.class);

    private final String killCmd;
    private final String restoreCmd;

    public ChaosController() {
        this.killCmd = LoadConfig.chaosKillCmd();
        this.restoreCmd = LoadConfig.chaosRestoreCmd();
    }

    public void induceOutage() {
        if (killCmd.isEmpty()) {
            log.warn("==============================================");
            log.warn("  MANUAL CHAOS: Please kill the downstream");
            log.warn("  consumer (e.g. risk-fraud process) NOW.");
            log.warn("  Press Enter after killing the consumer...");
            log.warn("==============================================");
            try {
                System.in.read();
            } catch (IOException ignored) {
            }
            return;
        }
        log.info("CHAOS: Executing kill command: {}", killCmd);
        try {
            int exit = Runtime.getRuntime()
                    .exec(new String[]{"sh", "-c", killCmd}).waitFor();
            log.info("CHAOS: kill command exited with code {}", exit);
            if (exit != 0) {
                log.warn("CHAOS: kill command returned non-zero ({}). Was the target process running?", exit);
            }
        } catch (Exception e) {
            log.error("CHAOS: kill command failed", e);
        }
    }

    public void healOutage() {
        if (restoreCmd.isEmpty()) {
            log.warn("==============================================");
            log.warn("  MANUAL CHAOS: Please restore the downstream");
            log.warn("  consumer NOW.");
            log.warn("  Press Enter after restoring...");
            log.warn("==============================================");
            try {
                System.in.read();
            } catch (IOException ignored) {
            }
            return;
        }
        log.info("CHAOS: Executing restore command: {}", restoreCmd);
        try {
            int exit = Runtime.getRuntime()
                    .exec(new String[]{"sh", "-c", restoreCmd}).waitFor();
            log.info("CHAOS: restore command exited with code {}", exit);
            if (exit != 0) {
                log.warn("CHAOS: restore command returned non-zero ({})", exit);
            }
        } catch (Exception e) {
            log.error("CHAOS: restore command failed", e);
        }
    }
}
