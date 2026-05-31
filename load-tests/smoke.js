import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 1,
  iterations: 5,
  thresholds: {
    http_req_duration: ["p(95)<2000"],
    http_req_failed: ["rate<0.1"],
  },
};

const BASE = "http://localhost:8080/api/v1";

export default function () {
  const payload = JSON.stringify({
    merchantReference: "k6-smoke-" + __VU + "-" + __ITER,
    amount: 49.99,
    currency: "USD",
    cardDetails: {
      cardNumber: "4111111111111111",
      cvv: "123",
      expiryMonth: "12",
      expiryYear: "2027",
      cardHolderName: "K6 Tester",
    },
  });

  const params = { headers: { "Content-Type": "application/json" } };

  const res = http.post(`${BASE}/payments`, payload, params);
  check(res, {
    "status is 2xx": (r) => r.status >= 200 && r.status < 300,
  });
  sleep(1);
}
