"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { apiClient, operationKey } from "@/lib/api";
import { money } from "@/lib/format";
import type { Cart, Product, Presentation } from "@/lib/types";
import { Button, ErrorNotice, Notice, PageTitle, Panel, Spinner } from "@/components/ui";
import { ProductImage } from "@/components/product-image";

export default function ProductPage() {
  const { slug } = useParams<{ slug: string }>();
  const [product, setProduct] = useState<Product>();
  const [presentation, setPresentation] = useState<Presentation>();
  const [quantity, setQuantity] = useState(1);
  const [message, setMessage] = useState("");
  const [error, setError] = useState<unknown>();
  useEffect(() => { apiClient.get<Product>("/api/v1/catalog/products/" + slug).then((data) => { setProduct(data); setPresentation(data.presentations[0]); }).catch(setError); }, [slug]);
  if (error) return <div className="page container"><ErrorNotice error={error} /></div>;
  if (!product) return <div className="loading-state"><Spinner /></div>;
  const add = async () => { if (!presentation) return; setMessage(""); try { await apiClient.post<Cart>("/api/v1/cart/items", { presentationId: presentation.id, quantity }, operationKey()); setMessage("Apresentação adicionada ao carrinho."); } catch (reason) { setError(reason); } };
  return <div className="page container"><Link className="muted" href="/catalog">← Voltar ao catálogo</Link><div className="two-col" style={{ marginTop: 24 }}><div><ProductImage src={product.images.find((image) => image.primary)?.url ?? product.images[0]?.url} alt={product.images[0]?.altText ?? product.name} className="detail-image" /></div><div className="stack"><PageTitle eyebrow={product.brand} title={product.name} description={product.description} /><Panel><label className="field"><span>Apresentação</span><select value={presentation?.id ?? ""} onChange={(event) => setPresentation(product.presentations.find((item) => item.id === event.target.value))}>{product.presentations.map((item) => <option value={item.id} key={item.id}>{item.volume} {item.unit} · {money(item.price)} {item.availability?.availableForPurchase ? "" : "· indisponível"}</option>)}</select></label><p className="price">{money(presentation?.price)}</p><p className="muted">Disponibilidade: {presentation?.availability?.availableQuantity ?? 0} unidade(s)</p><div className="form-actions"><label className="field"><span>Quantidade</span><input type="number" min="1" max="20" value={quantity} onChange={(event) => setQuantity(Number(event.target.value))} /></label><Button disabled={!presentation?.availability?.availableForPurchase} onClick={add}>Adicionar ao carrinho</Button></div>{message && <Notice tone="success">{message}</Notice>}{error !== undefined && <ErrorNotice error={error} />}</Panel></div></div></div>;
}
