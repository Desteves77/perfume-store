"use client";

import { useEffect, useState } from "react";
import { AdminShell } from "@/components/admin-shell";
import { apiClient, operationKey, upload } from "@/lib/api";
import type { Category, Page, Product } from "@/lib/types";
import { Button, ErrorNotice, Field, Notice, Panel, Spinner, StatusBadge } from "@/components/ui";
import { ProductImage } from "@/components/product-image";

export default function AdminCatalogPage() {
  const [products, setProducts] = useState<Page<Product>>();
  const [categories, setCategories] = useState<Category[]>([]);
  const [error, setError] = useState<unknown>();
  const [message, setMessage] = useState("");
  const [product, setProduct] = useState({ name: "", brand: "", description: "", slug: "", categoryId: "" });
  const [category, setCategory] = useState({ name: "", slug: "" });

  const load = () => apiClient.get<Page<Product>>("/api/v1/admin/products?page=0&size=50&sort=name,asc").then(setProducts).catch(setError);

  useEffect(() => {
    load();
    apiClient.get<Page<Category>>("/api/v1/admin/categories?page=0&size=100&sort=name,asc").then((data) => {
      setCategories(data.items);
      setProduct((current) => ({ ...current, categoryId: current.categoryId || data.items[0]?.id || "" }));
    }).catch(setError);
  }, []);

  const createCategory = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      const result = await apiClient.post<Category>("/api/v1/admin/categories", { ...category, active: true });
      setCategories([...categories, result]);
      setProduct({ ...product, categoryId: result.id });
      setCategory({ name: "", slug: "" });
      setMessage("Categoria criada.");
    } catch (reason) { setError(reason); }
  };

  const createProduct = async (event: React.FormEvent) => {
    event.preventDefault();
    try {
      await apiClient.post("/api/v1/admin/products", { ...product, active: false });
      setMessage("Produto criado. Adicione apresentação e imagem antes de publicar.");
      setProduct({ name: "", brand: "", description: "", slug: "", categoryId: categories[0]?.id ?? "" });
      await load();
    } catch (reason) { setError(reason); }
  };

  const addPresentation = async (productId: string) => {
    const sku = window.prompt("SKU");
    const volume = window.prompt("Volume");
    const price = window.prompt("Preço");
    if (!sku || !volume || !price) return;
    try {
      await apiClient.post("/api/v1/admin/products/" + productId + "/presentations", { sku, volume: Number(volume), price: Number(price), active: true }, operationKey());
      setMessage("Apresentação criada.");
      await load();
    } catch (reason) { setError(reason); }
  };

  const publish = async (item: Product) => {
    try {
      await apiClient.put("/api/v1/admin/products/" + item.id + "/status", { active: true, version: item.version });
      setMessage("Produto publicado.");
      await load();
    } catch (reason) { setError(reason); }
  };

  const uploadImage = async (productId: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    form.append("altText", "Imagem do produto");
    try {
      await upload("/api/v1/admin/products/" + productId + "/images", form);
      setMessage("Imagem enviada.");
      await load();
    } catch (reason) { setError(reason); }
  };

  return <AdminShell title="Catálogo">
    {message && <Notice tone="success">{message}</Notice>}
    {error !== undefined && <ErrorNotice error={error} />}
    <div className="split">
      <Panel><h2>Categoria</h2><form className="stack" onSubmit={createCategory}>
        <Field label="Nome" name="categoryName" required value={category.name} onChange={(event) => setCategory({ ...category, name: event.target.value })} />
        <Field label="Slug" name="categorySlug" required value={category.slug} onChange={(event) => setCategory({ ...category, slug: event.target.value })} />
        <Button type="submit">Criar categoria</Button>
      </form></Panel>
      <Panel><h2>Novo produto</h2><form className="stack" onSubmit={createProduct}>
        <Field label="Nome" name="productName" required value={product.name} onChange={(event) => setProduct({ ...product, name: event.target.value })} />
        <div className="split"><Field label="Marca" name="brand" required value={product.brand} onChange={(event) => setProduct({ ...product, brand: event.target.value })} /><Field label="Slug" name="slug" required value={product.slug} onChange={(event) => setProduct({ ...product, slug: event.target.value })} /></div>
        <Field label="Descrição" name="description" required value={product.description} onChange={(event) => setProduct({ ...product, description: event.target.value })} />
        <label className="field"><span>Categoria</span><select value={product.categoryId} onChange={(event) => setProduct({ ...product, categoryId: event.target.value })}>{categories.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
        <Button type="submit">Criar produto</Button>
      </form></Panel>
    </div>
    <Panel style={{ marginTop: 20 } as React.CSSProperties}><h2>Produtos</h2>{!products ? <div className="loading-state"><Spinner /></div> : <div className="grid">{products.items.map((item) => <div className="product-card" key={item.id} data-testid={"admin-product-" + item.slug}>
      <ProductImage src={item.images[0]?.url} alt={item.images[0]?.altText ?? item.name} />
      <div className="product-card-body"><h3>{item.name}</h3><p className="muted">{item.slug}</p><StatusBadge value={item.active ? "ACTIVE" : "INACTIVE"} /><p>{item.presentations.length} apresentação(ões)</p>
        {item.presentations.map((presentation) => <small className="muted" data-testid="presentation-id" key={presentation.id}>ID: {presentation.id}</small>)}
        <div className="form-actions"><Button variant="ghost" onClick={() => addPresentation(item.id)}>Adicionar apresentação</Button><label className="button button-secondary">Imagem<input hidden type="file" accept="image/png,image/jpeg" onChange={(event) => event.target.files?.[0] && uploadImage(item.id, event.target.files[0])} /></label>{!item.active && item.publishable && <Button onClick={() => publish(item)}>Publicar</Button>}</div>
      </div>
    </div>)}</div>}</Panel>
  </AdminShell>;
}
