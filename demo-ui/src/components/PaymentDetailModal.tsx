import { useEffect, useState, useCallback } from "react";
import { PaymentResponse, SagaState, EventEnvelope } from "../types";
import { fetchPayment, fetchSaga, capturePayment, refundPayment, voidPayment } from "../api";

interface Props {
  paymentId: string;
  events: EventEnvelope[];
  onClose: () => void;
  onAction: () => void;
}

export default function PaymentDetailModal({ paymentId, events, onClose, onAction }: Props) {
  const [payment, setPayment] = useState<PaymentResponse | null>(null);
  const [saga, setSaga] = useState<SagaState | null>(null);
  const [message, setMessage] = useState("");

  const load = useCallback(async () => {
    const [p, s] = await Promise.all([
      fetchPayment(paymentId),
      fetchSaga(paymentId),
    ]);
    setPayment(p);
    setSaga(s);
  }, [paymentId]);

  useEffect(() => {
    load();
    const timer = setInterval(load, 3000);
    return () => clearInterval(timer);
  }, [load]);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => { if (e.key === "Escape") onClose(); };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  const relatedEvents = events
    .filter((e) => e.paymentId === paymentId)
    .slice(-20)
    .reverse();

  const statusClass = (status: string) => {
    switch (status) {
      case "CAPTURED": return "badge-success";
      case "REJECTED":
      case "FAILED": return "badge-danger";
      case "VOID": return "badge-warning";
      default: return "badge-info";
    }
  };

  const handleAction = async (action: () => Promise<unknown>, label: string) => {
    setMessage(`${label}...`);
    try {
      await action();
      setMessage(`${label} sent`);
      onAction();
      load();
    } catch {
      setMessage(`${label} failed`);
    }
  };

  const allStates = ["CREATED", "RISK_ASSESSED", "ROUTED", "AUTHORIZED", "CAPTURED", "VOIDED", "REFUNDED", "REJECTED"];

  return (
    <div className="modal-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="modal">
        <div className="modal-header">
          <div className="modal-header-left">
            <span className="modal-pid">{paymentId}</span>
            {payment && (
              <span className={`badge ${statusClass(payment.paymentStatus)}`}>
                {payment.paymentStatus}
              </span>
            )}
          </div>
          <button className="modal-close" onClick={onClose}>&times;</button>
        </div>

        {payment ? (
          <>
            <div className="modal-section">
              <div className="modal-section-title">Payment Details</div>
              <div className="kv-grid">
                <span className="k">Amount</span>
                <span className="v">
                  {payment.amount
                    ? `${(payment.amount.amount / 100).toFixed(2)} ${payment.amount.currency}`
                    : "-"}
                </span>
                <span className="k">RRN</span>
                <span className="v">{payment.rrn || "-"}</span>
                <span className="k">Auth Code</span>
                <span className="v">{payment.authCode || "-"}</span>
                <span className="k">Processor Tx ID</span>
                <span className="v">{payment.processorTransactionId || "-"}</span>
                <span className="k">Capture ID</span>
                <span className="v">{payment.captureId || "-"}</span>
                <span className="k">Created At</span>
                <span className="v">{payment.createdAt ? new Date(payment.createdAt).toLocaleString() : "-"}</span>
                <span className="k">Message</span>
                <span className="v">{payment.message || "-"}</span>
              </div>
            </div>

            {saga && (
              <div className="modal-section">
                <div className="modal-section-title">Saga Timeline</div>
                <div className="saga-flow">
                  {allStates.map((state, i) => {
                    const idx = saga.history.indexOf(state);
                    const isCurrent = saga.currentState === state;
                    const isDone = idx >= 0 || allStates.indexOf(saga.currentState) > allStates.indexOf(state);
                    const isRejected = saga.currentState === "REJECTED" && state === "REJECTED";
                    let cls = "";
                    if (isRejected) cls = "rejected";
                    else if (isCurrent) cls = "active";
                    else if (isDone) cls = state === "REJECTED" ? "rejected" : "done";
                    return (
                      <span key={state}>
                        {i > 0 && <span className="saga-arrow">&rarr;</span>}
                        <span className={`saga-node ${cls}`}>{state}</span>
                      </span>
                    );
                  })}
                </div>
                <p style={{ marginTop: 8, fontSize: 12, color: "var(--text-muted)" }}>
                  History: {saga.history.length > 0 ? saga.history.join(" > ") : "(none)"}
                  {saga.currentState && <> &rarr; <strong>{saga.currentState}</strong></>}
                </p>
              </div>
            )}

            <div className="modal-section">
              <div className="modal-section-title">Related Events ({relatedEvents.length})</div>
              {relatedEvents.length === 0 ? (
                <p style={{ fontSize: 12, color: "var(--text-muted)" }}>
                  No events captured for this payment yet. Open the Event Tape to catch new events.
                </p>
              ) : (
                <div className="modal-events">
                  {relatedEvents.map((e, i) => (
                    <div className="modal-event" key={i}>
                      <span className="modal-event-time">
                        {new Date(e.timestamp).toLocaleTimeString()}
                      </span>
                      <span className="modal-event-topic">{e.topic}</span>
                      <span className="modal-event-preview" title={JSON.stringify(e.payload)}>
                        {JSON.stringify(e.payload).substring(0, 100)}
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>

            <div className="modal-actions">
              <button
                className="btn btn-sm"
                onClick={() => handleAction(() => capturePayment(paymentId), "Capture")}
                disabled={payment.paymentStatus !== "AUTHORIZED"}
              >
                Capture
              </button>
              <button
                className="btn btn-sm btn-secondary"
                onClick={() => handleAction(() => voidPayment(paymentId), "Void")}
                disabled={payment.paymentStatus !== "AUTHORIZED"}
              >
                Void
              </button>
              <button
                className="btn btn-sm btn-secondary"
                onClick={() => {
                  const amt = payment.amount ? payment.amount.amount / 100 : 0;
                  handleAction(() => refundPayment(paymentId, amt), "Refund");
                }}
                disabled={payment.paymentStatus !== "CAPTURED" && payment.paymentStatus !== "PARTIALLY_REFUNDED"}
              >
                Refund
              </button>
              {message && (
                <span style={{ fontSize: 12, color: "var(--text-muted)", marginLeft: 12, alignSelf: "center" }}>
                  {message}
                </span>
              )}
            </div>
          </>
        ) : (
          <p style={{ color: "var(--text-muted)", fontSize: 13, textAlign: "center", padding: 40 }}>
            Loading payment details...
          </p>
        )}
      </div>
    </div>
  );
}
