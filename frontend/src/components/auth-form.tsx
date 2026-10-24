"use client";

import Link from "next/link";
import { useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { apiClient } from "@/lib/api";
import { useSession } from "@/lib/session";
import { Button, ErrorNotice, Field, Notice, Panel, PageTitle } from "./ui";

export function LoginForm() {
  const router = useRouter(); const search = useSearchParams(); const { refresh } = useSession();
  const [email, setEmail] = useState(""); const [password, setPassword] = useState(""); const [error, setError] = useState<unknown>(); const [loading, setLoading] = useState(false);
  const submit = async (event: React.FormEvent) => { event.preventDefault(); setLoading(true); setError(undefined); try { await apiClient.post("/api/v1/auth/login", { email, password }); await apiClient.post("/api/v1/cart/claim").catch(() => undefined); await refresh(); const destination = search.get("next"); router.push(destination && destination.startsWith("/") ? destination : "/account"); } catch (reason) { setError(reason); } finally { setLoading(false); } };
  return <Panel><PageTitle eyebrow="Conta" title="Entrar" description="Acesse seus pedidos, endereços e carrinho." />{error !== undefined && <ErrorNotice error={error} />}<form className="stack" onSubmit={submit}><Field label="E-mail" name="email" type="email" autoComplete="email" required value={email} onChange={(event) => setEmail(event.target.value)} /><Field label="Senha" name="password" type="password" autoComplete="current-password" required value={password} onChange={(event) => setPassword(event.target.value)} /><Button loading={loading} type="submit">Entrar</Button></form><p className="muted"><Link href="/recover">Esqueci minha senha</Link> · <Link href="/register">Criar conta</Link></p></Panel>;
}

export function RegisterForm() {
  const router = useRouter(); const [name, setName] = useState(""); const [email, setEmail] = useState(""); const [password, setPassword] = useState(""); const [error, setError] = useState<unknown>(); const [done, setDone] = useState(false); const [loading, setLoading] = useState(false);
  const submit = async (event: React.FormEvent) => { event.preventDefault(); setLoading(true); setError(undefined); try { await apiClient.post("/api/v1/auth/register", { name, email, password }); setDone(true); setTimeout(() => router.push("/login"), 900); } catch (reason) { setError(reason); } finally { setLoading(false); } };
  return <Panel><PageTitle eyebrow="Conta" title="Criar conta" description="Use uma senha com pelo menos 12 caracteres." />{done && <Notice tone="success">Conta criada. Redirecionando para o login.</Notice>}{error !== undefined && <ErrorNotice error={error} />}<form className="stack" onSubmit={submit}><Field label="Nome" name="name" required value={name} onChange={(event) => setName(event.target.value)} /><Field label="E-mail" name="email" type="email" required value={email} onChange={(event) => setEmail(event.target.value)} /><Field label="Senha" name="password" type="password" minLength={12} required value={password} onChange={(event) => setPassword(event.target.value)} /><Button loading={loading} type="submit">Criar conta</Button></form><p className="muted"><Link href="/login">Já tenho uma conta</Link></p></Panel>;
}

export function RecoveryForm() {
  const [email, setEmail] = useState(""); const [message, setMessage] = useState(""); const [error, setError] = useState<unknown>();
  const submit = async (event: React.FormEvent) => { event.preventDefault(); setError(undefined); try { const result = await apiClient.post<{ message: string }>("/api/v1/auth/password-recovery-requests", { email }); setMessage(result.message); } catch (reason) { setError(reason); } };
  return <Panel><PageTitle eyebrow="Acesso" title="Recuperar senha" description="Se o e-mail estiver cadastrado, enviaremos instruções." />{message && <Notice tone="success">{message}</Notice>}{error !== undefined && <ErrorNotice error={error} />}<form className="stack" onSubmit={submit}><Field label="E-mail" name="email" type="email" required value={email} onChange={(event) => setEmail(event.target.value)} /><Button type="submit">Enviar instruções</Button></form><p className="muted"><Link href="/login">Voltar ao login</Link></p></Panel>;
}

export function ResetForm() {
  const search = useSearchParams(); const [token, setToken] = useState(search.get("token") ?? ""); const [password, setPassword] = useState(""); const [message, setMessage] = useState(""); const [error, setError] = useState<unknown>();
  const submit = async (event: React.FormEvent) => { event.preventDefault(); setError(undefined); try { await apiClient.post("/api/v1/auth/password-resets", { token, password }); setMessage("Senha redefinida. Você já pode entrar."); } catch (reason) { setError(reason); } };
  return <Panel><PageTitle eyebrow="Acesso" title="Nova senha" />{message && <Notice tone="success">{message}</Notice>}{error !== undefined && <ErrorNotice error={error} />}<form className="stack" onSubmit={submit}><Field label="Token do e-mail" name="token" required value={token} onChange={(event) => setToken(event.target.value)} /><Field label="Nova senha" name="password" type="password" minLength={12} required value={password} onChange={(event) => setPassword(event.target.value)} /><Button type="submit">Redefinir senha</Button></form></Panel>;
}
