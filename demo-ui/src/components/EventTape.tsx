import { useEffect, useRef } from "react";
import { EventEnvelope } from "../types";

export default function EventTape({ events }: { events: EventEnvelope[] }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (ref.current) {
      ref.current.scrollTop = ref.current.scrollHeight;
    }
  }, [events.length]);

  if (events.length === 0) {
    return (
      <div className="card">
        <div className="card-title">Live Event Tape</div>
        <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
          Waiting for events. Create a payment to see the live stream.
        </p>
      </div>
    );
  }

  return (
    <div className="card">
      <div className="card-title">Live Event Tape ({events.length} events)</div>
      <div className="tape" ref={ref}>
        {events.map((e, i) => (
          <div className="tape-line" key={i}>
            <span className="topic">[{e.topic}]</span>{" "}
            {e.paymentId && <span className="id">{e.paymentId}</span>}{" "}
            <span className="payload">
              {JSON.stringify(e.payload).substring(0, 120)}
            </span>
          </div>
        ))}
      </div>
    </div>
  );
}
