import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter } from "k6/metrics";

const tCreate = new Trend("eda_create_latency_ms");
const totalCreated = new Counter("eda_payments_created");

export const options = {
  stages: [
    { duration: "30s", target: 10 },
    { duration: "2m", target: 20 },
    { duration: "2m", target: 20 },
    { duration: "30s", target: 0 },
  ],
  thresholds: {
    "eda_create_latency_ms": ["p(95)<3000"],
    http_req_failed: ["rate<0.1"],
  },
};

const BASE = "http://localhost:8080/api/v1";
const CARDS = ["4111111111111111", "5111111111111111", "371111111111111", "6011111111111117"];
const PARAMS = { headers: { "Content-Type": "application/json" } };

export default function () {
  const payload = JSON.stringify({
    merchantReference: "k6-sustained-" + __VU + "-" + __ITER,
    amount: 10 + Math.random() * 200,
    currency: "USD",
    cardDetails: {
      cardNumber: CARDS[Math.floor(Math.random() * CARDS.length)],
      cvv: "123",
      expiryMonth: "12",
      expiryYear: "2027",
      cardHolderName: "K6 Sustained",
    },
  });

  const start = Date.now();
  const res = http.post(`${BASE}/payments`, payload, PARAMS);
  tCreate.add(Date.now() - start);

  check(res, {
    "create accepted": (r) => r.status >= 200 && r.status < 300,
  });

  totalCreated.add(1);

  // Fire-and-forget. Kafka decouples the producer from the async
  // authorization pipeline. The gateway never blocks.

  sleep(0.5 + Math.random());
}
