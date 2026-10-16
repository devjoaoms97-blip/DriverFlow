# Roadmap

Legenda: ✅ feito · 🟡 esqueleto/parcial · ⬜ a fazer

## Qualidade (vale para todas as etapas)
- ✅ Política de testes de regressão: ver [`TESTES.md`](TESTES.md)
- ✅ Testes de domínio, formatação, estado das telas e Repositório/Room
- ⬜ Testes de interface (Compose) para os fluxos principais, quando o app estiver mais estável

## Identidade visual
- ✅ Ícone adaptativo do app (moeda com volante), com versão monocromática para ícones temáticos
- ⬜ Nome e tela de abertura (splash) personalizados, se quiser

## Etapa 1: Base, ganhos e despesas
- ✅ Projeto Gradle, dependências e tema
- ✅ Entidades, DAOs, banco Room, repositório
- ✅ Cálculos de domínio + testes unitários
- ✅ Formulário de ganho/despesa
- ✅ Seletor de data (calendário + atalhos Hoje/Ontem)
- ✅ Editar e excluir lançamentos (com confirmação)
- ⬜ Anexar foto do comprovante

## Etapa 2: Início e histórico
- 🟡 Tela inicial (hoje e mês; falta semana)
- ✅ Card de atenção com manutenções atrasadas ou próximas no Início
- ✅ Histórico por mês, agrupado por dia, com filtro Todos/Ganhos/Despesas
- ⬜ Filtros por plataforma e categoria

## Etapa 3: Veículo
- ✅ Tela Veículo: consumo médio (km/l), odômetro atual, último litro, custo por km do mês
- ✅ Registrar, editar e excluir abastecimentos (a despesa de combustível acompanha)
- ✅ Despesa de abastecimento no Histórico abre o formulário do abastecimento
- ✅ Depreciação por km nas configurações (DataStore): entra no custo por km e no lucro após depreciação

## Etapa 4: Manutenção e reservas
- ✅ Registrar, editar e excluir manutenções (a despesa acompanha), com intervalo em km sugerido por tipo
- ✅ Alertas por km (atrasada / próxima a 500 km / em dia) na tela Veículo e no Início
- ✅ Despesa de manutenção no Histórico abre o formulário da manutenção
- ⬜ Notificações com WorkManager (hoje os alertas aparecem só dentro do app)
- 🟡 Reserva diária (lógica pronta e testada, sem tela)
- ⬜ Telas e formulários de reservas

## Etapa 5: Metas
- 🟡 Progresso da meta na Início (sem tela para definir a meta)
- ⬜ Tela de meta com "quanto falta por dia"

## Etapa 6: Análises e backup
- ⬜ Ganho por hora por dia da semana/horário
- ⬜ Comparativo Uber × 99
- ⬜ Backup/restauração JSON
- 🟡 Tela de Ajustes (só a depreciação por km por enquanto; falta o backup)
