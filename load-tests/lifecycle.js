import http from "k6/http";
import { check, sleep, group } from "k6";
import { Trend, Counter } from "k6/metrics";
import { waitForStatus } from "./lib/wait-for-state.js";

const paymentsStuck = new Counter("eda_payments_stuck");
const simulatedRejects = new Counter("eda_simulated_rejects");

function classify(w, stage) {
  if (w.reached) return "ok";
  if (w.status === "REJECTED" || w.status === "FAILED") {
    simulatedRejects.add(1, { stage });
    return "rejected";
  }
  paymentsStuck.add(1, { stage });
  return "stuck";
}

const tCreate2Auth = new Trend("eda_create_to_authorized_ms");
const tCapture2Done = new Trend("eda_capture_to_captured_ms");
const tAuth2Void = new Trend("eda_auth_to_voided_ms");

export const options = {
  stages: [
    { duration: "20s", target: 3 },
    { duration: "3m", target: 8 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    "eda_create_to_authorized_ms": ["p(95)<8000"],
    http_req_failed: ["rate<0.1"],
    checks: ["rate>0.9"],
    eda_payments_stuck: ["count<50"],
  },
};

const BASE = "http://localhost:8080/api/v1";
const CARDS = ["4111111111111111", "5111111111111111", "371111111111111", "6011111111111117"];
const PARAMS = { headers: { "Content-Type": "application/json" } };

export default function () {
  let pid;

  // ---- Phase 1: CREATE → wait for AUTHORIZED ----
  group("01-create-and-authorize", () => {
    const payload = JSON.stringify({
      merchantReference: "k6-lifecycle-" + __VU + "-" + __ITER,
      amount: 10 + Math.random() * 500,
      currency: "USD",
      cardDetails: {
        cardNumber: CARDS[Math.floor(Math.random() * CARDS.length)],
        cvv: "123",
        expiryMonth: "12",
        expiryYear: "2027",
        cardHolderName: "K6 Lifecycle",
      },
    });

    const res = http.post(`${BASE}/payments`, payload, PARAMS);
    const ok = check(res, { "create accepted": (r) => r.status >= 200 && r.status < 300 });
    if (!ok) return;

    const body = JSON.parse(res.body);
    pid = body.paymentId || (body.data && body.data.paymentId);
    if (!pid) return;

    const w = waitForStatus(pid, "AUTHORIZED", { maxWaitMs: 15000 });
    tCreate2Auth.add(w.ms);
    check(w, { "reached or simulated-reject": (x) =>
      x.reached || x.status === "REJECTED" || x.status === "FAILED" });
    if (classify(w, "auth") !== "ok") return;
  });

  sleep(0.2);

  // ---- Phase 2: CAPTURE or VOID ----
  const takeVoidPath = Math.random() < 0.2;

  if (takeVoidPath) {
    group("02-void", () => {
      const res = http.post(`${BASE}/void/${pid}`, null, PARAMS);
      check(res, { "void accepted": (r) => r.status >= 200 && r.status < 300 });

      const w = waitForStatus(pid, "VOID", { maxWaitMs: 8000 });
      tAuth2Void.add(w.ms);
      check(w, { "reached or simulated-reject": (x) =>
        x.reached || x.status === "FAILED" });
      classify(w, "void");
    });
  } else {
    group("02-capture", () => {
      const res = http.post(`${BASE}/capture/${pid}`, null, PARAMS);
      check(res, { "capture accepted": (r) => r.status >= 200 && r.status < 300 });

      const w = waitForStatus(pid, "CAPTURED", { maxWaitMs: 8000 });
      tCapture2Done.add(w.ms);
      check(w, { "reached or simulated-reject": (x) =>
        x.reached || x.status === "FAILED" });
      if (classify(w, "capture") !== "ok") return;
    });

    // ---- Phase 3: partial refund (50% of captures) ----
    if (Math.random() < 0.5) {
      group("03-refund", () => {
        const refundPayload = JSON.stringify({ amount: 5, currency: "USD" });
        const res = http.post(`${BASE}/refund/${pid}`, refundPayload, PARAMS);
        check(res, { "refund accepted": (r) => r.status >= 200 && r.status < 300 });

        const rw = waitForStatus(pid, ["REFUNDED", "PARTIALLY_REFUNDED"], { maxWaitMs: 8000 });
        check(rw, { "reached or simulated-reject": (x) =>
          x.reached || x.status === "FAILED" });
        classify(rw, "refund");
      });
    }
  }

  sleep(0.5 + Math.random() * 0.5);
}
