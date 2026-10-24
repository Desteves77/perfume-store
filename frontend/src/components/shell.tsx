"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { apiClient } from "@/lib/api";
import { useSession } from "@/lib/session";
import type { Cart } from "@/lib/types";

export function Shell({ children }: { children: React.ReactNode }) {
  const { user, loading, logout } = useSession();
  const [menu, setMenu] = useState(false);
  const [cart, setCart] = useState<Cart | null>(null);
  const pathname = usePathname();
  const router = useRouter();
  useEffect(() => { apiClient.get<Cart>("/api/v1/cart").then(setCart).catch(() => undefined); }, [pathname, user]);
  const signOut = async () => { await logout(); router.push("/"); };
  return <div className="app-shell">
    <header className="site-header">
      <Link className="brand" href="/"><span className="brand-mark">P</span><span>Perfume<span className="brand-soft">Store</span></span></Link>
      <button className="menu-toggle" onClick={() => setMenu(!menu)} aria-label="Abrir menu">☰</button>
      <nav className={"main-nav " + (menu ? "open" : "")} aria-label="Principal">
        <Link href="/catalog">Catálogo</Link>{user && <Link href="/account/orders">Meus pedidos</Link>}
        {user?.role === "ADMIN" && <Link href="/admin">Administração</Link>}
        <Link href="/cart">Carrinho <span className="cart-count">{cart?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0}</span></Link>
        {!loading && (user ? <><Link href="/account">Olá, {user.name.split(" ")[0]}</Link><button className="link-button" onClick={signOut}>Sair</button></> : <Link className="nav-cta" href="/login">Entrar</Link>)}
      </nav>
    </header>
    <main>{children}</main>
    <footer className="site-footer"><div><Link className="brand footer-brand" href="/">PerfumeStore</Link><p>Escolhas marcantes, com cuidado em cada detalhe.</p></div><div className="footer-links"><Link href="/contact">Atendimento</Link><Link href="/shipping">Entrega</Link><Link href="/privacy">Privacidade</Link><Link href="/terms">Termos</Link></div><small>Condições comerciais calculadas pelo backend.</small></footer>
  </div>;
}
