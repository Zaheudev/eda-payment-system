import http from "k6/http";
import { check, sleep, group } from "k6";
import { Counter } from "k6/metrics";
import { waitForStatus } from "./lib/wait-for-state.js";

const earlyAttempts = new Counter("eda_early_capture_attempts");
const eventualSuccess = new Counter("eda_eventual_capture_success");

export const options = {
  vus: 3,
  iterations: 20,
  thresholds: {
    checks: ["rate>0.8"],
  },
};

const BASE = "http://localhost:8080/api/v1";
const PARAMS = { headers: { "Content-Type": "application/json" } };

export default function () {
  let pid;

  // Step 1: CREATE
  group("01-create", () => {
    const payload = JSON.stringify({
      merchantReference: "k6-ooo-" + __VU + "-" + __ITER,
      amount: 50,
      currency: "USD",
      cardDetails: {
        cardNumber: "4111111111111111",
        cvv: "123",
        expiryMonth: "12",
        expiryYear: "2027",
        cardHolderName: "K6 OutOfOrder",
      },
    });
    const res = http.post(`${BASE}/payments`, payload, PARAMS);
    check(res, { "create ok": (r) => r.status >= 200 && r.status < 300 });
    pid = JSON.parse(res.body).paymentId;
  });

  sleep(0.1);

  // Step 2: fire CAPTURE IMMEDIATELY (should fail — not yet AUTHORIZED)
  group("02-too-early-capture", () => {
    const res = http.post(`${BASE}/capture/${pid}`, null, PARAMS);
    if (res.status >= 400) {
      earlyAttempts.add(1);
      console.log(`[EXPECTED] Early capture for ${pid} returned ${res.status} — payment not yet authorized`);
    }
    check(res, {
      "early capture fails (400/500)": (r) => r.status >= 400,
    });
  });

  // Step 3: WAIT for AUTHORIZED
  group("03-wait-authorized", () => {
    const w = waitForStatus(pid, "AUTHORIZED", { maxWaitMs: 10000 });
    check(w, { "eventually AUTHORIZED": (x) => x.reached });
  });

  // Step 4: RETRY CAPTURE
  group("04-retry-capture", () => {
    const res = http.post(`${BASE}/capture/${pid}`, null, PARAMS);
    if (res.status >= 200 && res.status < 300) {
      eventualSuccess.add(1);
    }
    check(res, {
      "retry capture succeeds": (r) => r.status >= 200 && r.status < 300,
    });
  });

  sleep(0.3);
}
