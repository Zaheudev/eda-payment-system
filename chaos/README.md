# Chaos Engineering (EDA Resilience Testing)

These scenarios demonstrate the resilience properties of event-driven architecture
by intentionally disrupting services and observing the system's behavior.

## Tools

- **Toxiproxy** — network chaos: latency, bandwidth, connection drops.
- **Pumba** — container chaos: randomly kill/stop/remove containers.
- **k6** — load generation while chaos is active.

## Scenarios

### 1. risk-fraud-kill

Kill the `risk-fraud` service while payments are flowing.

```bash
# Terminal 1: start load
k6 run load-tests/sustained.js

# Terminal 2: chaos
docker compose --profile chaos up pumba-riskfraud
```

**Expected EDA property**: Kafka retains messages in `payment-requests`. When
`risk-fraud` restarts, it resumes from its committed offset and processes ALL
messages. Zero payment loss.

### 2. kafka-latency

Inject 500ms latency on all Kafka connections.

```bash
# In the UI: Chaos tab → +500ms Latency
# Or via API:
curl -X POST http://localhost:8090/api/chaos/toxic/kafka \
  -H "Content-Type: application/json" \
  -d '{"type":"latency","latency":500}'
```

**Expected EDA property**: Throughput decreases but no errors. Producers and
consumers are fully decoupled by the broker's buffering.

### 3. kafka-bandwidth

Limit Kafka bandwidth to 1Mbps.

```bash
curl -X POST http://localhost:8090/api/chaos/toxic/kafka \
  -H "Content-Type: application/json" \
  -d '{"type":"bandwidth","rate":1024}'
```

**Expected EDA property**: Backpressure builds up naturally. Spring Kafka
consumers slow down but don't crash. When bandwidth is restored, the backlog
clears.

### 4. service-rotation

Use Pumba to randomly kill one of {risk-fraud, payment-routing, card-network-emulator}
every 30 seconds during a sustained load test.

```bash
docker compose --profile chaos up pumba-rotation
```

**Expected EDA property**: Consumer groups rebalance within ~5 seconds. No
duplicate processing (idempotent consumers or exactly-once semantics if
configured). The entire payment pipeline keeps functioning because Kafka
acts as a durable buffer between each stage.

## Cleanup

```bash
# Remove all toxics
curl -X DELETE http://localhost:8090/api/chaos/toxic/kafka

# Stop chaos containers
docker compose --profile chaos down
```
