import { useState, useEffect, useRef } from "react";
import { EventEnvelope, Tab } from "./types";
import PaymentForm from "./components/PaymentForm";
import PaymentsList from "./components/PaymentsList";
import LiveTracker from "./components/LiveTracker";
import EventTape from "./components/EventTape";
import SagaPanel from "./components/SagaPanel";
import DltPanel from "./components/DltPanel";
import ChaosPanel from "./components/ChaosPanel";
import ServiceGraph from "./components/ServiceGraph";

type EventsByTopic = Record<string, number>;

export default function App() {
  const [tab, setTab] = useState<Tab>("payments");
  const [refreshKey, setRefreshKey] = useState(0);
  const [events, setEvents] = useState<EventEnvelope[]>([]);
  const [topicCounts, setTopicCounts] = useState<EventsByTopic>({});
  const [sseConnected, setSseConnected] = useState(false);
  const [trackedPaymentId, setTrackedPaymentId] = useState<string | null>(null);
  const eventSourceRef = useRef<EventSource | null>(null);

  const appendEvent = (e: EventEnvelope) => {
    setEvents((prev) => [...prev.slice(-499), e]);
    setTopicCounts((prev) => ({ ...prev, [e.topic]: (prev[e.topic] || 0) + 1 }));
  };

  useEffect(() => {
    const es = new EventSource("/api/bff/events/stream");
    eventSourceRef.current = es;

    es.onopen = () => setSseConnected(true);
    es.onerror = () => setSseConnected(false);

    es.onmessage = (msg) => {
      try {
        const envelope: EventEnvelope = JSON.parse(msg.data);
        appendEvent(envelope);
      } catch {
        // skip unparseable
      }
    };

    return () => {
      es.close();
      eventSourceRef.current = null;
    };
  }, []);

  const tabs: { key: Tab; label: string }[] = [
    { key: "payments", label: "Payments" },
    { key: "graph", label: "Service Graph" },
    { key: "tape", label: "Event Tape" },
    { key: "saga", label: "Saga States" },
    { key: "dlt", label: "Dead Letters" },
    { key: "chaos", label: "Chaos" },
  ];

  return (
    <div className="app">
      <header className="app-header">
        <h1>Payment Gateway EDA Demo</h1>
        <nav>
          {tabs.map((t) => (
            <button
              key={t.key}
              className={tab === t.key ? "active" : ""}
              onClick={() => setTab(t.key)}
            >
              {t.label}
            </button>
          ))}
        </nav>
      </header>

      <div className="app-content">
        {tab === "payments" && (
          <>
            <div className="grid-2" style={{ marginBottom: 16 }}>
              <PaymentForm
                onEvent={appendEvent}
                onSuccess={() => setRefreshKey((k) => k + 1)}
                onTrack={(pid) => setTrackedPaymentId(pid)}
              />
              <PaymentsList key={refreshKey} events={events} onSuccess={() => setRefreshKey((k) => k + 1)} />
            </div>
            <LiveTracker
              paymentId={trackedPaymentId}
              events={events}
              onStop={() => setTrackedPaymentId(null)}
              onTrack={(pid) => setTrackedPaymentId(pid)}
            />
          </>
        )}

        {tab !== "payments" && (
          <div className="grid-2" style={{ marginBottom: 16 }}>
            <div className="card">
              <div className="card-title">Topic Throughput</div>
              {Object.keys(topicCounts).length === 0 ? (
                <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
                  No events yet. Submit a payment to see activity.
                </p>
              ) : (
                <table className="table">
                  <thead>
                    <tr>
                      <th>Topic</th>
                      <th>Events</th>
                    </tr>
                  </thead>
                  <tbody>
                    {Object.entries(topicCounts)
                      .sort(([, a], [, b]) => b - a)
                      .map(([topic, count]) => (
                        <tr key={topic}>
                          <td style={{ fontFamily: "var(--mono)", fontSize: 12 }}>{topic}</td>
                          <td>{count}</td>
                        </tr>
                      ))}
                  </tbody>
                </table>
              )}
            </div>
          </div>
        )}

        {tab === "graph" && <ServiceGraph events={events} />}
        {tab === "tape" && <EventTape events={events} />}
        {tab === "saga" && <SagaPanel />}
        {tab === "dlt" && <DltPanel />}
        {tab === "chaos" && <ChaosPanel />}
      </div>
    </div>
  );
}
