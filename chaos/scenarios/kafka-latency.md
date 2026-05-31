# Chaos Scenario: Kafka Network Latency

## EDA property demonstrated

**Decoupling and backpressure**. Adding latency between services and Kafka
does not cause errors — it only reduces throughput naturally. No messages are
lost because Kafka acts as a durable buffer.

## Setup

1. Start all services + toxiproxy:
   ```bash
   docker compose up -d
   ```

2. Open the demo UI Chaos tab.

## Procedure

1. Start mixed-ops load:
   ```bash
   k6 run load-tests/mixed-ops.js
   ```

2. In the UI Chaos tab, click "+500ms Latency". This adds 500ms of network
   delay between services and Kafka.

3. Observe:
   - Throughput in the Topic Throughput panel drops
   - The Event Tape shows events arriving with gaps
   - No errors appear in the UI

4. Remove the toxic (click "Remove All Toxics").

5. Observe:
   - Throughput recovers
   - Backlog clears

## Verification checklist

- [ ] Zero HTTP 5xx errors during latency injection
- [ ] Consumer lag grows during latency, clears after removal
- [ ] All created payments eventually complete

## Pass criteria

- System gracefully handles latency without crashes.
- 100% of payments are processed (no loss).
