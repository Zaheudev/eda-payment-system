import { useState } from "react";
import { EventEnvelope } from "../types";
import { createPayment, capturePayment, refundPayment, voidPayment } from "../api";

const emptyCard = {
  cardNumber: "4111111111111111",
  cvv: "123",
  expiryMonth: "12",
  expiryYear: "2027",
  cardHolderName: "John Doe",
};

export default function PaymentForm({
  onEvent,
  onSuccess,
  onTrack,
}: {
  onEvent: (e: EventEnvelope) => void;
  onSuccess?: () => void;
  onTrack?: (pid: string) => void;
}) {
  const [merchantRef, setMerchantRef] = useState("order-" + Date.now().toString(36));
  const [amount, setAmount] = useState(99.99);
  const [currency, setCurrency] = useState("USD");
  const [card, setCard] = useState(emptyCard);
  const [lastPid, setLastPid] = useState("");
  const [message, setMessage] = useState("");

  const handleCreate = async () => {
    setMessage("Sending...");
    try {
      const res = await createPayment({
        merchantReference: merchantRef,
        amount,
        currency,
        cardDetails: card,
      });
      const pid = res.paymentId || res.data?.paymentId || "";
      setLastPid(pid);
      setMessage(`Created: ${pid}`);
      setMerchantRef("order-" + Date.now().toString(36));
      setAmount(Math.floor(Math.random() * 500) + 10);
      onSuccess?.();
      onTrack?.(pid);
    } catch (e: unknown) {
      setMessage("Error: " + (e as Error).message);
    }
  };

  const handleCapture = async () => {
    if (!lastPid) return;
    setMessage("Capturing...");
    try {
      await capturePayment(lastPid);
      setMessage("Capture sent for " + lastPid);
      onSuccess?.();
    } catch (e: unknown) {
      setMessage("Error: " + (e as Error).message);
    }
  };

  const handleRefund = async () => {
    if (!lastPid) return;
    setMessage("Refunding...");
    try {
      await refundPayment(lastPid, amount);
      setMessage("Refund sent for " + lastPid);
      onSuccess?.();
    } catch (e: unknown) {
      setMessage("Error: " + (e as Error).message);
    }
  };

  const handleVoid = async () => {
    if (!lastPid) return;
    setMessage("Voiding...");
    try {
      await voidPayment(lastPid);
      setMessage("Void sent for " + lastPid);
      onSuccess?.();
    } catch (e: unknown) {
      setMessage("Error: " + (e as Error).message);
    }
  };

  return (
    <div className="card">
      <div className="card-title">Create Payment</div>
      <div className="form-row">
        <div className="form-group">
          <label>Merchant Ref</label>
          <input value={merchantRef} onChange={(e) => setMerchantRef(e.target.value)} />
        </div>
        <div className="form-group" style={{ flex: 0.5 }}>
          <label>Amount</label>
          <input
            type="number"
            value={amount}
            onChange={(e) => setAmount(Number(e.target.value))}
          />
        </div>
        <div className="form-group" style={{ flex: 0.4 }}>
          <label>Currency</label>
          <select value={currency} onChange={(e) => setCurrency(e.target.value)}>
            <option>USD</option>
            <option>EUR</option>
            <option>RON</option>
          </select>
        </div>
      </div>
      <div className="form-row">
        <div className="form-group" style={{ flex: 2 }}>
          <label>Card Number</label>
          <input
            value={card.cardNumber}
            onChange={(e) => setCard({ ...card, cardNumber: e.target.value })}
          />
        </div>
        <div className="form-group" style={{ flex: 0.5 }}>
          <label>CVV</label>
          <input
            value={card.cvv}
            onChange={(e) => setCard({ ...card, cvv: e.target.value })}
          />
        </div>
        <div className="form-group" style={{ flex: 0.5 }}>
          <label>Expiry</label>
          <input
            value={`${card.expiryMonth}/${card.expiryYear}`}
            onChange={(e) => {
              const [m, y] = e.target.value.split("/");
              if (m) setCard({ ...card, expiryMonth: m });
              if (y) setCard({ ...card, expiryYear: y });
            }}
          />
        </div>
        <div className="form-group" style={{ flex: 1 }}>
          <label>Cardholder</label>
          <input
            value={card.cardHolderName}
            onChange={(e) => setCard({ ...card, cardHolderName: e.target.value })}
          />
        </div>
      </div>
      <div style={{ display: "flex", gap: 8, marginTop: 8 }}>
        <button className="btn" onClick={handleCreate}>
          Create Payment
        </button>
        <button className="btn btn-secondary" onClick={handleCapture} disabled={!lastPid}>
          Capture
        </button>
        <button className="btn btn-secondary" onClick={handleVoid} disabled={!lastPid}>
          Void
        </button>
        <button className="btn btn-secondary" onClick={handleRefund} disabled={!lastPid}>
          Refund
        </button>
      </div>
      {message && (
        <p style={{ marginTop: 8, fontSize: 12, color: "var(--text-muted)" }}>{message}</p>
      )}
      {lastPid && (
        <p style={{ marginTop: 4, fontSize: 12, fontFamily: "var(--mono)" }}>
          Last: <strong>{lastPid}</strong>
        </p>
      )}
    </div>
  );
}
