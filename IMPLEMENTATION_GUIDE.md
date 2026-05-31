# EDA Showcase — Implementation Guide

This document is the implementation companion to the approved spec
(`~/.factory/specs/2026-05-28-eda-demo-ui-experimental-testing-stack.md`).

It explains, step by step, **how** to wire the new pieces into the existing
project. Existing service code is intentionally left untouched: all changes
are additive (new modules, new files, new docker services). Where existing
files must be edited, the required diff is shown inline.

Design rules:
- **Plain CSS** in `demo-ui` (no TailwindCSS, no UI kit). Keep it minimalistic.
- No business-logic rewrites in `payment-gateway`, `risk-fraud`,
  `payment-routing`, `card-network-emulator`, `card-token-manager`.
- All new infrastructure is opt-in via docker compose profiles.

---

## 0. New top-level layout

```
demo/
├── demo-bff/                  (new) Spring Boot BFF, port 8090
├── demo-ui/                   (new) React + Vite UI, served on 5173 (dev) / 8091 (prod)
├── load-tests/                (new) k6 scripts
├── chaos/                     (new) Pumba + Toxiproxy configs
├── IMPLEMENTATION_GUIDE.md    (this file)
└── ... existing modules unchanged ...
```

---

## 1. demo-bff (Spring Boot, WebFlux, port 8090)

Already scaffolded under `demo-bff/`. Responsibilities:

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/events/stream` | GET (SSE) | Live stream of all Kafka events (every topic) |
| `/api/sagas/{paymentId}` | GET | Current saga state projection |
| `/api/sagas` | GET | All known saga states |
| `/api/dlt/{topic}` | GET | List messages in a DLT topic |
| `/api/dlt/{topic}/replay` | POST | Re-publish a DLT message to origin topic |
| `/api/chaos/kill/{service}` | POST | Stop a container via Docker Engine API |
| `/api/chaos/start/{service}` | POST | Start a container |
| `/api/chaos/toxic/{service}` | POST | Add a Toxiproxy toxic (latency, bandwidth, down) |
| `/api/chaos/toxic/{service}` | DELETE | Remove all toxics |
| `/api/topics` | GET | Topic names + recent throughput counters |

Implementation notes:
- Use `spring-kafka` with a generic `GenericRecord` Avro deserializer; do NOT
  depend on `shared` types directly (we want the BFF to keep working if a
  schema evolves before the BFF is rebuilt).
- One consumer per topic, all in a unique consumer group computed at startup
  (`demo-bff-<uuid>`) so the UI always sees fresh events.
- Saga projection: in-memory `ConcurrentHashMap<String, SagaState>` keyed by
  `paymentId`. State machine: `CREATED → RISK_OK | RISK_REJECTED → ROUTED →
  AUTHORIZED → CAPTURED | VOIDED → REFUNDED`. Use the event `type` field or
  the topic name to advance.
- SSE: emit JSON envelopes `{ topic, key, timestamp, payload, paymentId }`.
- Chaos endpoint uses `com.github.docker-java:docker-java` to talk to the
  Docker daemon on `/var/run/docker.sock`. Mount that socket into the
  `demo-bff` container.

---

## 2. demo-ui (React + Vite + TS, plain CSS)

Scaffolded under `demo-ui/`. Run with:

```
cd demo-ui
npm install
npm run dev
```

Structure:
```
demo-ui/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── styles.css            (plain CSS, ~150 lines, minimalistic)
    ├── api.ts                (REST + SSE client)
    ├── components/
    │   ├── ServiceGraph.tsx  (React Flow)
    │   ├── EventTape.tsx
    │   ├── PaymentForm.tsx
    │   ├── SagaPanel.tsx
    │   ├── ChaosPanel.tsx
    │   └── DltPanel.tsx
    └── types.ts
```

Design tokens (`styles.css`):
- Background `#fafafa`, surface `#ffffff`, border `#e5e5e5`
- Mono font for events, system sans for UI
- Single accent color `#2563eb`
- No gradients, no shadows beyond 1px borders.

---

## 3. Transactional Outbox (in `payment-gateway`)

Do not touch the existing classes. Add these **new** files inside the
existing module (no rewrites required):

`payment-gateway/src/main/resources/db/migration/V2__outbox.sql`
```sql
CREATE TABLE IF NOT EXISTS outbox_event (
    id              UUID PRIMARY KEY,
    aggregate_id    VARCHAR(64) NOT NULL,
    topic           VARCHAR(128) NOT NULL,
    message_key     VARCHAR(128),
    avro_payload    BYTEA NOT NULL,
    headers_json    TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    sent_at         TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_outbox_unsent ON outbox_event (created_at) WHERE sent_at IS NULL;
```

New Java files (suggested package `com.zaheudev.gateway.outbox`):
- `OutboxEvent.java` (JPA entity)
- `OutboxRepository.java` (Spring Data)
- `OutboxPublisher.java` (annotated with `@Scheduled(fixedDelay = 500)`, reads
  unsent rows, publishes via `KafkaTemplate`, sets `sent_at` in a transaction)
- `OutboxRecorder.java` (a tiny helper your existing `PaymentService` could
  call instead of `kafkaTemplate.send(...)` going forward — opt-in, no need
  to refactor existing send-sites immediately)

The existing `PaymentService` will keep working as-is. The outbox is purely
additive; new event publications can use it, old ones can stay.

---

## 4. DLT + Replay

Each Kafka consumer (in every service) should be configured with:

```java
@Bean
DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> template) {
    var recoverer = new DeadLetterPublishingRecoverer(template,
        (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));
    return new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
}
```

Add this **once per service** in a new `KafkaErrorConfig` class. Do not
modify existing consumer code.

Replay is implemented entirely in `demo-bff` (`/api/dlt/{topic}/replay`): it
reads the message at the requested offset and re-publishes to the origin
topic (strip `.DLT` suffix).

---

## 5. Saga state visualization

`demo-bff` owns the projection (see §1). UI reads `/api/sagas/{paymentId}`
and renders a tiny state-machine diagram in `SagaPanel.tsx`. The diagram is
static SVG with circles per state and the current one highlighted.

States to model (derived from existing events in `shared/.../avro/`):
```
CREATED → RISK_OK → ROUTED → AUTHORIZED → CAPTURED
       ↘ RISK_REJECTED                  ↘ VOIDED
                                         ↘ REFUNDED (partial / full)
FAILED (terminal, from any error / DLT)
```

---

## 6. Chaos controls

### Containers
`demo-bff` calls Docker Engine API to stop/start containers by name. Names
match `docker-compose.yml`: `payment-risk-fraud`, `payment-routing`, etc.

### Toxiproxy
- Add a `toxiproxy` container (already in updated docker-compose).
- Each service in compose connects to Kafka through Toxiproxy at
  `toxiproxy:9092` instead of `kafka:9092` (only used in the `chaos` profile).
- `demo-bff` uses `eu.rekawek.toxiproxy:toxiproxy-java` to add/remove toxics
  per service. UI offers presets: `+500ms latency`, `bandwidth 1mbps`,
  `connection down`.

> If you don't want chaos enabled by default, run with:
> `docker compose --profile chaos up`.

---

## 7. Testcontainers integration tests

Per service, in `src/test/java/.../it/`:

```java
@SpringBootTest
@Testcontainers
class PaymentFlowIT {
    @Container static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:7.6.0");
    @Container static GenericContainer<?> schemaRegistry = new GenericContainer<>("confluentinc/cp-schema-registry:7.6.0")
        .withExposedPorts(8081)
        .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "kafka:9092")
        .dependsOn(kafka);
    @Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        r.add("spring.datasource.url", postgres::getJdbcUrl);
        // ...
    }

    @Test void happyPath() { /* publish create-payment, await captured */ }
}
```

Required deps to add to each service `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.20.4</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.2</version>
    <scope>test</scope>
</dependency>
```

Test scenarios to cover (one IT class per scenario):
1. Happy path: create → risk OK → routed → authorized → captured.
2. Risk rejected: create → risk REJECTED → no further events.
3. Emulator decline: routed → authorization fails → saga ends FAILED.
4. Partial refund then full refund.
5. Void after authorize (no capture).
6. Outbox survives crash: stop service mid-flow, restart, verify event eventually published.
7. Poison message lands in `.DLT` and replay restores normal flow.

---

## 8. Schema compatibility tests

Add to `shared/pom.xml` (already wired by §9 below):

```xml
<plugin>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-schema-registry-maven-plugin</artifactId>
    <version>7.6.0</version>
    <configuration>
        <schemaRegistryUrls>
            <param>http://localhost:8082</param>
        </schemaRegistryUrls>
        <subjects>
            <payment-events-value>src/main/resources/avro/PaymentEvent.avsc</payment-events-value>
            <!-- one entry per subject -->
        </subjects>
        <compatibilityLevels>
            <payment-events-value>BACKWARD</payment-events-value>
        </compatibilityLevels>
    </configuration>
    <goals>
        <goal>test-compatibility</goal>
    </goals>
</plugin>
```

Run with: `mvn -pl shared schema-registry:test-compatibility`.

---

## 9. Required additive edits to existing root files

### `pom.xml` (root)

Add `demo-bff` to the `<modules>` block:
```xml
<module>demo-bff</module>
```
(`demo-ui` is not a Maven module — built with npm.)

### `docker-compose.yml`

Append the services listed in section 11 below. No existing service entries
are removed or renamed.

---

## 10. Load tests (k6) and chaos scenarios

See `load-tests/README.md` and `chaos/README.md`. Quick reference:

```bash
# Smoke
k6 run load-tests/smoke.js
# Sustained 50 RPS for 5 min
k6 run load-tests/sustained.js
# Burst then chaos kill
make chaos-risk-fraud-kill & k6 run load-tests/sustained.js
```

Each chaos scenario in `chaos/scenarios/*.md` documents:
- Setup
- Expected EDA property (e.g., "consumer group rebalances within ~5s; zero
  message loss because Kafka retains offsets")
- How to verify (UI panel + Kafka UI offsets)

---

## 11. docker-compose additions (already applied)

The following services were appended to `docker-compose.yml`:
- `toxiproxy` (port 8474 admin, 9092 kafka proxy)
- `demo-bff` (port 8090)
- `demo-ui` (port 8091, nginx-served prod build)
- A `pumba` profile that randomly kills `payment-risk-fraud`

To use chaos: `docker compose --profile chaos up`.
To use full demo: `docker compose --profile demo up`.

---

## 12. Suggested implementation order

1. Bring up new compose services: `toxiproxy`, `demo-bff` (skeleton already in place), `demo-ui`.
2. Implement `demo-bff` SSE + saga projection + topic catalog.
3. Build `demo-ui` shell with the service graph and event tape.
4. Add forms (create/capture/refund/void) calling existing `payment-gateway`.
5. Add saga panel.
6. Add chaos panel (containers first, Toxiproxy second).
7. Add outbox in `payment-gateway` (new files only).
8. Add DLT config + replay endpoint + UI button.
9. Write Testcontainers ITs per service.
10. Wire Schema Registry compatibility plugin.
11. Author k6 scripts and chaos scenarios.

Each step is independent and shippable on its own.
