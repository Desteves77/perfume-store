import { PageTitle, Panel } from "@/components/ui";

export default function PrivacyPage() {
  return <div className="page container"><PageTitle eyebrow="Privacidade" title="Uso responsável dos dados" /><Panel><p className="muted">A aplicação utiliza dados necessários para autenticação, carrinho, pedidos, entrega e comunicação transacional. Não armazena tokens de sessão no navegador via JavaScript e não expõe credenciais ou dados financeiros sensíveis na interface.</p><p className="muted">A política jurídica definitiva deverá ser revisada antes da operação comercial pública.</p></Panel></div>;
}
