"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { apiClient, operationKey } from "@/lib/api";
import { dateTime, money, statusLabel } from "@/lib/format";
import type { Order, Payment } from "@/lib/types";
import { Button, ErrorNotice, Notice, PageTitle, Panel, Spinner, StatusBadge } from "@/components/ui";

export default function OrderDetailPage() {
  const { publicNumber } = useParams<{ publicNumber: string }>(); const [order, setOrder] = useState<Order>(); const [payment, setPayment] = useState<Payment>(); const [error, setError] = useState<unknown>(); const [busy, setBusy] = useState(false);
  useEffect(() => { apiClient.get<Order>("/api/v1/me/orders/" + publicNumber).then(setOrder).catch(setError); apiClient.get<Payment>("/api/v1/me/orders/" + publicNumber + "/payment").then(setPayment).catch(() => undefined); }, [publicNumber]);
  if (error) return <div className="page container"><ErrorNotice error={error} /></div>;
  if (!order) return <div className="loading-state"><Spinner /></div>;
  const pay = async () => { setBusy(true); try { const result = await apiClient.post<Payment>("/api/v1/me/orders/" + publicNumber + "/payment-sessions", undefined, operationKey()); setPayment(result); if (result.checkoutUrl.startsWith("http")) window.location.href = result.checkoutUrl; } catch (reason) { setError(reason); } finally { setBusy(false); } };
  return <div className="page container"><Link className="muted" href="/account/orders">← Meus pedidos</Link><PageTitle eyebrow="Pedido" title={order.publicNumber} description={dateTime(order.createdAt)} />{error !== undefined && <ErrorNotice error={error} />}<div className="two-col"><div className="stack"><Panel><div className="form-actions"><StatusBadge value={order.status} /><StatusBadge value={order.paymentStatus} /><StatusBadge value={order.deliveryStatus} /></div><h2>Itens</h2>{order.items.map((item) => <div className="summary-line" key={item.orderItemId}><span>{item.quantity} × {item.productName} · {item.volume} {item.unit}</span><strong>{money(item.lineTotal)}</strong></div>)}</Panel><Panel><h2>Entrega</h2><p>{order.address.street}, {order.address.number}<br />{order.address.neighborhood}, {order.address.city} - {order.address.state}<br />CEP {order.address.postalCode}</p><p className="muted">{order.shippingSnapshot.method} · estimativa de {order.shippingSnapshot.minDays}–{order.shippingSnapshot.maxDays} dias</p></Panel><Panel><h2>Histórico</h2><div className="stack">{order.history.map((item, index) => <div key={index}><strong>{statusLabel(item.toStatus)}</strong><p className="muted">{item.event} · {dateTime(item.createdAt)}</p></div>)}</div></Panel></div><Panel><h2>Resumo financeiro</h2><div className="summary-line"><span>Subtotal</span><strong>{money(order.subtotal)}</strong></div><div className="summary-line"><span>Frete</span><strong>{money(order.shipping)}</strong></div><div className="total-line"><span>Total</span><strong>{money(order.total)}</strong></div>{payment && <><hr /><p>Pagamento: <StatusBadge value={payment.status} /></p><p className="muted">{payment.provider === "MOCK" ? "Pagamento simulado disponível apenas no ambiente de homologação." : "Checkout hospedado pelo provedor."}</p></>}{order.paymentStatus === "PENDING" && <Button loading={busy} onClick={pay}>Continuar pagamento</Button>}{order.paymentStatus === "PAYMENT_REVIEW" && <Notice tone="warning">O pagamento está em revisão. O envio não é considerado confirmado.</Notice>}</Panel></div></div>;
}
