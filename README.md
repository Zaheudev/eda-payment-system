# Payment Gateway — Event-Driven Architecture

A microservices-based payment processing system built with Spring Boot and Apache Kafka, using Avro for message serialization.

---

## Modules

### `payment-gateway` — Port `8080`
The entry point of the system. Exposes a REST API for creating, capturing, refunding, and cancelling payments. Publishes payment events to Kafka and listens for results to update payment state.

### `risk-fraud` — Port `8085`
Consumes payment requests and performs risk assessment. Publishes either an approval or a rejection event based on the risk level.

### `payment-routing` — Port `8081`
Routes approved payments to the correct card network. Decides which processor should handle the transaction and forwards accordingly.

### `card-network-emulator` — Port `8086`
Simulates a real card network (e.g., Visa/Mastercard). Processes authorization, capture, and refund requests and publishes completion events back to Kafka. Adds configurable latency to mimic real-world conditions.

### `card-token-manager` — Port `8084`
Handles card tokenization and detokenization. Stores encrypted card data and returns tokens used by the rest of the system to avoid handling raw card numbers.

### `demo-vaadin` — Port `8095`
A web interface built with Vaadin Flow that visualizes the system in real time. It consumes all Kafka topics (read-only, it does not process payments) and pushes events to the browser via Vaadin server-push. It lets you create and track payments, view live saga states, inspect raw events, see live metrics (throughput, success rate, per-stage latency, network distribution), generate batches of payments for load demos, and compare optimal vs suboptimal routing costs.

### `shared`
A library module with shared Avro schemas and DTOs used across all services. Not a runnable application.

---


## Infrastructure

| Service         | Port   | Description                        |
|----------------|--------|------------------------------------|
| Kafka           | `29092` | Message broker (KRaft, no Zookeeper) |
| Schema Registry | `8082`  | Avro schema registry               |
| Kafka UI        | `8083`  | Web UI for Kafka topics & schemas  |
| PostgreSQL      | `5433`  | Shared database                    |

---

## How to Run

### 1. Start infrastructure

### 2. Build shared module first

### 3. Start each service

Run each module independently (in any order after infrastructure is up):

Or run them directly from IntelliJ using the individual Spring Boot run configurations.

---

## Notes

- All services connect to Kafka on `localhost:29092` and Schema Registry on `localhost:8082`.
- All services share the same PostgreSQL database (`payment-gateway`).
- Avro schemas are defined in `shared/src/main/resources/avro/`.

---

## API Reference

### `payment-gateway` — `http://localhost:8080`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/payments` | Create a new payment |
| `POST` | `/api/v1/capture/{paymentId}` | Capture an authorized payment |
| `POST` | `/api/v1/refund/{paymentId}` | Refund a captured payment |
| `GET`  | `/api/v1/payments` | List all payments *(not yet implemented)* |
| `GET`  | `/api/v1/payments/{paymentId}` | Get a payment by ID *(not yet implemented)* |

**Create Payment — request body:**
```json
{
  "merchantReference": "order-123",
  "amount": 99.99,
  "currency": "USD",
  "cardDetails": {
    "cardNumber": "4111111111111111",
    "cvv": "123",
    "expiryMonth": "12",
    "expiryYear": "2027",
    "cardHolderName": "John Doe"
  }
}
```

> You can pass a `tokenRef` instead of `cardDetails` if the card is already tokenized.

**Refund — request body:**
```json
{
  "amount": "49.99"
}
```

---

### `card-token-manager` — `http://localhost:8084`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/v1/tokenize` | Tokenize a card |
| `GET`  | `/api/v1/{tokenRef}/detokenize` | Retrieve raw card data by token |
| `GET`  | `/api/v1/{tokenRef}` | Get token metadata (no sensitive data) |

**Tokenize — request body:**
```json
{
  "cardNumber": "4111111111111111",
  "cvv": "123",
  "expiryMonth": "12",
  "expiryYear": "2027",
  "cardHolderName": "John Doe"
}
```

---

## BIN Lookup — Card Network & Type Detection

The `card-token-manager` determines the card network and type automatically from the card number at tokenization time. No manual configuration is needed — just use the right card number prefix.

### Card Network (determined by card number prefix)

| Network | Prefix | Example card number |
|---------|--------|---------------------|
| Visa | `4` | `4111111111111111` |
| Mastercard | `51` – `55` | `5111111111111111` |
| Amex | `34`, `37` | `371111111111111` |
| Discover | `6` | `6011111111111117` |

> If no prefix matches, the network defaults to **Visa**.

### Card Type (determined by BIN — first 6 digits)

| Type | BIN prefix    | Example card number |
|------|---------------|---------------------|
| DEBIT | `xxx1`        | `4530012222222222`  |
| CREDIT | anything else | `5555529999999999`  |

> ⚠️ In a real system this would come from a BIN database. This is a simplified demo rule.

## User Interface (Vaadin)

A web dashboard (`demo-vaadin`) for creating payments and watching the event flow in real time.

### Prerequisites
- Infrastructure running (Kafka, Schema Registry, PostgreSQL).
- At least `payment-gateway` running; start the other services too to see the full flow.

### Start the interface

```bash
./mvnw -pl demo-vaadin spring-boot:run -am
```

Then open the dashboard in your browser:

```
http://localhost:8095
```

> The interface connects to Kafka on `localhost:29092`, Schema Registry on `localhost:8082`, and the gateway on `localhost:8080`. It only reads events, so it can be started or stopped anytime without affecting payment processing.

### What you can do
- **Payments tab:** create a payment (with a built-in test-card generator), track it live through its states, run a **batch generator** to push many payments at once, and watch cumulative **routing fees** (optimal vs suboptimal).
- **Saga States tab:** see the current state and history of every transaction.
- **Live metrics:** throughput per minute, success vs rejection rate, per-stage latency (avg/min/max/p95), and network distribution.
- **Raw events:** a local, read-only view of the Kafka event log filtered for a single payment.

---

## Running Tests

### Prerequisites

- Java 21+ (the project targets release 21). If your default JDK is older, prefix commands with `JAVA_HOME=/usr/lib/jvm/java-25-openjdk`.
- Docker running (integration tests use Testcontainers to spin up Kafka, Schema Registry, and PostgreSQL).

### 1. Unit tests

Run with Surefire (fast, no external infrastructure needed):

```bash
./mvnw test

# single module:
./mvnw -pl risk-fraud test
```

### 2. Integration tests

End-to-end tests (`*IT` naming convention) using Failsafe and Testcontainers. Run on the `verify` phase:

```bash
./mvnw -pl integration-tests verify -am
```

Generates an EDA report at `integration-tests/target/eda-report/report.md` and JUnit XML reports at `integration-tests/target/failsafe-reports/`.

### 3. Load tests (Gatling)

Requires the full system and infrastructure running first. Each simulation submits traffic and observes the system under load or chaos conditions:

```bash
./mvnw -pl load-tests gatling:test -Dgatling.simulationClass=com.zaheudev.load.SpikeSimulation
```

Available simulations: `SpikeSimulation`, `ResilienceSimulation`, `DecouplingSimulation`. HTML results are written under `load-tests/target/gatling/`.