import { useEffect, useState } from "react";
import { SagaState } from "../types";
import { fetchAllSagas, fetchSaga } from "../api";

export default function SagaPanel() {
  const [sagas, setSagas] = useState<SagaState[]>([]);
  const [selectedPid, setSelectedPid] = useState("");
  const [detail, setDetail] = useState<SagaState | null>(null);

  useEffect(() => {
    fetchAllSagas().then(setSagas).catch(console.error);
    const timer = setInterval(() => {
      fetchAllSagas().then(setSagas).catch(() => {});
    }, 3000);
    return () => clearInterval(timer);
  }, []);

  const viewSaga = async (pid: string) => {
    setSelectedPid(pid);
    const s = await fetchSaga(pid);
    setDetail(s);
  };

  const allStates = [
    "CREATED",
    "RISK_ASSESSED",
    "ROUTED",
    "AUTHORIZED",
    "CAPTURED",
    "VOIDED",
    "REFUNDED",
    "REJECTED",
  ];

  return (
    <div>
      <div className="grid-2">
        <div className="card">
          <div className="card-title">All Sagas ({sagas.length})</div>
          {sagas.length === 0 ? (
            <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
              No sagas yet. Create a payment first.
            </p>
          ) : (
            <table className="table">
              <thead>
                <tr>
                  <th>Payment ID</th>
                  <th>Current State</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {[...sagas]
                  .sort(
                    (a, b) =>
                      new Date(b.lastUpdatedAt).getTime() -
                      new Date(a.lastUpdatedAt).getTime()
                  )
                  .slice(0, 20)
                  .map((s) => (
                    <tr key={s.paymentId}>
                      <td style={{ fontFamily: "var(--mono)", fontSize: 12 }}>
                        {s.paymentId.substring(0, 16)}...
                      </td>
                      <td>
                        <span
                          className={`badge ${
                            s.currentState === "REJECTED"
                              ? "badge-danger"
                              : s.currentState === "CAPTURED"
                              ? "badge-success"
                              : "badge-info"
                          }`}
                        >
                          {s.currentState}
                        </span>
                      </td>
                      <td>
                        <button
                          className="btn btn-sm btn-secondary"
                          onClick={() => viewSaga(s.paymentId)}
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          )}
        </div>

        <div className="card">
          <div className="card-title">Saga Detail</div>
          {detail ? (
            <div>
              <p style={{ marginBottom: 16, fontSize: 13 }}>
                <strong>{detail.paymentId}</strong>
              </p>
              <div className="saga-flow">
                {allStates.map((state, i) => {
                  const idx = detail.history.indexOf(state);
                  const isCurrent = detail.currentState === state;
                  const isDone =
                    idx >= 0 ||
                    (detail.history.length > 0 &&
                      allStates.indexOf(detail.currentState) >
                        allStates.indexOf(state));
                  const isRejected =
                    detail.currentState === "REJECTED" && state === "REJECTED";
                  let cls = "";
                  if (isRejected) cls = "rejected";
                  else if (isCurrent) cls = "active";
                  else if (isDone)
                    cls = state === "REJECTED" ? "rejected" : "done";

                  return (
                    <span key={state}>
                      {i > 0 && <span className="saga-arrow">&rarr;</span>}
                      <span className={`saga-node ${cls}`}>{state}</span>
                    </span>
                  );
                })}
              </div>
              <p style={{ marginTop: 12, fontSize: 12, color: "var(--text-muted)" }}>
                History: {detail.history.join(" > ")} &rarr;{" "}
                <strong>{detail.currentState}</strong>
              </p>
            </div>
          ) : (
            <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
              Select a saga from the list.
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
