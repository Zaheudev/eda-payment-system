import { useCallback, useMemo } from "react";
import ReactFlow, {
  Background,
  Controls,
  Node,
  Edge,
  MarkerType,
} from "reactflow";
import "reactflow/dist/style.css";
import { EventEnvelope } from "../types";

const serviceNodes: Node[] = [
  {
    id: "gateway",
    position: { x: 50, y: 240 },
    data: { label: "Payment\nGateway" },
    style: {
      background: "#dbeafe",
      border: "1px solid #2563eb",
      borderRadius: 8,
      padding: "12px 20px",
      fontSize: 13,
      fontWeight: 600,
      textAlign: "center",
      lineHeight: 1.3,
    },
  },
  {
    id: "token",
    position: { x: 50, y: 400 },
    data: { label: "Card Token\nManager" },
    style: {
      background: "#fef9c3",
      border: "1px solid #d97706",
      borderRadius: 8,
      padding: "12px 20px",
      fontSize: 13,
      fontWeight: 600,
      textAlign: "center",
      lineHeight: 1.3,
    },
  },
  {
    id: "risk",
    position: { x: 320, y: 180 },
    data: { label: "Risk &\nFraud" },
    style: {
      background: "#fef2f2",
      border: "1px solid #dc2626",
      borderRadius: 8,
      padding: "12px 20px",
      fontSize: 13,
      fontWeight: 600,
      textAlign: "center",
      lineHeight: 1.3,
    },
  },
  {
    id: "routing",
    position: { x: 320, y: 320 },
    data: { label: "Payment\nRouting" },
    style: {
      background: "#dcfce7",
      border: "1px solid #16a34a",
      borderRadius: 8,
      padding: "12px 20px",
      fontSize: 13,
      fontWeight: 600,
      textAlign: "center",
      lineHeight: 1.3,
    },
  },
  {
    id: "emulator",
    position: { x: 600, y: 240 },
    data: { label: "Card Network\nEmulator" },
    style: {
      background: "#f3e8ff",
      border: "1px solid #9333ea",
      borderRadius: 8,
      padding: "12px 20px",
      fontSize: 13,
      fontWeight: 600,
      textAlign: "center",
      lineHeight: 1.3,
    },
  },
  {
    id: "kafka",
    position: { x: 380, y: 480 },
    data: { label: "Apache\nKafka" },
    style: {
      background: "#f0f0f0",
      border: "1px solid #666",
      borderRadius: 8,
      padding: "12px 20px",
      fontSize: 13,
      fontWeight: 600,
      textAlign: "center",
      lineHeight: 1.3,
    },
  },
];

const baseEdges: Edge[] = [
  { id: "e-gw-rf", source: "gateway", target: "risk", animated: false, style: { stroke: "#999" }, markerEnd: { type: MarkerType.ArrowClosed } },
  { id: "e-gw-tm", source: "gateway", target: "token", animated: false, style: { stroke: "#999" }, markerEnd: { type: MarkerType.ArrowClosed } },
  { id: "e-rf-pr", source: "risk", target: "routing", animated: false, style: { stroke: "#999" }, markerEnd: { type: MarkerType.ArrowClosed } },
  { id: "e-rf-gw", source: "risk", target: "gateway", animated: false, style: { stroke: "#dc2626" }, markerEnd: { type: MarkerType.ArrowClosed } },
  { id: "e-pr-em", source: "routing", target: "emulator", animated: false, style: { stroke: "#999" }, markerEnd: { type: MarkerType.ArrowClosed } },
  { id: "e-em-gw", source: "emulator", target: "gateway", animated: false, style: { stroke: "#999" }, markerEnd: { type: MarkerType.ArrowClosed } },
  { id: "e-gw-pr", source: "gateway", target: "routing", animated: false, style: { stroke: "#999" }, markerEnd: { type: MarkerType.ArrowClosed } },
];

const topicEdgeMap: Record<string, string[]> = {
  "payment-requests": ["e-gw-rf"],
  "risk-assessed": ["e-rf-pr", "e-rf-gw"],
  "payment-rejected": ["e-rf-gw"],
  "routing-completed": ["e-gw-pr"],
  "authorization-completed": ["e-em-gw"],
  "capture-completed": ["e-em-gw"],
  "refund-completed": ["e-em-gw"],
  "void-completed": ["e-em-gw"],
  "capture-requests": ["e-pr-em"],
  "refund-requests": ["e-pr-em"],
  "void-requests": ["e-pr-em"],
};

export default function ServiceGraph({ events }: { events: EventEnvelope[] }) {
  const recentEvents = events.slice(-20);

  const edges = useMemo(() => {
    const active = new Set<string>();
    for (const e of recentEvents) {
      const ids = topicEdgeMap[e.topic];
      if (ids) ids.forEach((id) => active.add(id));
    }
    return baseEdges.map((edge) => ({
      ...edge,
      animated: active.has(edge.id),
      style: {
        ...edge.style,
        stroke: active.has(edge.id) ? "#2563eb" : (edge.style as { stroke?: string }).stroke || "#999",
        strokeWidth: active.has(edge.id) ? 2 : 1,
      },
    }));
  }, [recentEvents]);

  const nodes = useMemo(() => {
    const topicToNode: Record<string, string> = {
      "payment-requests": "gateway",
      "risk-assessed": "risk",
      "payment-rejected": "risk",
      "routing-completed": "routing",
      "authorization-completed": "emulator",
      "capture-requests": "gateway",
      "capture-completed": "emulator",
      "refund-requests": "gateway",
      "refund-completed": "emulator",
      "void-requests": "gateway",
      "void-completed": "emulator",
    };

    const activeCounts: Record<string, number> = {};
    for (const e of recentEvents) {
      const nodeId = topicToNode[e.topic];
      if (nodeId) activeCounts[nodeId] = (activeCounts[nodeId] || 0) + 1;
    }

    return serviceNodes.map((n) => {
      const count = activeCounts[n.id] || 0;
      return {
        ...n,
        style: {
          ...n.style,
          boxShadow: count > 0 ? `0 0 8px rgba(37,99,235,0.3)` : "none",
        },
      };
    });
  }, [recentEvents]);

  return (
    <div className="card">
      <div className="card-title">Service Graph (live)</div>
      <div className="graph-container">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          fitView
          attributionPosition="bottom-left"
          nodesDraggable={false}
          nodesConnectable={false}
          elementsSelectable={false}
          proOptions={{ hideAttribution: true }}
        >
          <Background gap={16} color="#e5e5e5" />
          <Controls showInteractive={false} />
        </ReactFlow>
      </div>
    </div>
  );
}
