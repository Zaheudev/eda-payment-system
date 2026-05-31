const BASE = "";

export async function createPayment(data: {
  merchantReference: string;
  amount: number;
  currency: string;
  cardDetails: {
    cardNumber: string;
    cvv: string;
    expiryMonth: string;
    expiryYear: string;
    cardHolderName: string;
  };
}) {
  const res = await fetch(`${BASE}/api/v1/payments`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
  return res.json();
}

export async function capturePayment(paymentId: string) {
  const res = await fetch(`${BASE}/api/v1/capture/${paymentId}`, { method: "POST" });
  return res.json();
}

export async function refundPayment(paymentId: string, amount: number) {
  const res = await fetch(`${BASE}/api/v1/refund/${paymentId}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ amount, currency: "USD" }),
  });
  return res.json();
}

export async function voidPayment(paymentId: string) {
  const res = await fetch(`${BASE}/api/v1/void/${paymentId}`, { method: "POST" });
  return res.json();
}

export async function fetchAllPayments() {
  const res = await fetch(`${BASE}/api/v1/payments`);
  if (!res.ok) return [];
  return res.json();
}

export async function fetchPayment(paymentId: string) {
  const res = await fetch(`${BASE}/api/v1/payments/${paymentId}`);
  if (!res.ok) return null;
  return res.json();
}

export async function fetchSaga(paymentId: string) {
  const res = await fetch(`/api/bff/sagas/${paymentId}`);
  if (!res.ok) return null;
  return res.json();
}

export async function fetchAllSagas() {
  const res = await fetch("/api/bff/sagas");
  return res.json();
}

export async function fetchDltTopics(): Promise<string[]> {
  const res = await fetch("/api/bff/dlt/topics");
  return res.json();
}

export async function fetchDltMessages(topic: string, limit = 10) {
  const res = await fetch(`/api/bff/dlt/${topic}?limit=${limit}`);
  return res.json();
}

export async function replayDltMessage(topic: string, partition: number, offset: number) {
  const res = await fetch(`/api/bff/dlt/${topic}/replay/${partition}/${offset}`, { method: "POST" });
  return res.json();
}

export async function fetchContainers() {
  const res = await fetch("/api/bff/chaos/containers");
  return res.json();
}

export async function killContainer(name: string) {
  const res = await fetch(`/api/bff/chaos/kill/${name}`, { method: "POST" });
  return res.json();
}

export async function startContainer(name: string) {
  const res = await fetch(`/api/bff/chaos/start/${name}`, { method: "POST" });
  return res.json();
}

export async function addToxic(proxyName: string, type: string, opts?: Record<string, unknown>) {
  const res = await fetch(`/api/bff/chaos/toxic/${proxyName}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ type, ...opts }),
  });
  return res.json();
}

export async function removeToxics(proxyName: string) {
  const res = await fetch(`/api/bff/chaos/toxic/${proxyName}`, { method: "DELETE" });
  return res.json();
}
