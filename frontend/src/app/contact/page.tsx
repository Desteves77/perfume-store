import { PageTitle, Panel } from "@/components/ui";

export default function ContactPage() {
  return <div className="page container"><PageTitle eyebrow="Atendimento" title="Estamos aqui para ajudar" description="Consulte as informações da sua conta e do seu pedido para obter suporte." /><div className="two-col"><Panel><h2>Antes de falar conosco</h2><p className="muted">Tenha em mãos o número público do pedido. Os prazos, valores e estados exibidos na sua conta são sempre os dados confirmados pelo backend.</p></Panel><Panel><h2>Canal de suporte</h2><p className="muted">O canal de atendimento oficial será configurado antes da operação comercial. Em homologação, utilize os dados definidos pelo ambiente de teste.</p></Panel></div></div>;
}
