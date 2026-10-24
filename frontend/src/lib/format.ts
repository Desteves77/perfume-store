import type { Money } from "./types";

export const money = (value?: Money | string | number | null) => {
  const amount = typeof value === "object" && value !== null ? value.amount : value;
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(Number(amount ?? 0));
};

export const dateTime = (value?: string | null) =>
  value ? new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value)) : "—";

const labels: Record<string, string> = {
  PENDING_PAYMENT: "Pagamento pendente", CONFIRMED: "Confirmado", CANCELLED: "Cancelado", EXPIRED: "Expirado",
  PENDING: "Pendente", APPROVED: "Aprovado", REJECTED: "Recusado", PAYMENT_REVIEW: "Pagamento em revisão",
  NOT_STARTED: "Não iniciado", PREPARING: "Em separação", SHIPPED: "Enviado", DELIVERED: "Entregue",
  SENT: "Enviado", RETRY_PENDING: "Em nova tentativa", FAILED: "Falhou", PROCESSING: "Processando",
  PENDING_PROVIDER: "Pendente no provedor", REQUESTED: "Solicitada",
  AWAITING_INSPECTION: "Aguardando inspeção", INSPECTED: "Inspecionada", RESTOCKED: "Reposta",
};

export const statusLabel = (value?: string) => value ? labels[value] ?? value.toLowerCase().replaceAll("_", " ").replace(/^\w/, (char) => char.toUpperCase()) : "—";
