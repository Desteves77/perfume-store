import { PageTitle, Panel } from "@/components/ui";

export default function ShippingPage() {
  return <div className="page container"><PageTitle eyebrow="Entrega" title="Frete calculado no checkout" description="A disponibilidade, a tarifa e o prazo dependem do endereço informado e da cotação vigente." /><div className="two-col"><Panel><h2>Como funciona</h2><p className="muted">Escolha um endereço no checkout para consultar as condições retornadas pelo serviço de frete. Adicionar itens ao carrinho não reserva estoque nem fixa uma tarifa.</p></Panel><Panel><h2>Homologação</h2><p className="muted">As condições exibidas em ambientes de teste podem ser sintéticas. A política comercial definitiva depende da configuração oficial da loja.</p></Panel></div></div>;
}
