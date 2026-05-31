# Chaos Scenario: Kill risk-fraud During Load

## EDA property demonstrated

**Consumer resilience via Kafka buffering**. When a consumer goes down, Kafka
retains uncommitted messages. On restart, the consumer resumes exactly where it
left off.

## Setup

1. Start all services: `docker compose up -d`
2. Open the demo UI at http://localhost:5173
3. Open Kafka UI at http://localhost:8083 to watch consumer groups

## Procedure

1. Start sustained load:
   ```bash
   k6 run load-tests/sustained.js
   ```

2. Wait ~30 seconds for steady state.

3. In the UI Chaos tab, click "Kill" on the `risk-fraud` container.
   Or via CLI: `docker stop payment-risk-fraud`

4. Observe:
   - Kafka UI → consumer group `risk-group` → lag starts growing on `payment-requests`
   - Payments keep getting created (gateway is still up)
   - Sagas stall at CREATED state

5. After 30-60 seconds, restart:
   ```bash
   docker start payment-risk-fraud
   ```
   Or click "Start" in the UI Chaos tab.

6. Observe:
   - Consumer lag drops to zero as `risk-fraud` catches up
   - All sagas advance from CREATED → RISK_ASSESSED → ROUTED → ...
   - **Zero payment loss** — every payment gets assessed

## Verification checklist

- [ ] No 5xx errors from gateway during the kill window
- [ ] Consumer lag in `payment-requests` grows during kill, drops to zero after restart
- [ ] Every saga from the load test reaches a terminal state
- [ ] No duplicate risk assessments (each payment processed exactly once)

## Pass criteria

- All payments created during chaos are fully processed.
- The system self-heals without manual intervention.
