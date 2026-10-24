"use client";

import type { ReactNode } from "react";
import { ApiError } from "@/lib/api";
import { statusLabel } from "@/lib/format";

export function Button({ children, variant = "primary", loading, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger"; loading?: boolean;
}) {
  return <button className={"button button-" + variant} disabled={loading || props.disabled} {...props}>{loading ? "Aguarde..." : children}</button>;
}
export function Field({ label, error, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string }) {
  const id = props.id ?? props.name;
  return <label className="field" htmlFor={id}><span>{label}</span><input {...props} id={id} aria-invalid={Boolean(error)} aria-describedby={error ? id + "-error" : undefined} />
    {error && <small id={id + "-error"} className="field-error">{error}</small>}</label>;
}
export function Select({ label, children, ...props }: React.SelectHTMLAttributes<HTMLSelectElement> & { label: string }) {
  return <label className="field" htmlFor={props.id ?? props.name}><span>{label}</span><select {...props} id={props.id ?? props.name}>{children}</select></label>;
}
export function Notice({ children, tone = "info" }: { children: ReactNode; tone?: "info" | "success" | "error" | "warning" }) {
  return <div className={"notice notice-" + tone} role={tone === "error" ? "alert" : "status"}>{children}</div>;
}
export function Panel({ children, className = "", ...props }: React.HTMLAttributes<HTMLElement> & { children: ReactNode; className?: string }) { return <section className={"panel " + className} {...props}>{children}</section>; }
export function PageTitle({ eyebrow, title, description }: { eyebrow?: string; title: string; description?: string }) {
  return <div className="page-title">{eyebrow && <p className="eyebrow">{eyebrow}</p>}<h1>{title}</h1>{description && <p className="muted">{description}</p>}</div>;
}
export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return <div className="empty"><div className="empty-mark">✦</div><h2>{title}</h2>{children && <p className="muted">{children}</p>}</div>;
}
export function ErrorNotice({ error }: { error: unknown }) {
  const message = error instanceof ApiError ? error.problem.fieldErrors?.map((item) => item.field + ": " + item.message).join(" · ") || error.message : "Não foi possível carregar os dados. Tente novamente.";
  return <Notice tone="error">{message}</Notice>;
}
export function StatusBadge({ value }: { value?: string }) { return <span className={"badge badge-" + (value?.toLowerCase() ?? "default")}>{statusLabel(value)}</span>; }
export function Spinner() { return <span className="spinner" aria-label="Carregando" />; }
