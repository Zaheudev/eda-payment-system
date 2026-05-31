# Load Tests (k6)

EDA-aware tests that prove the system works **with** its asynchronous nature,
not against it.

## Prerequisites

```bash
# Install k6
# macOS:    brew install k6
# Linux:    https://k6.io/docs/get-started/installation/
```

All infrastructure + services must be running before executing tests:

```bash
# infra
docker compose up -d

# services (each in its own terminal / IntelliJ run config)
# payment-gateway, risk-fraud, payment-routing, card-network-emulator, card-token-manager
# demo-bff (optional — needed only for UI Event Tape / Saga tabs)
```

## Scripts

```bash
# Quick health check
k6 run load-tests/smoke.js

# Sustained CREATE load — proves producer never blocks
k6 run load-tests/sustained.js

# Full lifecycle — create → wait for AUTHORIZED → capture → (refund | void)
k6 run load-tests/lifecycle.js

# Showpiece EDA test — high-RPS CREATE burst, zero wait, verify later
k6 run load-tests/fire-and-forget.js

# Eventual consistency — too-early capture fails, wait, retry succeeds
k6 run load-tests/out-of-order.js
```

## What each test proves

| Test | EDA property demonstrated |
|------|---------------------------|
| `smoke.js` | Basic connectivity — gateway accepts payments |
| `sustained.js` | **Producer never blocks.** Gateway returns 201 immediately for every payment at 20 RPS. The async pipeline (risk → routing → emulator) catches up through Kafka buffering. Zero HTTP errors. |
| `lifecycle.js` | **State-aware client pattern.** Each VU walks one payment through CREATE → AUTHORIZED → CAPTURED → (REFUND or VOID). Polls `GET /api/v1/payments/{id}` with exponential backoff between each stage. Measures per-transition latency via k6 Trends: `eda_create_to_authorized_ms`, `eda_capture_to_captured_ms`, `eda_auth_to_voided_ms`. |
| `fire-and-forget.js` | **Decoupling.** Hammers CREATE at 30 VUs, never waits per-request. Kafka absorbs the burst; consumers catch up at their own pace. All payments eventually reach AUTHORIZED (verify via the UI Payments tab or Kafka UI consumer lag → 0). |
| `out-of-order.js` | **Eventual consistency & retry.** For each iteration: creates a payment, immediately fires CAPTURE (expected 400/500 — not yet authorized), waits for AUTHORIZED, retries CAPTURE (expected 202). Counts `eda_early_capture_attempts` and `eda_eventual_capture_success`. Demonstrates why EDA clients must be state-aware. |

## Verifying zero-loss (`fire-and-forget.js`)

After the test finishes, open the UI (http://localhost:5173) → Payments tab:

1. Note the count of created payments (should match k6 output `eda_payments_created`).
2. After 30-60 seconds, all payments should be AUTHORIZED (or CAPTURED if you ran lifecycle).
3. Check Kafka UI (http://localhost:8083) → consumer lag for all groups → should be 0.
4. **No payment was lost.** Every CREATE resulted in a terminal saga state.

## Key design difference from the old `mixed-ops.js`

The old script randomly chose create / capture / void / refund — but capture always
failed because the payment was still CREATED (the authorization chain takes 100-500ms).
**Random action selection only works for stateless APIs.** EDA systems require the
client to observe state before acting. Every script in this suite does that.
