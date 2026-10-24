"use client";

import { useEffect, useState } from "react";
import { AdminShell } from "@/components/admin-shell";
import { apiClient, operationKey } from "@/lib/api";
import type { Page } from "@/lib/types";
import { Button, ErrorNotice, Field, Notice, Panel } from "@/components/ui";

type Movement = { id: string; presentationId: string; type: string; physicalDelta: number; reason: string; createdAt: string };
export default function AdminInventoryPage() {
  const [presentationId, setPresentationId] = useState(""); const [quantity, setQuantity] = useState("1"); const [reason, setReason] = useState(""); const [type, setType] = useState("ENTRY"); const [movements, setMovements] = useState<Page<Movement>>(); const [message, setMessage] = useState(""); const [error, setError] = useState<unknown>();
  const load = () => apiClient.get<Page<Movement>>("/api/v1/admin/inventory/movements?page=0&size=50&sort=createdAt,desc").then(setMovements).catch(setError);
  useEffect(() => { load(); }, []);
  const adjust = async (event: React.FormEvent) => { event.preventDefault(); try { await apiClient.post("/api/v1/admin/inventory/adjustments", { presentationId, quantity: Number(quantity), type, reason }, operationKey()); setMessage("Ajuste registrado."); await load(); } catch (reasonError) { setError(reasonError); } };
  return <AdminShell title="Estoque">{message && <Notice tone="success">{message}</Notice>}{error !== undefined && <ErrorNotice error={error} />}<Panel><h2>Registrar movimentação</h2><form className="split" onSubmit={adjust}><Field label="ID da apresentação" name="presentationId" required value={presentationId} onChange={(event) => setPresentationId(event.target.value)} /><label className="field"><span>Tipo</span><select value={type} onChange={(event) => setType(event.target.value)}><option value="ENTRY">Entrada</option><option value="ADJUSTMENT">Ajuste</option></select></label><Field label="Quantidade ou delta" name="quantity" type="number" required value={quantity} onChange={(event) => setQuantity(event.target.value)} /><Field label="Motivo" name="reason" required value={reason} onChange={(event) => setReason(event.target.value)} /><Button type="submit">Salvar ajuste</Button></form></Panel><Panel style={{ marginTop: 20 } as React.CSSProperties}><h2>Movimentações</h2><div className="table-wrap"><table className="data-table"><thead><tr><th>Data</th><th>Apresentação</th><th>Tipo</th><th>Variação</th><th>Motivo</th></tr></thead><tbody>{movements?.items.map((item) => <tr key={item.id}><td>{new Date(item.createdAt).toLocaleString("pt-BR")}</td><td>{item.presentationId}</td><td>{item.type}</td><td>{item.physicalDelta}</td><td>{item.reason}</td></tr>)}</tbody></table></div></Panel></AdminShell>;
}
