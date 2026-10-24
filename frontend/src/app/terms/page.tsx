import { PageTitle, Panel } from "@/components/ui";

export default function TermsPage() {
  return <div className="page container"><PageTitle eyebrow="Condições" title="Termos da experiência" /><Panel><p className="muted">Os valores, a disponibilidade, o frete, os estados do pedido e as condições de pagamento são determinados pelo backend. O ambiente de homologação pode utilizar pagamento simulado e condições sintéticas.</p><p className="muted">Os termos comerciais definitivos, a política de entrega e as regras de pós-venda serão publicados antes da liberação pública.</p></Panel></div>;
}
