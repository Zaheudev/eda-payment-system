import { useEffect, useState } from "react";
import { ContainerInfo } from "../types";
import { addToxic, bounceContainer, fetchContainers, killContainer, removeToxics, startContainer } from "../api";

export default function ChaosPanel() {
  const [containers, setContainers] = useState<ContainerInfo[]>([]);
  const [status, setStatus] = useState("");

  const refresh = async () => {
    try {
      const c = await fetchContainers();
      setContainers(c);
    } catch {
      setContainers([]);
    }
  };

  useEffect(() => {
    refresh();
    const t = setInterval(refresh, 5000);
    return () => clearInterval(t);
  }, []);

  const handleKill = async (name: string) => {
    setStatus(`Killing ${name}...`);
    try {
      await killContainer(name);
      setStatus(`${name} killed`);
    } catch {
      setStatus(`Failed to kill ${name}`);
    }
    refresh();
  };

  const handleStart = async (name: string) => {
    setStatus(`Starting ${name}...`);
    try {
      await startContainer(name);
      setStatus(`${name} started`);
    } catch {
      setStatus(`Failed to start ${name}`);
    }
    refresh();
  };

  const handleBounce = async (name: string) => {
    setStatus(`Bouncing ${name} (5s delay)...`);
    try {
      await bounceContainer(name);
      setStatus(`${name} bouncing...`);
    } catch {
      setStatus(`Failed to bounce ${name}`);
    }
    setTimeout(refresh, 6000);
  };

  const paymentContainers = containers.filter((c) => {
    const names = Array.isArray(c.names) ? c.names.join(",") : c.names ?? "";
    return /\/(payment|card)-/.test(names);
  });

  return (
    <div>
      <div className="card">
        <div className="card-title">
          Service Containers ({paymentContainers.length})
        </div>
        {paymentContainers.length === 0 ? (
          <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
            No payment service containers detected. Make sure demo-bff has Docker
            socket access (mount /var/run/docker.sock).
          </p>
        ) : (
          <div className="chaos-grid">
            {paymentContainers.map((c) => {
              const name = Array.isArray(c.names)
                ? c.names[0]?.replace("/", "")
                : c.names?.replace("/", "");
              const running = c.state === "running";
              return (
                <div className="chaos-card" key={c.id}>
                  <div className="name">{name}</div>
                  <div className="status">
                    Status:{" "}
                    <span
                      className={`badge ${
                        running ? "badge-success" : "badge-danger"
                      }`}
                    >
                      {c.state}
                    </span>
                  </div>
                  <div className="actions">
                    {running ? (
                      <>
                        <button
                          className="btn btn-sm btn-danger"
                          onClick={() => handleKill(name)}
                        >
                          Kill
                        </button>
                        <button
                          className="btn btn-sm btn-secondary"
                          onClick={() => handleBounce(name)}
                        >
                          Bounce
                        </button>
                      </>
                    ) : (
                      <button
                        className="btn btn-sm btn-secondary"
                        onClick={() => handleStart(name)}
                      >
                        Start
                      </button>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      <div className="card">
        <div className="card-title">Toxiproxy Latency</div>
        <p style={{ color: "var(--text-muted)", fontSize: 13, marginBottom: 8 }}>
          Inject network latency, bandwidth limits, or connection drops on Kafka
          connections (requires Toxiproxy in docker-compose).
        </p>
        <div style={{ display: "flex", gap: 8 }}>
          <button
            className="btn btn-sm btn-secondary"
            onClick={async () => {
              setStatus("Adding 500ms latency...");
              try {
                await addToxic("kafka", "latency", { latency: 500 });
                setStatus("500ms latency added");
              } catch {
                setStatus("Failed (is Toxiproxy running?)");
              }
            }}
          >
            +500ms Latency
          </button>
          <button
            className="btn btn-sm btn-secondary"
            onClick={async () => {
              setStatus("Adding 1mbps limit...");
              try {
                await addToxic("kafka", "bandwidth", { rate: 1024 });
                setStatus("Bandwidth limited to 1Mbps");
              } catch {
                setStatus("Failed (is Toxiproxy running?)");
              }
            }}
          >
            Limit 1Mbps
          </button>
          <button
            className="btn btn-sm btn-danger"
            onClick={async () => {
              setStatus("Removing all toxics...");
              try {
                await removeToxics("kafka");
                setStatus("All toxics removed");
              } catch {
                setStatus("Failed");
              }
            }}
          >
            Remove All Toxics
          </button>
        </div>
        {status && (
          <p style={{ marginTop: 8, fontSize: 12, color: "var(--text-muted)" }}>
            {status}
          </p>
        )}
      </div>
    </div>
  );
}
