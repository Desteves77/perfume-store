"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiClient } from "@/lib/api";
import { money } from "@/lib/format";
import type { Cart } from "@/lib/types";
import { Button, Empty, ErrorNotice, Notice, PageTitle, Panel, Spinner } from "@/components/ui";
import { ProductImage as Image } from "@/components/product-image";

export default function CartPage() {
  const [cart, setCart] = useState<Cart>(); const [error, setError] = useState<unknown>(); const [loading, setLoading] = useState(true); const [busy, setBusy] = useState("");
  const load = () => apiClient.get<Cart>("/api/v1/cart").then(setCart).catch(setError).finally(() => setLoading(false));
  useEffect(() => { load(); }, []);
  if (loading) return <div className="loading-state"><Spinner /></div>;
  if (error) return <div className="page container"><ErrorNotice error={error} /></div>;
  if (!cart || cart.items.length === 0) return <div className="page container"><PageTitle eyebrow="Carrinho" title="Seu carrinho está vazio" /><Empty title="Ainda não há itens por aqui">Explore o catálogo e adicione uma apresentação.</Empty><Link className="button button-primary" href="/catalog">Ir para o catálogo</Link></div>;
  const update = async (presentationId: string, quantity: number, version: number) => { setBusy(presentationId); try { await apiClient.patch<Cart>("/api/v1/cart/items/" + presentationId, { quantity, version }).then(setCart); } catch (reason) { setError(reason); } finally { setBusy(""); } };
  const remove = async (presentationId: string) => { setBusy(presentationId); try { await apiClient.delete<void>("/api/v1/cart/items/" + presentationId); await load(); } catch (reason) { setError(reason); } finally { setBusy(""); } };
  return <div className="page container"><PageTitle eyebrow="Carrinho" title="Revise suas escolhas" description="Adicionar ao carrinho não reserva estoque. A disponibilidade será revalidada no checkout." />{cart.issues.map((issue) => <Notice key={issue.code} tone="warning">{issue.message}</Notice>)}{error !== undefined && <ErrorNotice error={error} />}<div className="two-col"><Panel><div className="stack">{cart.items.map((item) => <div className="cart-row" key={item.id}><Image alt={item.productName} /><div><Link href={"/catalog/" + item.productSlug}><h3>{item.productName}</h3></Link><p className="muted">{item.volume} {item.unit} · {item.sku}</p><p>{money(item.unitPrice)} cada</p>{item.issues.map((issue) => <small className="field-error" key={issue.code}>{issue.message}</small>)}<div className="form-actions"><label className="field"><span>Quantidade</span><input type="number" min="1" max="20" value={item.quantity} disabled={busy === item.presentationId} onChange={(event) => update(item.presentationId, Number(event.target.value), item.version)} /></label><Button variant="ghost" loading={busy === item.presentationId} onClick={() => remove(item.presentationId)}>Remover</Button></div></div><strong>{money(item.lineTotal)}</strong></div>)}</div></Panel><Panel><h2>Resumo</h2><div className="summary-line"><span>Subtotal</span><strong>{money(cart.subtotal)}</strong></div><p className="muted">Frete e total são calculados no checkout.</p><Link className={"button button-primary " + (!cart.canCheckout ? "disabled-link" : "")} href={cart.canCheckout ? "/checkout" : "#"}>Continuar para checkout</Link></Panel></div></div>;
}
