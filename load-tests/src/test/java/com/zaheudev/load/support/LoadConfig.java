package com.zaheudev.load.support;

public final class LoadConfig {

    private LoadConfig() {}

    public static String baseUrl() {
        return System.getProperty("target.baseUrl", "http://localhost:8080");
    }

    public static String kafkaBootstrap() {
        return System.getProperty("kafka.bootstrap", "localhost:29092");
    }

    public static String schemaRegistryUrl() {
        return System.getProperty("schema.registry.url", "http://localhost:8082");
    }

    public static String chaosKillCmd() {
        return System.getProperty("eda.chaos.kill.cmd", "");
    }

    public static String chaosRestoreCmd() {
        return System.getProperty("eda.chaos.restore.cmd", "");
    }
}
