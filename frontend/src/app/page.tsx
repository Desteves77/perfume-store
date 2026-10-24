"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { apiClient } from "@/lib/api";
import { money } from "@/lib/format";
import type { Page, Product } from "@/lib/types";
import { Empty, ErrorNotice, PageTitle, Panel, Spinner } from "@/components/ui";
import { ProductImage } from "@/components/product-image";

export default function HomePage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState<unknown>();
  const [loading, setLoading] = useState(true);
  useEffect(() => { apiClient.get<Page<Product>>("/api/v1/catalog/products?page=0&size=8&sort=createdAt,desc").then((data) => setProducts(data.items)).catch(setError).finally(() => setLoading(false)); }, []);
  return <div>
    <section className="hero"><div><p className="eyebrow">PerfumeStore</p><h1>Uma presença que fica.</h1><p>Descubra fragrâncias com personalidade e escolha a apresentação que combina com o seu momento.</p><div className="hero-actions"><Link className="button button-primary" href="/catalog">Explorar catálogo</Link><Link className="button button-ghost" href="/account">Minha conta</Link></div></div></section>
    <section className="section container"><div className="section-head"><PageTitle eyebrow="Seleção da loja" title="Encontre seu próximo aroma" description="Produtos e disponibilidade são carregados do catálogo real." /><Link href="/catalog" className="button button-secondary">Ver catálogo</Link></div>
      {loading && <div className="loading-state"><Spinner /></div>}{error !== undefined && <ErrorNotice error={error} />}{!loading && !error && products.length === 0 && <Empty title="Catálogo em preparação" />}
      <div className="grid">{products.map((product) => <Link className="product-card" href={"/catalog/" + product.slug} key={product.id}><ProductImage src={product.images.find((image) => image.primary)?.url ?? product.images[0]?.url} alt={product.images[0]?.altText ?? product.name} /><div className="product-card-body"><span className="brand-label">{product.brand}</span><h3>{product.name}</h3><p className="muted">{product.presentations[0]?.volume} {product.presentations[0]?.unit}</p><div className="price">{money(product.presentations[0]?.price)}</div></div></Link>)}</div>
    </section>
    <section className="section container split"><Panel><p className="eyebrow">Escolha com clareza</p><h2>Valores e disponibilidade sempre atualizados</h2><p className="muted">O backend confirma cada preço, quantidade, frete e pedido. A interface acompanha essas respostas sem prometer o que ainda não foi confirmado.</p></Panel><Panel><p className="eyebrow">Precisa de ajuda?</p><h2>Fale com a nossa equipe</h2><p className="muted">Consulte informações de entrega, trocas e atendimento.</p><Link className="button button-secondary" href="/contact">Ver atendimento</Link></Panel></section>
  </div>;
}
