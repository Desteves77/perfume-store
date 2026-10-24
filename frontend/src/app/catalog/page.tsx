"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiClient } from "@/lib/api";
import { money } from "@/lib/format";
import type { Category, Page, Product } from "@/lib/types";
import { Button, Empty, ErrorNotice, Field, PageTitle, Spinner } from "@/components/ui";
import { ProductImage } from "@/components/product-image";

export default function CatalogPage() {
  const [data, setData] = useState<Page<Product>>({ items: [], page: 0, size: 12, totalElements: 0, totalPages: 0 });
  const [categories, setCategories] = useState<Category[]>([]);
  const [q, setQ] = useState("");
  const [category, setCategory] = useState("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>();
  useEffect(() => { apiClient.get<Page<Category>>("/api/v1/catalog/categories?page=0&size=100&sort=name,asc").then((result) => setCategories(result.items)).catch(() => undefined); }, []);
  useEffect(() => { setLoading(true); setError(undefined); const params = new URLSearchParams({ page: String(page), size: "12", sort: "name,asc" }); if (q) params.set("q", q); if (category) params.set("category", category); apiClient.get<Page<Product>>("/api/v1/catalog/products?" + params).then(setData).catch(setError).finally(() => setLoading(false)); }, [page, q, category]);
  return <div className="page container"><PageTitle eyebrow="Catálogo" title="Encontre sua assinatura" description="Busque por nome, marca ou filtre por categoria." />
    <div className="toolbar"><Field label="Buscar" name="q" value={q} onChange={(event) => { setPage(0); setQ(event.target.value); }} placeholder="Ex.: floral, amadeirado..." /><label className="field"><span>Categoria</span><select value={category} onChange={(event) => { setPage(0); setCategory(event.target.value); }}><option value="">Todas</option>{categories.map((item) => <option key={item.id} value={item.slug}>{item.name}</option>)}</select></label><Button variant="ghost" onClick={() => { setQ(""); setCategory(""); setPage(0); }}>Limpar</Button></div>
    {loading && <div className="loading-state"><Spinner /></div>}{error !== undefined && <ErrorNotice error={error} />}{!loading && !error && data.items.length === 0 && <Empty title="Nenhum produto encontrado">Tente remover um filtro ou buscar por outro termo.</Empty>}
    <div className="grid">{data.items.map((product) => <Link className="product-card" href={"/catalog/" + product.slug} key={product.id}><ProductImage src={product.images.find((image) => image.primary)?.url ?? product.images[0]?.url} alt={product.images[0]?.altText ?? product.name} /><div className="product-card-body"><span className="brand-label">{product.brand}</span><h3>{product.name}</h3><p className="muted">{product.presentations.length} apresentação(ões)</p><div className="price">{money(product.presentations[0]?.price)}</div></div></Link>)}</div>
    {data.totalPages > 1 && <div className="pagination"><span className="muted">Página {data.page + 1} de {data.totalPages}</span><div className="form-actions"><Button variant="ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>Anterior</Button><Button variant="ghost" disabled={page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Próxima</Button></div></div>}
  </div>;
}
