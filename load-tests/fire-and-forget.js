import http from "k6/http";
import { check, sleep } from "k6";
import { waitForStatus } from "./lib/wait-for-state.js";

export const options = {
  stages: [
    { duration: "15s", target: 10 },
    { duration: "30s", target: 30 },
    { duration: "15s", target: 0 },
  ],
  thresholds: {
    http_req_duration: ["p(95)<1000"],
    http_req_failed: ["rate<0.05"],
  },
};

const BASE = "http://localhost:8080/api/v1";
const CARDS = ["4111111111111111", "5111111111111111", "371111111111111", "6011111111111117"];
const PARAMS = { headers: { "Content-Type": "application/json" } };

export function setup() {
  return { startTime: new Date().toISOString() };
}

export default function () {
  const payload = JSON.stringify({
    merchantReference: "k6-fire-" + __VU + "-" + __ITER,
    amount: 10 + Math.random() * 200,
    currency: "USD",
    cardDetails: {
      cardNumber: CARDS[Math.floor(Math.random() * CARDS.length)],
      cvv: "123",
      expiryMonth: "12",
      expiryYear: "2027",
      cardHolderName: "K6 Fire+Forget",
    },
  });

  const res = http.post(`${BASE}/payments`, payload, PARAMS);
  check(res, {
    "create accepted": (r) => r.status >= 200 && r.status < 300,
  });

  // Do NOT wait — fire and forget.
  // Kafka buffers the events and consumers catch up asynchronously.
  sleep(0.1);
}
