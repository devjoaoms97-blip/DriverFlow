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

## Ideias futuras
Feitas em 22/10/2026: todas; o leitor de corridas está na primeira etapa (gravação e conta por cima), falta ajustar com cards reais e guardar o histórico das ofertas.

**Proteger os dados**
- ✅ Backup automático diário ou semanal numa pasta escolhida uma vez (seletor de pastas do Android), mantendo os 7 mais recentes, com aviso no Início quando o último backup tem 7 dias ou mais (ou nunca foi feito)
- ✅ Bloqueio opcional com a digital ou o bloqueio de tela do celular, ao abrir e ao voltar depois de 2 minutos fora

**Decidir melhor na rua**
- 🟡 Leitor de corridas: quando uma corrida toca na Uber/99, o app lê o card (valor, km até o passageiro, km da viagem, tempo, nota do passageiro, destino) e mostra por cima, em menos de 1 s, o R$/km contando o deslocamento até o passageiro, o R$/h, o lucro estimado pelo custo por km real e uma cor (verde/amarelo/vermelho) por limites definidos pelo motorista. Guarda o histórico das ofertas (aceitas e recusadas) para as Análises
  - Como: serviço de acessibilidade (lê o texto da tela de outros apps) + janela sobreposta. Se o card não expuser o texto, plano B: captura de tela com OCR (mais pesado, com aviso de gravação do Android)
  - Primeiro passo: "modo gravação" com corridas reais, para aprender o formato do card de cada app (não dá para simular uma corrida tocando)
    - ✅ Etapa 1 (22/10/2026): serviço de acessibilidade só para Uber Driver e 99 Motorista, modo gravação (compartilhar e apagar), leitura do card por regras genéricas, janela por cima com R$/km, R$/h, lucro estimado pelo custo por km dos últimos 30 dias e cor pelas faixas de Ajustes
    - ⬜ Etapa 2: ajustar a leitura com as gravações reais de cada app (e conferir o nome do pacote da 99)
    - ⬜ Etapa 3: histórico das ofertas (aceitas e recusadas) nas Análises, sem nome nem nota do passageiro
  - Regras: só mostrar, **nunca aceitar sozinho** (aceitar automaticamente viola os termos das plataformas e pode bloquear a conta); nome e nota do passageiro não são guardados (LGPD)
  - Riscos: atualização da Uber/99 pode quebrar a leitura (manutenção contínua); ler a tela é zona cinzenta nos termos de uso; para publicar na Play Store, acessibilidade fora do uso para pessoas com deficiência exige declaração e revisão
- ✅ Meta do dia: (meta do mês − lucro até ontem) ÷ dias que faltam; cartão no Início que fica verde ao bater
- ✅ Evolução mês a mês nas Análises: lucro dos últimos 6 meses em barras, com o R$/h e o melhor mês fechado

**Números mais certos** (22/10/2026)
- ✅ Km pessoal: terceiro tipo no + (Ganho / Despesa / Km pessoal), com data, km e observação; entra no custo por km e no odômetro estimado, aparece no cartão do lucro (custo do uso pessoal) e na seção "Uso pessoal" do Veículo (editar e excluir). Banco e backup na versão 3
- ✅ Corridas no ganho (opcional): ganho por corrida e tempo médio por corrida no Início e nas Análises (total e por plataforma)
- ✅ Conferência do odômetro no Veículo: km reais entre duas leituras × km lançados (turnos + pessoais), com o % lançado e aviso de km esquecidos ou lançados a mais
  - O km vazio (sem passageiro) não é separado: o "Km rodados" do ganho já é o turno inteiro. Dá para separar depois com o "km com passageiro" que o leitor de corridas vai guardar no histórico das ofertas
- ⬜ Reservas com saldo ("já separei R$ X") e aviso quando o vencimento chega sem o valor guardado
- ⬜ Turno com cronômetro (Iniciar/Encerrar, também num bloco das Configurações rápidas) que preenche início e horas
- ⬜ Widget na tela inicial com o lucro do dia e o quanto falta na meta do dia
- ⬜ Etanol × gasolina: tipo de combustível no abastecimento e, com o km/l real de cada um, qual compensa pelo preço da bomba

**Menos atrito para lançar**
- ✅ Lembrete no fim do dia ("Lançou o turno de hoje?"), no horário escolhido, só em dias sem ganho lançado; tocar abre o Novo lançamento
- ✅ Repetir o último lançamento (mesma plataforma e horários do turno anterior; só troca o valor)
- ✅ Relatório do mês em PDF (resumo, totais por plataforma e categoria e lançamentos), salvo ou compartilhado pelo Histórico
