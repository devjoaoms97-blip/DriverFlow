# Especificação: DriverFlow

## 1. Objetivo

Dar ao motorista de aplicativo uma visão clara do **lucro líquido real**, descontando custos do carro,
com lançamento rápido (poucos toques) e funcionamento offline.

**Público:** uso pessoal (um usuário, um veículo, um aparelho).

## 2. Escopo

### Incluído
- Lançamento de ganhos por plataforma (Uber, 99, InDrive, particular, outra)
- Lançamento de despesas por categoria, com foto de comprovante opcional
- Lucro líquido por dia/semana/mês, ganho por km, ganho por hora, custo por km
- Abastecimentos com consumo médio (km/l)
- Manutenção com alertas por km
- Reserva diária para despesas recorrentes/anuais (IPVA, seguro, parcela etc.)
- Meta mensal de lucro líquido
- Análises: melhores dias/horários, comparativo Uber × 99
- Backup/restauração em arquivo JSON

### Fora do escopo (decisão do projeto)
- Multiusuário, login, nuvem e sincronização
- Importação automática (OCR/relatórios das plataformas)
- Multi-veículo
- Relatórios fiscais (MEI, Carnê-Leão, IR)

## 3. Requisitos funcionais

| ID | Requisito | Etapa |
|---|---|---|
| RF-01 | Lançar ganho: data, plataforma, valor bruto, gorjeta/bônus, km e início e fim do turno (as horas são a diferença) | 1 |
| RF-02 | Lançar despesa: data, categoria, valor, observação, foto opcional | 1 |
| RF-03 | Editar e excluir lançamentos | 1 |
| RF-04 | Tela inicial com lucro líquido (hoje, semana, mês) e indicadores | 2 |
| RF-05 | Histórico com filtro por período, plataforma e categoria | 2 |
| RF-06 | Registrar abastecimento (litros, valor, odômetro, tanque cheio) e gerar despesa de combustível automaticamente | 3 |
| RF-07 | Calcular consumo médio (km/l) | 3 |
| RF-08 | Exibir custo por km (despesas + depreciação ÷ km) | 3 |
| RF-09 | Registrar manutenção com intervalo em km e gerar despesa automaticamente | 4 |
| RF-10 | Alertar manutenção próxima/atrasada (notificação, ligada em Ajustes) | 4 |
| RF-11 | Cadastrar despesas recorrentes e exibir reserva diária total | 4 |
| RF-12 | Definir meta mensal e mostrar progresso e quanto falta por dia | 5 |
| RF-13 | Análise de ganho por hora por dia da semana e faixa de horário | 6 |
| RF-14 | Comparativo entre plataformas | 6 |
| RF-15 | Exportar e importar backup (.zip com JSON e fotos) | 6 |
| RF-16 | Ajustes: depreciação por km, categorias, parâmetros | 6 |

## 4. Requisitos não funcionais

- **Offline por padrão.** A única permissão é a de notificações (Android 13+), pedida uma única vez ao abrir o app, enquanto o aviso de manutenção estiver ligado (o padrão)
- Lançamento em até 3 toques para os casos comuns
- Valores monetários sempre em **centavos (Long)**
- minSdk 26 (Android 8.0), tema próprio claro/escuro (sem cores dinâmicas)
- Lógica de cálculo em `domain/` sem dependência de UI, coberta por testes unitários

## 5. Regras de negócio e fórmulas

| Indicador | Fórmula |
|---|---|
| Ganho bruto | Σ (valor + gorjeta/bônus) dos ganhos |
| Lucro líquido | ganho bruto − Σ despesas |
| Lucro após depreciação | lucro líquido − (km × depreciação por km) |
| Ganho por km | ganho bruto ÷ km rodados |
| Ganho por hora | ganho bruto ÷ horas online |
| Horas do turno | fim − início do turno, passando da meia-noite normalmente (22:00 às 02:00 = 4 h). O fim não é guardado: é sempre início + minutos. Pausas no meio do turno contam como horas. Só o início preenchido: o ganho fica sem horas. Só o fim, ou fim igual ao início: não deixa salvar. Lançamento antigo, com horas e sem horário, mantém as horas até que início e fim sejam preenchidos |
| Custo por km | (despesas + depreciação) ÷ km rodados |
| Consumo médio | pelo método do tanque cheio: km entre dois "cheios" ÷ litros abastecidos entre eles (incluindo o último) |
| Odômetro (alertas) | **estimado** = maior leitura real (abastecimento ou manutenção; empate fica com a mais recente) + km dos ganhos lançados **nos dias seguintes** a ela. Ganhos do mesmo dia da leitura não entram (não dá para saber se foram antes ou depois). Km fora dos apps não entram, então a estimativa tende a ficar abaixo do real: o alerta pode chegar um pouco tarde, nunca cedo. Uma nova leitura zera a estimativa |
| Km restantes (manutenção) | odômetro da última + intervalo − odômetro (estimado) atual (negativo = atrasada) |
| Reserva diária | valor da despesa ÷ dias do período (mensal 30, trimestral 91, semestral 182, anual 365) |
| Próximo vencimento | data cadastrada; se já passou, avança de 1, 3, 6 ou 12 meses (conforme a periodicidade), sempre contando a partir da data original |
| Desempenho (Análises) | ganho bruto (com gorjeta/bônus), lançamentos e dias trabalhados (datas diferentes). **Ganho por hora** = ganho dos lançamentos que têm horas ÷ suas horas; **ganho por km** = ganho dos lançamentos que têm km ÷ seus km. Lançamento sem horas ou sem km não entra na divisão, para não inflar o valor |
| Melhor dia por hora | dia da semana com maior ganho por hora, só quando pelo menos 2 dias da semana têm horas informadas; empate fica com o dia que vem primeiro na semana |
| Semana | de segunda a domingo. No Início soma os lançamentos da semana de hoje, mesmo quando ela atravessa dois meses (o card do mês continua só com o mês) |
| Progresso da meta | lucro líquido do mês ÷ meta (percentual arredondado para baixo; a barra para em 100%, o percentual pode passar) |
| Falta da meta | meta - lucro líquido do mês (nunca negativo) |
| Dias restantes | dias do mês de hoje até o último dia, **contando hoje** (dia 15 de outubro: 17 dias) |
| Falta por dia | falta da meta ÷ dias restantes, arredondado para cima; no último dia do mês é tudo o que falta |
| Meta batida | lucro líquido do mês ≥ meta; o card mostra quanto passou e não mostra mais "por dia" |
| Repetir meta | quando o mês ainda não tem meta e o anterior tinha (dentro do limite), o card do Início oferece "Repetir a de setembro: R$ X" (grava na hora) e a tela da meta oferece o mesmo atalho para preencher o campo |
| Faixas de horário (Análises) | madrugada 0h–6h, manhã 6h–12h, tarde 12h–18h, noite 18h–24h. Cada ganho com início do turno e horas online é repartido entre as faixas na proporção do tempo que passou em cada uma (um turno das 17h às 20h: 1/3 na tarde e 2/3 na noite; turno que passa da meia-noite continua na madrugada). Ganho por hora da faixa = ganho repartido ÷ horas na faixa. Melhor faixa só com 2 ou mais faixas com horas. Ganhos sem início ou sem horas não entram, e a tela diz quantos ficaram de fora |

**Despesas automáticas:** abastecimento e manutenção criam uma despesa vinculada (`origem` + `origemId`),
evitando lançamento duplo. Ao excluir o abastecimento/manutenção, a despesa vinculada também é excluída.

**Odômetro atual:** maior odômetro registrado entre abastecimentos e manutenções.

**Notificações de manutenção (RF-10):** vêm **ligadas** por padrão; o usuário desliga em Ajustes. No Android 13+ o app pergunta a permissão uma única vez ao abrir (se negar, Ajustes mostra como liberar). Avisa quando um serviço fica **próximo** (500 km ou menos) ou **atrasado**; "em dia" nunca avisa. Cada alerta é identificado pelo registro mais recente do serviço mais o status, então avisa **uma vez por estado**: próxima e depois atrasada são dois avisos, e uma nova troca começa um ciclo novo. A notificação mostra todos os alertas urgentes de uma vez (uma só, que substitui a anterior) e some quando nada mais está urgente ou quando a chave é desligada. Se o Android não permitir notificar, o alerta não é marcado como avisado e a verificação tenta de novo depois. A verificação roda (WorkManager) a cada mudança em manutenções, abastecimentos ou na chave, e uma vez por dia como rede de segurança. Como o odômetro só avança com abastecimento ou manutenção, é nesses momentos que o aviso aparece. Tocar na notificação abre a aba Veículo. A chave entra no backup (backups antigos, sem ela, mantêm a chave atual); ao restaurar, a lista de alertas já avisados é zerada, porque os ids dos registros podem ter mudado.

## 6. Modelo de dados (Room)

| Tabela | Campos |
|---|---|
| `ganhos` | id, data, plataforma, valorCentavos, gorjetaBonusCentavos, km, minutosOnline, observacao, inicioTurnoMinutos (minutos desde a meia-noite, opcional; desde a versão 2) |
| `despesas` | id, data, categoria, valorCentavos, observacao, fotoUri, origem (MANUAL/ABASTECIMENTO/MANUTENCAO), origemId |
| `abastecimentos` | id, data, litros, valorTotalCentavos, odometroKm, tanqueCheio |
| `manutencoes` | id, tipo, data, odometroKm, valorCentavos, intervaloKm, observacao |
| `reservas` | id, nome, valorCentavos, periodicidade, proximoVencimento |
| `metas` | mes ("2026-10"), valorAlvoLucroCentavos |

Datas armazenadas como `epochDay` (Long) via `TypeConverter`. Enums armazenados pelo nome.

**Comprovantes:** o campo `fotoUri` da despesa guarda apenas o NOME do arquivo da foto, que fica em `filesDir/comprovantes/` (pasta privada do app). Fotos são reduzidas para cerca de 1.600 px e regravadas em JPEG, com a rotação do EXIF aplicada. Arquivos que nenhuma despesa usa são apagados na abertura do app, desde que tenham mais de 24 h.
**Configurações** (depreciação por km, aviso de manutenção ligado/desligado, alertas já avisados, se a permissão já foi perguntada etc.) ficam no DataStore, fora do Room.
Schema do Room exportado em `app/schemas/` para versionar migrações. **Versão atual: 2.** Migrações em
`data/db/AppDatabase.kt` (`MIGRACOES`), nunca apagadas: 1 → 2 acrescenta `ganhos.inicioTurnoMinutos` (nulo nos
lançamentos antigos). O `MigracaoTest` cria um banco da versão 1 com o SQL do schema exportado e abre com o app atual.

## 7. Telas

Navegação: barra inferior com Início, Histórico, **+** (no centro), Veículo e Análises. O + abre o Novo lançamento;
na aba Veículo, pergunta se é abastecimento ou manutenção. Reservas, Meta e Ajustes abrem a partir do Início, sem a barra.

1. **Início:** data, seletor Hoje/Semana/Mês, cartão do lucro líquido do período (barra lucro × despesas, ganho bruto e despesas; depreciação quando configurada), indicadores (ganho por km, ganho por hora, custo por km, km e horas rodados), aviso de manutenção (atrasadas e próximas, abre a aba Veículo), card da meta (progresso e quanto falta por dia; sem meta, o atalho para repetir a do mês anterior) e atalho de Reservas (quanto separar hoje e o próximo vencimento)
2. **Novo lançamento** (botão +): seletor Ganho / Despesa, valor em destaque na cor do tipo, plataforma ou categoria em chips, data com atalhos Hoje/Ontem, início e fim do turno (relógio de 24 h; as horas são calculadas e aparecem embaixo), km e gorjeta lado a lado, e o botão de salvar fixo no rodapé
3. **Histórico:** lista por mês filtrável por tipo (Todos/Ganhos/Despesas). Com Ganhos, filtra também por plataforma; com Despesas, por categoria. Os dois refinos são limpos ao trocar o tipo. Com um refino ativo, aparece o total e a quantidade exibidos ("Uber no mês: R$ 450,00 (3 lançamentos)"); o resumo do mês inteiro não muda. Editar/excluir continuam valendo
4. **Veículo:** odômetro em destaque ("estimado", com a última leitura e os km somados, quando há ganhos depois dela), consumo médio, último litro e custo por km do mês; botões Abastecer e Manutenção; um cartão por manutenção com intervalo (selo atrasada/próxima/em dia e barra do quanto do intervalo já foi rodado); manutenções feitas e abastecimentos
5. **Reservas** (aberta pelo atalho do Início; o + do cabeçalho cadastra): despesas recorrentes e total a separar por dia
6. **Análises:** período (30 dias, 90 dias ou tudo, sempre até hoje); resumo, ganho por hora por dia da semana (segunda a domingo, com barra, dias trabalhados e média por dia), melhor dia, ganho por hora por faixa de horário (com a melhor faixa e quantos lançamentos ficaram de fora) e comparativo por plataforma
7. **Ajustes:** depreciação por km, aparência (Automático, Claro ou Escuro), aviso de manutenção (chave que liga e desliga as notificações; pede a permissão do Android 13+ se ainda não liberada) e backup (Exportar backup, Restaurar backup)
8. **Meta mensal** (aberta pelo card do Início): definir, editar e remover a meta do mês (R$ 0,01 a R$ 100.000,00). Cada mês tem a sua meta; mês novo começa sem meta

## 8. Arquitetura

```
UI (Compose) → ViewModel (StateFlow) → Repositorio → DAOs (Room)
                       ↓
                domain/ (cálculos puros)
```

- Injeção de dependência manual via `MotoristaApp` e `viewModelFactory` (Hilt pode entrar depois, se quiser)
- Um `ViewModel` por tela
- Fluxos reativos (`Flow`) do banco até a UI
- Visual "Painel de bordo": fonte Manrope (`res/font/manrope.ttf`, arquivo variável, licença OFL em `docs/licencas/`) com algarismos tabulares, escala de texto e cantos em `ui/theme/Tipografia.kt`, e componentes comuns (cartão, indicador, seletor, chip, selo, barras, ícones de traço) em `ui/componentes/`
- Tema próprio (`ui/theme`), escuro e claro, escolhido em Ajustes > Aparência (Automático, Claro ou Escuro; padrão Escuro). Azul-marinho do logo no fundo escuro, azul nas ações, verde nos ganhos, vermelho nas despesas e âmbar nos alertas próximos. Sem as cores dinâmicas do Android, para o app ter a mesma cara em qualquer celular. A escolha fica só no aparelho e não entra no backup
- `notificacoes/`: `VerificadorDeManutencao` (regra, sem Android) + `NotificadorAndroid` (canal e notificação) + `VerificarManutencaoWorker` e `AgendadorDeNotificacoes` (WorkManager)

## 9. Backup

**Arquivo:** um único `.zip`, escolhido pelo usuário no seletor de arquivos do sistema (Storage Access Framework,
sem permissão extra), sugerido como `motorista-financeiro-backup-AAAA-MM-DD.zip`. Por que `.zip` e não só `.json`:
as fotos dos comprovantes entram no backup, e fotos em base64 dentro de um JSON ficariam enormes.

```
motorista-financeiro-backup-2026-10-20.zip
├── dados.json            texto legível, com todas as tabelas
└── comprovantes/         só as fotos que alguma despesa usa
    └── comprovante_*.jpg
```

**`dados.json`:** `formato` ("motorista-financeiro-backup"), `versaoSchema` (hoje 2; a 1 continua sendo lida), `criadoEmMs`,
`depreciacaoPorKmCentavos`, `notificacoesManutencaoAtivas` (opcional: backups antigos não têm, e ao restaurá-los a chave atual é mantida) e as tabelas `ganhos`, `despesas`, `abastecimentos`, `manutencoes`, `reservas` e `metas`.
Datas em ISO (`2026-10-20`), meses como `2026-10`, enums pelo nome e dinheiro em centavos. Os **ids são mantidos**,
porque as despesas geradas por abastecimento/manutenção apontam para eles.

**Exportar:** lê todas as tabelas em uma única transação e grava o `.zip`. Comprovante referenciado que não existe mais
no aparelho fica de fora e é avisado ao final.

**Restaurar** (substitui os dados atuais, com confirmação):
1. Lê e **valida o arquivo inteiro antes de tocar em qualquer dado**. Se algo estiver errado, mostra o motivo
   ("ganhos #2: campo \"data\" não é uma data válida") e **nada é alterado**.
2. Copia as fotos (só adiciona), troca todas as tabelas em **uma transação** (se falhar, volta ao que era),
   grava a depreciação e só então apaga as fotos que não pertencem mais a nenhuma despesa.
3. Despesa cuja foto não veio no arquivo fica sem foto (e isso é avisado).

**Validação:** formato e versão; tabelas e campos obrigatórios; tipos; enums conhecidos; datas válidas; valores, km e
minutos não negativos; ids positivos e sem repetição; despesa de abastecimento/manutenção apontando para um registro
que existe no backup; nomes de foto simples (sem pasta nem `..`); depreciação dentro do teto. Entradas do `.zip` com
caminho suspeito são ignoradas.

**Versões:** um backup mais novo que o app é recusado com pedido para atualizar. Quando o banco muda, a `versaoSchema`
sobe e o app continua lendo as versões antigas. Versão 2: ganhos com `inicioTurnoMinutos` (opcional, de 0 a 1439;
backups da versão 1 restauram com os ganhos sem horário).

## 10. Decisões em aberto

- Lançamento por corrida individual além do lançamento por turno?
- ~~Faixa de horário nas Análises (RF-13)~~ **Resolvido:** início do turno opcional no ganho (banco e backup na versão 2)
- ~~Odômetro estimado~~ **Resolvido:** última leitura + km dos ganhos dos dias seguintes (regra na seção 5), sempre ligado
- ~~Definição exata de "semana"~~ **Resolvido:** segunda a domingo
- ~~Valor padrão de depreciação por km~~ **Resolvido:** começa em R$ 0,00/km (ignorada) e o usuário define em Ajustes; teto de R$ 10,00/km
