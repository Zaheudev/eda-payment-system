import { useEffect, useState, useMemo } from "react";
import { PaymentResponse, EventEnvelope } from "../types";
import { fetchAllPayments } from "../api";
import PaymentDetailModal from "./PaymentDetailModal";

interface Props {
  events: EventEnvelope[];
  refreshKey: number;
  onSuccess: () => void;
}

export default function PaymentsList({ events, refreshKey, onSuccess }: Props) {
  const [payments, setPayments] = useState<PaymentResponse[]>([]);
  const [selectedPid, setSelectedPid] = useState<string | null>(null);

  const refresh = () => {
    fetchAllPayments(20)
      .then((data) => setPayments(Array.isArray(data) ? data : []))
      .catch(() => {});
  };

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, 5000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    refresh();
  }, [refreshKey]);

  const sortedPayments = useMemo(
    () =>
      [...payments].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      ),
    [payments]
  );

  const statusClass = (status: string) => {
    switch (status) {
      case "AUTHORIZED":
      case "CAPTURED":
      case "REFUNDED":
        return "badge-success";
      case "REJECTED":
      case "FAILED":
        return "badge-danger";
      case "VOID":
        return "badge-warning";
      default:
        return "badge-info";
    }
  };

  return (
    <>
      <div className="card">
        <div className="card-title" style={{ display: "flex", justifyContent: "space-between" }}>
          <span>Payments (latest 20)</span>
          <button className="btn btn-sm btn-secondary" onClick={refresh}>
            Refresh
          </button>
        </div>
        {payments.length === 0 ? (
          <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
            No payments yet. Submit a payment using the form on the left.
          </p>
        ) : (
          <div className="payments-scroll">
            <table className="table">
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Status</th>
                  <th>Amount</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {sortedPayments.map((p) => (
                  <tr
                    key={p.paymentId}
                    className="clickable"
                    onClick={() => setSelectedPid(p.paymentId)}
                  >
                    <td style={{ fontFamily: "var(--mono)", fontSize: 12 }}>
                      {p.paymentId?.substring(0, 16)}...
                    </td>
                    <td>
                      <span className={`badge ${statusClass(p.paymentStatus)}`}>
                        {p.paymentStatus}
                      </span>
                    </td>
                    <td>
                      {p.amount
                        ? `${(p.amount.amount / 100).toFixed(2)} ${p.amount.currency}`
                        : "-"}
                    </td>
                    <td style={{ fontSize: 12, color: "var(--text-muted)" }}>
                      {p.createdAt ? new Date(p.createdAt).toLocaleString() : "-"}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selectedPid && (
        <PaymentDetailModal
          paymentId={selectedPid}
          events={events}
          onClose={() => setSelectedPid(null)}
          onAction={() => {
            refresh();
            onSuccess();
          }}
        />
      )}
    </>
  );
}
