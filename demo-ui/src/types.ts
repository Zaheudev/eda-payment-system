export interface EventEnvelope {
  topic: string;
  key: string;
  timestamp: number;
  payload: Record<string, unknown>;
  paymentId: string | null;
  partition: number;
  offset: number;
}

export interface SagaState {
  paymentId: string;
  currentState: string;
  history: string[];
  startedAt: string;
  lastUpdatedAt: string;
}

export interface ContainerInfo {
  id: string;
  names: string;
  state: string;
  status: string;
}

export interface PaymentResponse {
  paymentId: string;
  paymentStatus: string;
  rrn: string | null;
  authCode: string | null;
  processorTransactionId: string | null;
  captureId: string | null;
  amount: { amount: number; currency: string } | null;
  createdAt: string;
  message: string | null;
}

export type Tab = "payments" | "graph" | "tape" | "saga" | "dlt" | "chaos";
