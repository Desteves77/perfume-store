"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AdminShell } from "@/components/admin-shell";
import { apiClient } from "@/lib/api";
import { dateTime, money } from "@/lib/format";
import type { Page } from "@/lib/types";
import { ErrorNotice, Field, Panel, Spinner, StatusBadge } from "@/components/ui";

type AdminSummary = { publicNumber: string; customerId: string; createdAt: string; status: string; paymentStatus: string; deliveryStatus: string; total: { amount: string; currency: string }; itemCount: number };
export default function AdminOrdersPage() {
  const [data, setData] = useState<Page<AdminSummary>>(); const [search, setSearch] = useState(""); const [status, setStatus] = useState(""); const [page, setPage] = useState(0); const [error, setError] = useState<unknown>();
  useEffect(() => { const params = new URLSearchParams({ page: String(page), size: "20", sort: "createdAt,desc" }); if (search) params.set("search", search); if (status) params.set("status", status); apiClient.get<Page<AdminSummary>>("/api/v1/admin/orders?" + params).then(setData).catch(setError); }, [page, search, status]);
  return <AdminShell title="Pedidos"><Panel><div className="toolbar"><Field label="Buscar pedido" name="search" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} placeholder="Número público" /><label className="field"><span>Situação</span><select value={status} onChange={(event) => { setPage(0); setStatus(event.target.value); }}><option value="">Todas</option><option value="PENDING_PAYMENT">Pagamento pendente</option><option value="CONFIRMED">Confirmado</option><option value="CANCELLED">Cancelado</option><option value="EXPIRED">Expirado</option></select></label></div>{error !== undefined && <ErrorNotice error={error} />}{!data && !error && <div className="loading-state"><Spinner /></div>}{data && <><div className="table-wrap"><table className="data-table"><thead><tr><th>Pedido</th><th>Data</th><th>Status</th><th>Pagamento</th><th>Entrega</th><th>Total</th></tr></thead><tbody>{data.items.map((item) => <tr key={item.publicNumber}><td><Link href={"/admin/orders/" + item.publicNumber}><strong>{item.publicNumber}</strong></Link><br /><small>{item.itemCount} item(ns)</small></td><td>{dateTime(item.createdAt)}</td><td><StatusBadge value={item.status} /></td><td><StatusBadge value={item.paymentStatus} /></td><td><StatusBadge value={item.deliveryStatus} /></td><td>{money(item.total)}</td></tr>)}</tbody></table></div><div className="pagination"><span className="muted">{data.totalElements} pedido(s)</span><div className="form-actions"><button className="button button-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</button><button className="button button-ghost" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Próxima</button></div></div></>}</Panel></AdminShell>;
}
