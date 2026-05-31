import { useState, useEffect, useRef } from "react";
import { EventEnvelope, PaymentResponse } from "../types";
import { fetchPayment, capturePayment, refundPayment, voidPayment } from "../api";

const ORDER = [
  "CREATED",
  "RISK_ASSESSED",
  "ROUTING_COMPLETED",
  "AUTHORIZED",
  "CAPTURED",
  "VOID",
  "REFUNDED",
  "PARTIALLY_REFUNDED",
];

const TERMINAL = new Set(["REJECTED", "FAILED", "VOID", "REFUNDED", "PARTIALLY_REFUNDED"]);

function eventToState(e: EventEnvelope): string | null {
  const p = e.payload as Record<string, unknown>;
  switch (e.topic) {
    case "payment-requests":
      return "CREATED";
    case "risk-assessed":
      return "RISK_ASSESSED";
    case "payment-rejected":
      return "REJECTED";
    case "routing-completed":
      return "ROUTING_COMPLETED";
    case "authorization-completed":
      return p.success ? "AUTHORIZED" : "FAILED";
    case "capture-completed":
      return p.success ? "CAPTURED" : "FAILED";
    case "void-completed":
      return p.success ? "VOID" : "FAILED";
    case "refund-completed":
      return p.status === "REFUNDED" ? "REFUNDED" : "PARTIALLY_REFUNDED";
    default:
      return null;
  }
}

function stateIndex(state: string): number {
  const i = ORDER.indexOf(state);
  return i >= 0 ? i : ORDER.length;
}

function shortSummary(e: EventEnvelope): string {
  const p = e.payload as Record<string, unknown>;
  switch (e.topic) {
    case "payment-requests":
      return "Payment created";
    case "risk-assessed": {
      const rl = p.riskLevel ?? "?";
      return `Risk: ${rl}`;
    }
    case "payment-rejected":
      return `Rejected: ${p.reason ?? ""}`;
    case "routing-completed":
      return `Network: ${p.selectedPaymentMethod ?? "?"} cost=${p.estimatedCost ?? "?"}`;
    case "authorization-completed":
      return p.success
        ? `RRN: ${p.rrn ?? ""} auth: ${p.authCode ?? ""}`
        : `Failed: ${p.errorMessage ?? ""}`;
    case "capture-completed":
      return p.success
        ? `Capture: ${p.captureId ?? ""}`
        : `Capture failed: ${p.errorMessage ?? ""}`;
    case "void-completed":
      return p.success ? "Voided" : `Void failed: ${p.errorMessage ?? ""}`;
    case "refund-completed":
      return `Refund: ${p.status ?? ""}`;
    default:
      return "";
  }
}

function fmtMs(ms: number): string {
  if (ms < 1000) return `${ms} ms`;
  return `${(ms / 1000).toFixed(2)} s`;
}

interface TimelineNode {
  state: string;
  timestamp: number;
  detail: string;
}

interface Props {
  paymentId: string | null;
  events: EventEnvelope[];
  onStop: () => void;
  onTrack: (pid: string) => void;
}

export default function LiveTracker({ paymentId, events, onStop, onTrack }: Props) {
  const [timeline, setTimeline] = useState<TimelineNode[]>([]);
  const [reachedStates, setReachedStates] = useState<Set<string>>(new Set());
  const [terminal, setTerminal] = useState(false);
  const [tStart, setTStart] = useState<number | null>(null);
  const [tEnd, setTEnd] = useState<number | null>(null);
  const [now, setNow] = useState(Date.now());
  const [paymentData, setPaymentData] = useState<PaymentResponse | null>(null);
  const [showRaw, setShowRaw] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const [actionMsg, setActionMsg] = useState("");
  const seenRef = useRef(new Set<string>());
  const pollTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const tickRef = useRef<ReturnType<typeof setInterval> | null>(null);

  useEffect(() => {
    seenRef.current = new Set();
    setTimeline([]);
    setReachedStates(new Set());
    setTerminal(false);
    setTStart(null);
    setTEnd(null);
    setPaymentData(null);
    setActionMsg("");

    if (!paymentId) return;

    fetchPayment(paymentId).then((data) => {
      if (data) setPaymentData(data);
    });
  }, [paymentId]);

  useEffect(() => {
    if (!paymentId) return;
    if (terminal) return;
    let changed = false;
    const nextReached = new Set(reachedStates);
    const nextTimeline = [...timeline];
    let nextTStart = tStart;
    let nextTEnd = tEnd;

    for (const e of events) {
      if (e.paymentId !== paymentId) continue;
      const key = `${e.topic}:${e.partition}:${e.offset}`;
      if (seenRef.current.has(key)) continue;
      seenRef.current.add(key);

      const state = eventToState(e);
      if (!state) continue;

      nextReached.add(state);
      nextTimeline.push({
        state,
        timestamp: e.timestamp,
        detail: shortSummary(e),
      });
      changed = true;

      if (!nextTStart) {
        nextTStart = e.timestamp;
      }

      if ((state === "AUTHORIZED" || TERMINAL.has(state)) && !nextTEnd) {
        nextTEnd = e.timestamp;
      }
    }

    if (changed) {
      setReachedStates(nextReached);
      setTimeline(nextTimeline);
      if (nextTStart !== tStart) setTStart(nextTStart);
      if (nextTEnd !== tEnd) setTEnd(nextTEnd);

      const isDone = [...nextReached].some((s) => TERMINAL.has(s));
      if (isDone) {
        setTerminal(true);
      }

      if (pollTimerRef.current) clearTimeout(pollTimerRef.current);
      pollTimerRef.current = setTimeout(async () => {
        const data = await fetchPayment(paymentId);
        if (data) setPaymentData(data);
      }, 300);
    }
  }, [events, paymentId, terminal, reachedStates, timeline, tStart, tEnd]);

  useEffect(() => {
    if (!paymentId || terminal) {
      if (tickRef.current) clearInterval(tickRef.current);
      return;
    }
    tickRef.current = setInterval(() => setNow(Date.now()), 100);
    return () => {
      if (tickRef.current) clearInterval(tickRef.current);
    };
  }, [paymentId, terminal]);

  useEffect(() => {
    return () => {
      if (pollTimerRef.current) clearTimeout(pollTimerRef.current);
      if (tickRef.current) clearInterval(tickRef.current);
    };
  }, []);

  const getNodeClass = (nodeState: string): string => {
    if (nodeState === "REJECTED" || nodeState === "FAILED")
      return "timeline-node timeline-node--failed";
    if (reachedStates.has(nodeState)) return "timeline-node timeline-node--done";
    const highest = [...reachedStates].reduce(
      (max, s) => Math.max(max, stateIndex(s)),
      -1
    );
    if (stateIndex(nodeState) === highest + 1 && highest < ORDER.length)
      return "timeline-node timeline-node--active";
    return "timeline-node timeline-node--pending";
  };

  const formatTs = (ts: number) => {
    const d = new Date(ts);
    return d.toLocaleTimeString("en-GB", { hour12: false }) + "." +
      String(d.getMilliseconds()).padStart(3, "0");
  };

  const handleSearch = () => {
    const trimmed = searchInput.trim();
    if (!trimmed) return;
    onTrack(trimmed);
  };

  const pt = paymentData;

  const elapsedMs = tStart ? ((tEnd ?? now) - tStart) : null;
  const elapsedLabel = (() => {
    if (elapsedMs === null) return null;
    if (tEnd && (reachedStates.has("REJECTED") || reachedStates.has("FAILED")))
      return { text: `Failed after: ${fmtMs(elapsedMs)}`, cls: "tracker-elapsed tracker-elapsed--failed" };
    if (tEnd && reachedStates.has("AUTHORIZED"))
      return { text: `Total: ${fmtMs(elapsedMs)}`, cls: "tracker-elapsed tracker-elapsed--done" };
    if (tEnd)
      return { text: `Total: ${fmtMs(elapsedMs)}`, cls: "tracker-elapsed tracker-elapsed--done" };
    return { text: `Elapsed: ${fmtMs(elapsedMs)}`, cls: "tracker-elapsed tracker-elapsed--running" };
  })();

  const canCapture = pt?.paymentStatus === "AUTHORIZED";
  const canVoid = pt?.paymentStatus === "AUTHORIZED";
  const canRefund = pt?.paymentStatus === "CAPTURED" || pt?.paymentStatus === "PARTIALLY_REFUNDED";

  const allNodes = [...ORDER, "REJECTED", "FAILED"];

  const trackerInner = paymentId ? (
    <>
      <div className="tracker-header">
        <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
          <span className="tracker-pid" title={paymentId}>
            {paymentId.slice(0, 12)}...
          </span>
          {pt && (
            <span
              className={`badge ${
                pt.paymentStatus === "AUTHORIZED" || pt.paymentStatus === "CAPTURED" || pt.paymentStatus === "REFUNDED"
                  ? "badge-success"
                  : pt.paymentStatus === "REJECTED" || pt.paymentStatus === "FAILED"
                  ? "badge-danger"
                  : "badge-info"
              }`}
            >
              {pt.paymentStatus}
            </span>
          )}
          {elapsedLabel && (
            <span className={elapsedLabel.cls}>{elapsedLabel.text}</span>
          )}
        </div>
        <button className="btn btn-secondary btn-sm" onClick={onStop}>
          Stop Tracking
        </button>
      </div>

      <div className="timeline" style={{ marginTop: 12 }}>
        {allNodes.map((s) => {
          const node = timeline.find((n) => n.state === s);
          const cls = getNodeClass(s);
          return (
            <div key={s} className="timeline-row">
              <div className={cls}>{s}</div>
              {node ? (
                <div className="timeline-meta">
                  <span className="timeline-time">{formatTs(node.timestamp)}</span>
                  <span className="timeline-detail">{node.detail}</span>
                </div>
              ) : (
                <div className="timeline-meta" />
              )}
            </div>
          );
        })}
      </div>

      {pt && (
        <div style={{ marginTop: 16 }}>
          <div className="card-title">Payment Details</div>
          <div className="kv-grid">
            <span className="k">Payment ID</span>
            <span className="v">{pt.paymentId}</span>
            <span className="k">Amount</span>
            <span className="v">
              {pt.amount
                ? `${(pt.amount.amount / 100).toFixed(2)} ${pt.amount.currency}`
                : "-"}
            </span>
            <span className="k">Status</span>
            <span className="v">{pt.paymentStatus}</span>
            <span className="k">RRN</span>
            <span className="v">{pt.rrn ?? "-"}</span>
            <span className="k">Auth Code</span>
            <span className="v">{pt.authCode ?? "-"}</span>
            <span className="k">Processor TXN ID</span>
            <span className="v">{pt.processorTransactionId ?? "-"}</span>
            <span className="k">Capture ID</span>
            <span className="v">{pt.captureId ?? "-"}</span>
            <span className="k">Error</span>
            <span className="v" style={{ color: "var(--danger)" }}>
              {pt.message ?? "-"}
            </span>
            <span className="k">Created</span>
            <span className="v">{pt.createdAt ? formatTs(new Date(pt.createdAt).getTime()) : "-"}</span>
          </div>
        </div>
      )}

      {timeline.length === 0 && (
        <p style={{ marginTop: 8, fontSize: 12, color: "var(--text-muted)" }}>
          Waiting for events. The KV grid shows snapshot data; the timeline populates as events arrive.
        </p>
      )}

      <div className="tracker-actions">
        <button className="btn btn-sm" onClick={async () => {
          setActionMsg("Capturing...");
          try { await capturePayment(paymentId); } catch (e: unknown) { setActionMsg("Error: " + (e as Error).message); }
        }} disabled={!canCapture}>
          Capture
        </button>
        <button className="btn btn-sm btn-secondary" onClick={async () => {
          setActionMsg("Voiding...");
          try { await voidPayment(paymentId); } catch (e: unknown) { setActionMsg("Error: " + (e as Error).message); }
        }} disabled={!canVoid}>
          Void
        </button>
        <button className="btn btn-sm btn-secondary" onClick={async () => {
          setActionMsg("Refunding...");
          try {
            const amt = pt?.amount ? (pt.amount.amount / 100) : 0;
            await refundPayment(paymentId, amt);
          } catch (e: unknown) { setActionMsg("Error: " + (e as Error).message); }
        }} disabled={!canRefund}>
          Refund
        </button>
        {actionMsg && (
          <span style={{ fontSize: 12, color: "var(--text-muted)", marginLeft: 8 }}>{actionMsg}</span>
        )}
      </div>

      {timeline.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <button
            className="btn btn-secondary btn-sm"
            onClick={() => setShowRaw(!showRaw)}
          >
            {showRaw ? "Hide" : "Show"} Raw Events ({timeline.length})
          </button>
          {showRaw && (
            <div className="tape" style={{ marginTop: 8, height: 180 }}>
              {timeline.map((n, i) => (
                <div key={i} className="tape-line">
                  <span className="topic">{n.state}</span>{" "}
                  <span className="id">{formatTs(n.timestamp)}</span>{" "}
                  <span className="payload">{n.detail}</span>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </>
  ) : (
    <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
      Enter a payment ID above or create a payment to start tracking.
    </p>
  );

  return (
    <div className="card">
      <div className="tracker-search">
        <input
          type="text"
          placeholder="Paste payment ID to track..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && handleSearch()}
        />
        <button className="btn btn-sm" onClick={handleSearch}>
          Track
        </button>
      </div>
      {trackerInner}
    </div>
  );
}
