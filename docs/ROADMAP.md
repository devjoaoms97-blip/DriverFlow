# Roadmap

Legenda: ✅ feito · 🟡 esqueleto/parcial · ⬜ a fazer

## Qualidade (vale para todas as etapas)
- ✅ Política de testes de regressão: ver [`TESTES.md`](TESTES.md)
- ✅ Testes de domínio, formatação, estado do histórico e Repositório/Room
- ⬜ Testes de interface (Compose) para os fluxos principais, quando o app estiver mais estável

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
- ✅ Histórico por mês, agrupado por dia, com filtro Todos/Ganhos/Despesas
- ⬜ Filtros por plataforma e categoria

## Etapa 3: Veículo
- 🟡 Cálculo de km/l e custo por km (lógica pronta e testada, sem tela)
- ⬜ Tela de abastecimentos e formulário
- ⬜ Edição/exclusão de despesas geradas por abastecimento e manutenção (hoje só aparecem no histórico, sem edição)
- ⬜ Depreciação por km nas configurações (DataStore)

## Etapa 4: Manutenção e reservas
- 🟡 Lógica de km restantes e reserva diária (pronta e testada, sem tela)
- ⬜ Telas e formulários de manutenção e reservas
- ⬜ Notificações com WorkManager

## Etapa 5: Metas
- 🟡 Progresso da meta na Início (sem tela para definir a meta)
- ⬜ Tela de meta com "quanto falta por dia"

## Etapa 6: Análises e backup
- ⬜ Ganho por hora por dia da semana/horário
- ⬜ Comparativo Uber × 99
- ⬜ Backup/restauração JSON
- ⬜ Tela de Ajustes
