import { useEffect, useState } from "react";
import { fetchDltTopics, fetchDltMessages, replayDltMessage } from "../api";

interface DltMessage {
  offset: number;
  partition: number;
  key: string;
  timestamp: number;
  value: string;
}

export default function DltPanel() {
  const [topics, setTopics] = useState<string[]>([]);
  const [selectedTopic, setSelectedTopic] = useState("");
  const [messages, setMessages] = useState<DltMessage[]>([]);
  const [status, setStatus] = useState("");

  useEffect(() => {
    fetchDltTopics().then(setTopics).catch(console.error);
  }, []);

  const loadMessages = async (topic: string) => {
    setSelectedTopic(topic);
    setStatus("Loading...");
    try {
      const msgs = await fetchDltMessages(topic);
      setMessages(msgs);
      setStatus("");
    } catch {
      setStatus("Error loading messages");
    }
  };

  const handleReplay = async (msg: DltMessage) => {
    setStatus(`Replaying offset ${msg.offset}...`);
    try {
      await replayDltMessage(selectedTopic, msg.partition, msg.offset);
      setStatus("Replay sent!");
      setTimeout(() => loadMessages(selectedTopic), 1500);
    } catch {
      setStatus("Replay failed");
    }
  };

  return (
    <div className="grid-2">
      <div className="card">
        <div className="card-title">DLT Topics</div>
        {topics.length === 0 ? (
          <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
            No DLT topics found. DLTs appear when a consumer repeatedly fails to
            process a message.
          </p>
        ) : (
          <table className="table">
            <thead>
              <tr>
                <th>Topic</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {topics.map((t) => (
                <tr key={t}>
                  <td style={{ fontFamily: "var(--mono)", fontSize: 12 }}>{t}</td>
                  <td>
                    <button
                      className="btn btn-sm btn-secondary"
                      onClick={() => loadMessages(t)}
                    >
                      Browse
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div className="card">
        <div className="card-title">
          {selectedTopic
            ? `Messages in ${selectedTopic}`
            : "Select a DLT topic"}
        </div>
        {messages.length > 0 ? (
          <table className="table">
            <thead>
              <tr>
                <th>Partition</th>
                <th>Offset</th>
                <th>Key</th>
                <th>Preview</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              {messages.map((m, i) => (
                <tr key={i}>
                  <td>{m.partition}</td>
                  <td style={{ fontFamily: "var(--mono)", fontSize: 11 }}>
                    {m.offset}
                  </td>
                  <td style={{ fontFamily: "var(--mono)", fontSize: 11 }}>
                    {m.key?.substring(0, 16)}
                  </td>
                  <td style={{ fontFamily: "var(--mono)", fontSize: 11 }}>
                    {m.value?.substring(0, 40)}...
                  </td>
                  <td>
                    <button
                      className="btn btn-sm btn-secondary"
                      onClick={() => handleReplay(m)}
                    >
                      Replay
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : selectedTopic ? (
          <p style={{ color: "var(--text-muted)", fontSize: 13 }}>
            No messages in this topic.
          </p>
        ) : null}
        {status && (
          <p style={{ marginTop: 8, fontSize: 12, color: "var(--text-muted)" }}>
            {status}
          </p>
        )}
      </div>
    </div>
  );
}
