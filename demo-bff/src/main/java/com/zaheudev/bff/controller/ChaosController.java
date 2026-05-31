package com.zaheudev.bff.controller;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@CrossOrigin(origins = "*")
public class ChaosController {

    @Value("${toxiproxy.host:localhost}")
    private String toxiproxyHost;

    @Value("${toxiproxy.port:8474}")
    private int toxiproxyPort;

    private DockerClient createDockerClient() {
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(URI.create("unix:///var/run/docker.sock"))
                .maxConnections(10)
                .connectionTimeout(Duration.ofSeconds(5))
                .responseTimeout(Duration.ofSeconds(10))
                .build();
        return DockerClientBuilder.getInstance().withDockerHttpClient(httpClient).build();
    }

    @GetMapping("/api/chaos/containers")
    public List<Map<String, Object>> listContainers() {
        List<Map<String, Object>> result = new ArrayList<>();
        try (DockerClient docker = createDockerClient()) {
            for (Container c : docker.listContainersCmd().withShowAll(true).exec()) {
                Map<String, Object> info = new LinkedHashMap<>();
                info.put("id", c.getId().substring(0, 12));
                info.put("names", Arrays.toString(c.getNames()));
                info.put("state", c.getState());
                info.put("status", c.getStatus());
                result.add(info);
            }
        } catch (Exception e) {
            log.error("Failed to list containers", e);
        }
        return result;
    }

    @PostMapping("/api/chaos/kill/{containerName}")
    public ResponseEntity<Map<String, Object>> killContainer(@PathVariable String containerName) {
        try (DockerClient docker = createDockerClient()) {
            docker.stopContainerCmd(containerName).exec();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("action", "kill");
            result.put("container", containerName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to kill container {}", containerName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/chaos/start/{containerName}")
    public ResponseEntity<Map<String, Object>> startContainer(@PathVariable String containerName) {
        try (DockerClient docker = createDockerClient()) {
            docker.startContainerCmd(containerName).exec();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("action", "start");
            result.put("container", containerName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to start container {}", containerName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/chaos/bounce/{containerName}")
    public ResponseEntity<Map<String, Object>> bounceContainer(
            @PathVariable String containerName,
            @RequestParam(defaultValue = "5") int delay) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("action", "bounce");
        result.put("container", containerName);
        result.put("delay", delay);

        CompletableFuture.runAsync(() -> {
            try (DockerClient docker = createDockerClient()) {
                log.info("Bouncing {}: stopping...", containerName);
                docker.stopContainerCmd(containerName).exec();
                log.info("Bouncing {}: waiting {}s...", containerName, delay);
                Thread.sleep(delay * 1000L);
                log.info("Bouncing {}: starting...", containerName);
                docker.startContainerCmd(containerName).exec();
                log.info("Bouncing {}: done", containerName);
            } catch (Exception e) {
                log.error("Failed to bounce container {}", containerName, e);
            }
        });

        return ResponseEntity.accepted().body(result);
    }

    @PostMapping("/api/chaos/toxic/{proxyName}")
    public ResponseEntity<Map<String, Object>> addToxic(
            @PathVariable String proxyName,
            @RequestBody Map<String, Object> request) {
        try {
            ToxiproxyClient client = new ToxiproxyClient(toxiproxyHost, toxiproxyPort);
            Proxy proxy = client.getProxy(proxyName);
            String type = (String) request.getOrDefault("type", "latency");
            ToxicDirection direction = "upstream".equals(request.get("direction"))
                    ? ToxicDirection.UPSTREAM : ToxicDirection.DOWNSTREAM;

            switch (type) {
                case "latency" -> {
                    long ms = ((Number) request.getOrDefault("latency", 500)).longValue();
                    proxy.toxics().latency("latency-" + proxyName, direction, ms).setJitter(ms / 5);
                }
                case "bandwidth" -> {
                    long kbps = ((Number) request.getOrDefault("rate", 1024)).longValue();
                    proxy.toxics().bandwidth("bw-" + proxyName, direction, kbps);
                }
                case "down" -> proxy.toxics().timeout("down-" + proxyName, direction, 0);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("action", "toxic-added");
            result.put("type", type);
            result.put("proxy", proxyName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to add toxic to {}", proxyName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/chaos/toxic/{proxyName}")
    public ResponseEntity<Map<String, Object>> removeToxics(@PathVariable String proxyName) {
        try {
            ToxiproxyClient client = new ToxiproxyClient(toxiproxyHost, toxiproxyPort);
            for (var toxic : client.getProxy(proxyName).toxics().getAll()) {
                toxic.remove();
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("action", "toxics-removed");
            result.put("proxy", proxyName);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Failed to remove toxics from {}", proxyName, e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
