"use client";

import Link from "next/link";
import { useSession } from "@/lib/session";
import { PageTitle, Spinner } from "./ui";

export function AdminShell({ title, children }: { title: string; children: React.ReactNode }) {
  const { user, loading } = useSession();
  if (loading) return <div className="loading-state"><Spinner /></div>;
  if (!user || user.role !== "ADMIN") return <div className="page container"><PageTitle title="Acesso restrito" description="Esta área está disponível somente para administradores autenticados." /><Link href="/login?next=/admin" className="button button-primary">Entrar</Link></div>;
  return <div className="page container"><div className="admin-layout"><aside className="admin-side"><h2>Operação</h2><nav><Link href="/admin">Pedidos</Link><Link href="/admin/catalog">Catálogo</Link><Link href="/admin/inventory">Estoque</Link><Link href="/admin/notifications">Notificações</Link></nav></aside><section><PageTitle eyebrow="Painel administrativo" title={title} />{children}</section></div></div>;
}
