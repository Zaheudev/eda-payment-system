import http from "k6/http";
import { sleep } from "k6";

const BASE = "http://localhost:8080/api/v1";

/**
 * Poll GET /api/v1/payments/{paymentId} until the payment reaches one
 * of the expected states or hits the timeout.
 *
 * @param {string}  paymentId
 * @param {string|string[]} expected  e.g. "AUTHORIZED" or ["CAPTURED", "REFUNDED"]
 * @param {object}  [opts]
 * @param {number}  [opts.maxWaitMs=10000]  maximum total wait time
 * @returns {{reached: boolean, status: string, ms: number}}
 */
export function waitForStatus(paymentId, expected, opts = {}) {
  const maxWaitMs = opts.maxWaitMs || 10000;
  const start = Date.now();
  let delay = 100;
  const expectedSet = new Set(Array.isArray(expected) ? expected : [expected]);

  while (Date.now() - start < maxWaitMs) {
    const res = http.get(`${BASE}/payments/${paymentId}`);
    if (res.status === 200) {
      const body = JSON.parse(res.body);
      if (expectedSet.has(body.paymentStatus)) {
        return { reached: true, status: body.paymentStatus, ms: Date.now() - start };
      }
      if (body.paymentStatus === "REJECTED" || body.paymentStatus === "FAILED") {
        return { reached: false, status: body.paymentStatus, ms: Date.now() - start };
      }
    }
    sleep(delay / 1000);
    delay = Math.min(delay * 1.5, 1000);
  }
  return { reached: false, status: "TIMEOUT", ms: Date.now() - start };
}
