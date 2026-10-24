"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiClient } from "@/lib/api";
import { dateTime, money, statusLabel } from "@/lib/format";
import type { OrderSummary, Page } from "@/lib/types";
import { Empty, ErrorNotice, PageTitle, Panel, Spinner, StatusBadge } from "@/components/ui";

export default function OrdersPage() {
  const [data, setData] = useState<Page<OrderSummary>>(); const [error, setError] = useState<unknown>();
  useEffect(() => { apiClient.get<Page<OrderSummary>>("/api/v1/me/orders?page=0&size=20&sort=createdAt,desc").then(setData).catch(setError); }, []);
  if (error) return <div className="page container"><ErrorNotice error={error} /></div>;
  if (!data) return <div className="loading-state"><Spinner /></div>;
  return <div className="page container"><PageTitle eyebrow="Minha conta" title="Meus pedidos" description="Histórico real dos seus pedidos, pagamentos e entregas." />{data.items.length === 0 ? <Empty title="Você ainda não fez pedidos">Quando criar um pedido, ele aparecerá aqui.</Empty> : <Panel><div className="table-wrap"><table className="data-table"><thead><tr><th>Pedido</th><th>Data</th><th>Status</th><th>Pagamento</th><th>Entrega</th><th>Total</th></tr></thead><tbody>{data.items.map((order) => <tr key={order.publicNumber}><td><Link href={"/account/orders/" + order.publicNumber}><strong>{order.publicNumber}</strong></Link><br /><small>{order.itemCount} item(ns)</small></td><td>{dateTime(order.createdAt)}</td><td><StatusBadge value={order.status} /></td><td>{statusLabel(order.paymentStatus)}</td><td>{statusLabel(order.deliveryStatus)}</td><td>{money(order.total)}</td></tr>)}</tbody></table></div></Panel>}</div>;
}
