# Roadmap

Legenda: ✅ feito · 🟡 esqueleto/parcial · ⬜ a fazer

## Qualidade (vale para todas as etapas)
- ✅ Política de testes de regressão: ver [`TESTES.md`](TESTES.md)
- ✅ Testes de domínio, formatação, estado das telas e Repositório/Room
- ✅ Testes de interface (Compose) dos fluxos principais (lançar, editar, excluir, filtrar e navegar), rodando no PC com Robolectric junto com os demais testes, sem emulador nem celular

## Identidade visual
- ✅ Ícone adaptativo do app (letra D com estrada e barras), com versão monocromática para ícones temáticos
- ✅ Nome DriverFlow e abertura animada (Lottie, 2,5 s, toque pula): fundo azul-marinho, o logo do ícone, o nome e o lema "Sua operação sob controle."
- ✅ Tema próprio do DriverFlow (azul-marinho do logo, azul nas ações, verde nos ganhos, vermelho nas despesas), sem as cores dinâmicas do Android
- ✅ Aparência em Ajustes: Automático (segue o celular), Claro ou Escuro (padrão Escuro, como a abertura)
- ✅ Novo visual "Painel de bordo": fonte Manrope com números tabulares, cartões com borda fina e números em destaque, barra inferior com o + no centro (Início, Histórico, +, Veículo, Análises) e Reservas aberta por um atalho no Início
- ✅ Novo visual também nos formulários de Abastecimento, Manutenção, Reserva e Meta (valor em destaque, botões de escolha, Salvar fixo no rodapé) e em Ajustes (seções em cartões)

## Etapa 1: Base, ganhos e despesas
- ✅ Projeto Gradle, dependências e tema
- ✅ Entidades, DAOs, banco Room, repositório
- ✅ Cálculos de domínio + testes unitários
- ✅ Formulário de ganho/despesa
- ✅ Seletor de data (calendário + atalhos Hoje/Ontem)
- ✅ Editar e excluir lançamentos (com confirmação)
- ✅ Foto do comprovante nas despesas (câmera ou galeria), guardada na pasta privada do app, com ampliação

## Etapa 2: Início e histórico
- ✅ Tela inicial (hoje, semana de segunda a domingo, mês e meta)
- ✅ Card de atenção com manutenções atrasadas ou próximas no Início
- ✅ Histórico por mês, agrupado por dia, com filtro Todos/Ganhos/Despesas
- ✅ Filtros por plataforma (nos Ganhos) e por categoria (nas Despesas), com total e quantidade do que está na tela

## Etapa 3: Veículo
- ✅ Tela Veículo: consumo médio (km/l), odômetro atual, último litro, custo por km do mês
- ✅ Registrar, editar e excluir abastecimentos (a despesa de combustível acompanha)
- ✅ Despesa de abastecimento no Histórico abre o formulário do abastecimento
- ✅ Depreciação por km nas configurações (DataStore): entra no custo por km e no lucro após depreciação

## Etapa 4: Manutenção e reservas
- ✅ Registrar, editar e excluir manutenções (a despesa acompanha), com intervalo em km sugerido por tipo
- ✅ Alertas por km (atrasada / próxima a 500 km / em dia) na tela Veículo e no Início
- ✅ Despesa de manutenção no Histórico abre o formulário da manutenção
- ✅ Notificações de manutenção (próxima a 500 km ou atrasada) com WorkManager, ligadas por padrão (chave em Ajustes); pedem a permissão do Android 13+ uma vez ao abrir o app, a chave entra no backup e tocar na notificação abre a aba Veículo
- ✅ Odômetro estimado: última leitura (abastecimento ou manutenção) + km dos ganhos dos dias seguintes; alertas, Início, Veículo e notificações usam a estimativa, e lançar um ganho também dispara a verificação
- ✅ Reservas: cadastro, edição e exclusão de despesas fixas (IPVA, seguro, parcela...), com quanto separar por dia e por semana
- ✅ Próximo vencimento avança sozinho quando a data cadastrada já passou

## Etapa 5: Metas
- ✅ Tela de meta mensal (definir, editar e remover), aberta pelo card do Início
- ✅ Progresso da meta no Início (barra, percentual) e "quanto falta por dia" até o fim do mês, contando hoje
- ✅ Repetir a meta do mês anterior com um toque (no card do Início e na tela da meta), quando o mês ainda não tem meta

## Etapa 6: Análises e backup
- ✅ Ganho por hora por dia da semana (com dias trabalhados e média por dia) e melhor dia
- ✅ Ganho por hora por faixa de horário (madrugada, manhã, tarde, noite): início do turno opcional no lançamento, banco na versão 2 (migração sem perder dados) e backup na versão 2
- ✅ Comparativo entre plataformas (Uber × 99 e as demais): ganho, média por lançamento, por hora e por km
- ✅ Tela Análises com período de 30 dias, 90 dias ou tudo
- ✅ Backup e restauração em um arquivo .zip (dados em JSON + fotos dos comprovantes), com validação antes de restaurar
- ✅ Tela de Ajustes (depreciação por km e backup; categorias e outros parâmetros só se fizerem falta)

## Antes de publicar
- ⬜ Zerar as versões de desenvolvimento: banco do Room e formato do backup voltam para a versão 1, já com todos os campos
  (apagar as migrações e os schemas antigos em `app/schemas/`). Ninguém de fora terá as versões anteriores.
  Cuidado com os dados reais do celular de desenvolvimento, que está numa versão maior e não abre num banco "mais velho":
  1. Exportar um backup no app atual
  2. Fazer a consolidação para a versão 1, garantindo que o backup exportado no passo 1 ainda seja lido
  3. Desinstalar o app e instalar a versão de lançamento
  4. Restaurar o backup e conferir lançamentos, metas, reservas e fotos
