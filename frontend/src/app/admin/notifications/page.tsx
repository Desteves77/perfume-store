"use client";
import { useEffect, useState } from "react";
import { AdminShell } from "@/components/admin-shell";
import { apiClient } from "@/lib/api";
import type { Page } from "@/lib/types";
import { ErrorNotice, Panel, Spinner, StatusBadge } from "@/components/ui";
type Notification = { id: string; eventType: string; status: string; orderId?: string; recipientEmail: string; attemptCount: number; createdAt: string; sentAt?: string };
export default function AdminNotificationsPage() {
  const [data, setData] = useState<Page<Notification>>(); const [error, setError] = useState<unknown>();
  useEffect(() => { apiClient.get<Page<Notification>>("/api/v1/admin/notifications?page=0&size=50&sort=createdAt,desc").then(setData).catch(setError); }, []);
  return <AdminShell title="Notificações">{error !== undefined && <ErrorNotice error={error} />}{!data && !error && <div className="loading-state"><Spinner /></div>}{data && <Panel><div className="table-wrap"><table className="data-table"><thead><tr><th>Evento</th><th>Destinatário</th><th>Estado</th><th>Tentativas</th><th>Criada</th></tr></thead><tbody>{data.items.map((item) => <tr key={item.id}><td>{item.eventType}</td><td>{item.recipientEmail}</td><td><StatusBadge value={item.status} /></td><td>{item.attemptCount}</td><td>{new Date(item.createdAt).toLocaleString("pt-BR")}</td></tr>)}</tbody></table></div></Panel>}</AdminShell>;
}
