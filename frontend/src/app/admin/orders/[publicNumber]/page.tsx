"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { AdminShell } from "@/components/admin-shell";
import { apiClient, operationKey } from "@/lib/api";
import { dateTime, money } from "@/lib/format";
import type { AdminOrder, ReturnRecord } from "@/lib/types";
import { Button, ErrorNotice, Field, Panel, Spinner, StatusBadge } from "@/components/ui";

export default function AdminOrderDetailPage() {
  const { publicNumber } = useParams<{ publicNumber: string }>();
  const [detail, setDetail] = useState<AdminOrder>();
  const [error, setError] = useState<unknown>();
  const [reason, setReason] = useState("");
  const [carrier, setCarrier] = useState("");
  const [tracking, setTracking] = useState("");
  const [refundReason, setRefundReason] = useState("");
  const [busy, setBusy] = useState("");

  const load = () => apiClient.get<AdminOrder>("/api/v1/admin/orders/" + publicNumber).then(setDetail).catch(setError);
  useEffect(() => { load(); }, [publicNumber]); // eslint-disable-line react-hooks/exhaustive-deps

  const action = async (name: string, body?: unknown, path = name) => {
    setBusy(name);
    try { await apiClient.post("/api/v1/admin/orders/" + publicNumber + "/" + path, body, operationKey()); await load(); }
    catch (reasonError) { setError(reasonError); }
    finally { setBusy(""); }
  };

  const returnAction = async (item: ReturnRecord, operation: "receive" | "inspect" | "restock") => {
    setBusy(operation + item.id);
    try {
      const body = operation === "receive"
        ? { quantity: item.quantity, conditionNote: "Recebimento registrado na homologação." }
        : operation === "inspect"
          ? { approvedQuantity: item.receivedQuantity, rejectedQuantity: 0, reason: "Inspeção aprovada na homologação." }
          : { quantity: item.approvedQuantity };
      await apiClient.post("/api/v1/admin/returns/" + item.id + "/" + operation, body, operation === "restock" ? operationKey() : undefined);
      await load();
    } catch (reasonError) { setError(reasonError); }
    finally { setBusy(""); }
  };

  if (!detail) return <div className="loading-state">{error ? <ErrorNotice error={error} /> : <Spinner />}</div>;
  const order = detail.order;
  return <AdminShell title={publicNumber}>
    <Link href="/admin" className="muted">← Pedidos</Link>
    {error !== undefined && <ErrorNotice error={error} />}
    <div className="two-col" style={{ marginTop: 20 }}>
      <div className="stack">
        <Panel><div className="form-actions"><StatusBadge value={order.status} /><StatusBadge value={order.paymentStatus} /><StatusBadge value={order.deliveryStatus} /></div>
          <h2>Cliente</h2><p><strong>{detail.customer.displayName}</strong><br />{detail.customer.email}</p>
          <h2>Itens</h2>{order.items.map((item) => <div className="summary-line" data-testid="order-item" key={item.orderItemId}><span>{item.quantity} × {item.productName}</span><strong>{money(item.lineTotal)}</strong></div>)}
        </Panel>
        <Panel><h2>Histórico administrativo</h2>{detail.administrationHistory.map((item, index) => <p className="muted" key={index}>{item.operation} · {item.reason ?? "sem motivo"} · {dateTime(item.createdAt)}</p>)}</Panel>
      </div>
      <div className="stack">
        <Panel><h2>Operação</h2><div className="form-actions">
          <Button loading={busy === "prepare"} disabled={order.status !== "CONFIRMED"} onClick={() => action("prepare", { reason: reason || undefined }, "prepare")}>Iniciar separação</Button>
          <Button loading={busy === "deliver"} disabled={order.deliveryStatus !== "SHIPPED"} onClick={() => action("deliver", { reason: reason || undefined }, "deliver")}>Confirmar entrega</Button>
        </div><div className="stack" style={{ marginTop: 16 }}>
          <Field label="Motivo" name="reason" value={reason} onChange={(event) => setReason(event.target.value)} />
          <Field label="Transportadora" name="carrier" value={carrier} onChange={(event) => setCarrier(event.target.value)} />
          <Field label="Rastreamento" name="tracking" value={tracking} onChange={(event) => setTracking(event.target.value)} />
          <Button loading={busy === "ship"} disabled={order.deliveryStatus !== "PREPARING"} onClick={() => action("ship", { carrier, trackingCode: tracking || undefined, reason: reason || undefined }, "ship")}>Registrar envio</Button>
          <Button variant="danger" loading={busy === "cancel"} disabled={order.status === "CANCELLED"} onClick={() => action("cancel", { reason: reason || "Cancelamento administrativo" }, "cancel")}>Cancelar pedido</Button>
        </div></Panel>
        <Panel><h2>Reembolso simulado</h2><p className="muted">A solicitação é registrada; a confirmação depende do provedor permitido no ambiente.</p>
          <Field label="Motivo" name="refundReason" value={refundReason} onChange={(event) => setRefundReason(event.target.value)} />
          <Button loading={busy === "refund"} onClick={async () => { setBusy("refund"); try { await apiClient.post("/api/v1/admin/orders/" + publicNumber + "/refunds", { reason: refundReason }, operationKey()); await load(); } catch (reasonError) { setError(reasonError); } finally { setBusy(""); } }}>Solicitar reembolso</Button>
          {detail.refunds.map((refund) => <div className="form-actions" key={refund.id}><span><StatusBadge value={refund.status} /> {money(refund.amount)} · {refund.reason}</span>{refund.status === "PENDING_PROVIDER" && <Button variant="ghost" loading={busy === "refund-" + refund.id} onClick={async () => { setBusy("refund-" + refund.id); try { await apiClient.post("/api/v1/dev/payment/refunds/" + refund.id + "/simulate", { outcome: "APPROVED" }, operationKey()); await load(); } catch (reasonError) { setError(reasonError); } finally { setBusy(""); } }}>Simular aprovação</Button>}</div>)}
        </Panel>
        <Panel><h2>Devoluções</h2>{order.items.map((item) => <div className="form-actions" key={item.orderItemId}><span>{item.productName}</span><Button variant="ghost" onClick={() => action("return-" + item.orderItemId, { orderItemId: item.orderItemId, quantity: item.quantity, reason: "Solicitação registrada pelo painel" }, "returns")}>Registrar devolução</Button></div>)}
          {detail.returns.map((item) => <div className="stack" key={item.id}><p><StatusBadge value={item.status} /> {item.quantity} unidade(s)</p><div className="form-actions">
            {item.status === "REQUESTED" && <Button variant="ghost" loading={busy === "receive" + item.id} onClick={() => returnAction(item, "receive")}>Registrar recebimento</Button>}
            {item.status === "AWAITING_INSPECTION" && <Button variant="ghost" loading={busy === "inspect" + item.id} onClick={() => returnAction(item, "inspect")}>Registrar inspeção</Button>}
            {item.status === "INSPECTED" && item.approvedQuantity > 0 && <Button variant="ghost" loading={busy === "restock" + item.id} onClick={() => returnAction(item, "restock")}>Repor estoque</Button>}
          </div></div>)}
        </Panel>
      </div>
    </div>
  </AdminShell>;
}
