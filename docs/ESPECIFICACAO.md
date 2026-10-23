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
- Leitor de corridas: a conta da oferta da Uber/99 por cima do app, sem aceitar nada sozinho

### Fora do escopo (decisão do projeto)
- Multiusuário, login, nuvem e sincronização
- Importação automática (OCR/relatórios das plataformas)
- Multi-veículo
- Relatórios fiscais (MEI, Carnê-Leão, IR)

## 3. Requisitos funcionais

| ID | Requisito | Etapa |
|---|---|---|
| RF-01 | Lançar ganho: data, plataforma, valor bruto, gorjeta/bônus, km do turno, corridas (opcional) e início e fim do turno (as horas são a diferença) | 1 |
| RF-01b | Lançar km pessoal (uso do carro fora do trabalho): data, km e observação; editar e excluir pela aba Veículo | 3 |
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
| Ganho por corrida | ganho dos lançamentos que informam corridas ÷ suas corridas. **Tempo por corrida** = horas dos lançamentos com corridas e horas ÷ suas corridas. Sem corridas no período, os dois não aparecem (Início e Análises) |
| Horas do turno | fim − início do turno, passando da meia-noite normalmente (22:00 às 02:00 = 4 h). O fim não é guardado: é sempre início + minutos. Pausas no meio do turno contam como horas. Só o início preenchido: o ganho fica sem horas. Só o fim, ou fim igual ao início: não deixa salvar. Lançamento antigo, com horas e sem horário, mantém as horas até que início e fim sejam preenchidos |
| Custo por km | (despesas + depreciação dos km de trabalho e pessoais) ÷ (km dos ganhos + km pessoais). Os km pessoais entram no divisor porque as despesas também pagam o uso pessoal. O ganho por km e o lucro após depreciação continuam só com os km de trabalho |
| Custo do uso pessoal | custo por km × km pessoais do período; só informativo no cartão do lucro (o lucro líquido não muda) |
| Consumo médio | pelo método do tanque cheio: km entre dois "cheios" ÷ litros abastecidos entre eles (incluindo o último) |
| Odômetro (alertas) | **estimado** = maior leitura real (abastecimento ou manutenção; empate fica com a mais recente) + km dos ganhos e dos km pessoais lançados **nos dias seguintes** a ela. Os do mesmo dia da leitura não entram (não dá para saber se foram antes ou depois). Km não lançados não entram, então a estimativa tende a ficar abaixo do real: o alerta pode chegar um pouco tarde, nunca cedo. Uma nova leitura zera a estimativa |
| Conferência do odômetro (Veículo) | entre a última leitura e a mais antiga dos 90 dias anteriores a ela: km reais = diferença do odômetro; km lançados = ganhos + pessoais dos dias depois da leitura de partida até o dia da última, inclusive. Mostra o % lançado e, fora de 90% a 110%, pede atenção (km esquecidos deixam o custo por km alto; acima de 110%, km dos turnos ou odômetro errados). Não aparece sem duas leituras em dias diferentes ou com menos de 100 km entre elas. Como o "Km rodados" do ganho é o turno inteiro (com os deslocamentos sem passageiro), o km vazio não é separado |
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
| Meta do dia | (meta do mês − lucro líquido até ontem) ÷ dias que faltam no mês contando hoje, arredondado para cima. Fica fixa durante o dia; o lucro de hoje enche a barra e o cartão fica verde ao bater. Não aparece sem meta ou com o mês já batido até ontem |
| Evolução mês a mês | os 6 meses até o atual: lucro líquido (ganhos − despesas) e R$/h (ganho bruto ÷ horas online) de cada mês. Prejuízo em vermelho. Melhor mês = mês fechado de maior lucro (o atual não entra), só com 2 ou mais meses fechados com lançamento |
| Repetir meta | quando o mês ainda não tem meta e o anterior tinha (dentro do limite), o card do Início oferece "Repetir a de setembro: R$ X" (grava na hora) e a tela da meta oferece o mesmo atalho para preencher o campo |
| Faixas de horário (Análises) | madrugada 0h–6h, manhã 6h–12h, tarde 12h–18h, noite 18h–24h. Cada ganho com início do turno e horas online é repartido entre as faixas na proporção do tempo que passou em cada uma (um turno das 17h às 20h: 1/3 na tarde e 2/3 na noite; turno que passa da meia-noite continua na madrugada). Ganho por hora da faixa = ganho repartido ÷ horas na faixa. Melhor faixa só com 2 ou mais faixas com horas. Ganhos sem início ou sem horas não entram, e a tela diz quantos ficaram de fora |

**Despesas automáticas:** abastecimento e manutenção criam uma despesa vinculada (`origem` + `origemId`),
evitando lançamento duplo. Ao excluir o abastecimento/manutenção, a despesa vinculada também é excluída.

**Odômetro atual:** maior odômetro registrado entre abastecimentos e manutenções.

**Notificações de manutenção (RF-10):** vêm **ligadas** por padrão; o usuário desliga em Ajustes. No Android 13+ o app pergunta a permissão uma única vez ao abrir (se negar, Ajustes mostra como liberar). Avisa quando um serviço fica **próximo** (500 km ou menos) ou **atrasado**; "em dia" nunca avisa. Cada alerta é identificado pelo registro mais recente do serviço mais o status, então avisa **uma vez por estado**: próxima e depois atrasada são dois avisos, e uma nova troca começa um ciclo novo. A notificação mostra todos os alertas urgentes de uma vez (uma só, que substitui a anterior) e some quando nada mais está urgente ou quando a chave é desligada. Se o Android não permitir notificar, o alerta não é marcado como avisado e a verificação tenta de novo depois. A verificação roda (WorkManager) a cada mudança em manutenções, abastecimentos ou na chave, e uma vez por dia como rede de segurança. Como o odômetro só avança com abastecimento ou manutenção, é nesses momentos que o aviso aparece. Tocar na notificação abre a aba Veículo. A chave entra no backup (backups antigos, sem ela, mantêm a chave atual); ao restaurar, a lista de alertas já avisados é zerada, porque os ids dos registros podem ter mudado.

## 6. Modelo de dados (Room)

| Tabela | Campos |
|---|---|
| `ganhos` | id, data, plataforma, valorCentavos, gorjetaBonusCentavos, km, minutosOnline, observacao, inicioTurnoMinutos (minutos desde a meia-noite, opcional; desde a versão 2), corridas (opcional; desde a versão 3) |
| `km_pessoais` | id, data, km, observacao (desde a versão 3) |
| `despesas` | id, data, categoria, valorCentavos, observacao, fotoUri, origem (MANUAL/ABASTECIMENTO/MANUTENCAO), origemId |
| `abastecimentos` | id, data, litros, valorTotalCentavos, odometroKm, tanqueCheio |
| `manutencoes` | id, tipo, data, odometroKm, valorCentavos, intervaloKm, observacao |
| `reservas` | id, nome, valorCentavos, periodicidade, proximoVencimento |
| `metas` | mes ("2026-10"), valorAlvoLucroCentavos |

Datas armazenadas como `epochDay` (Long) via `TypeConverter`. Enums armazenados pelo nome.

**Comprovantes:** o campo `fotoUri` da despesa guarda apenas o NOME do arquivo da foto, que fica em `filesDir/comprovantes/` (pasta privada do app). Fotos são reduzidas para cerca de 1.600 px e regravadas em JPEG, com a rotação do EXIF aplicada. Arquivos que nenhuma despesa usa são apagados na abertura do app, desde que tenham mais de 24 h.
**Configurações** (depreciação por km, aviso de manutenção ligado/desligado, alertas já avisados, se a permissão já foi perguntada, aparência, bloqueio, lembrete, backup automático e as faixas e o modo gravação do leitor de corridas) ficam no DataStore, fora do Room.

**Bloqueio do app:** opcional (Ajustes > Segurança), desligado por padrão. Usa a digital/rosto ou o bloqueio de tela do próprio celular (`BiometricPrompt` com `BIOMETRIC_WEAK | DEVICE_CREDENTIAL`), sem PIN próprio do app. Pede ao abrir (depois da abertura animada, que toca inteira por cima da tela de bloqueio) e ao voltar depois de 2 minutos fora (girar a tela ou tirar a foto de um comprovante não bloqueia). Ao ligar, pede a digital uma vez; sem bloqueio de tela no celular, não liga e explica. Fica só no aparelho, fora do backup.

**Lembrete de lançamento:** opcional (Ajustes > Notificações), desligado por padrão, às 22:00 se não mudar. No horário, avisa "Lançou o turno de hoje?" se não há **ganho** com a data de hoje (despesa sozinha não conta). Tocar abre o Novo lançamento. Agendado com WorkManager para o horário, e cada execução marca a do dia seguinte.

**Repetir o último:** no ganho novo, um atalho copia a plataforma e os horários (início e fim) do ganho mais recente (maior data; no mesmo dia, o último lançado). O valor, os km e a gorjeta não são copiados.

**Leitor de corridas:** serviço de acessibilidade (`leitor/LeitorDeCorridasService`) que o motorista liga em Configurações > Acessibilidade; Ajustes > Leitor de corridas mostra se está ligado e abre esse caminho. Só recebe eventos da Uber Driver (`com.ubercab.driver`) e da 99 Motorista (`com.app99.driver`). A cada rajada de eventos (no máximo a cada 250 ms) lê os textos visíveis das janelas desses apps e procura uma oferta (`domain/Corrida.kt`, `lerOferta`):
- valor: o primeiro "R$ 12,50" que não seja acréscimo ("+R$ 3,00") nem taxa ("R$ 1,50/km");
- trechos: cada linha com distância (km ou m) é um trecho, com o tempo da mesma linha (min, ou h e min); o primeiro é até o passageiro e o segundo é a viagem; um trecho só conta como a viagem;
- nota: 1,00 a 5,00 numa linha com ★ ou sozinha. Sem valor ou sem distância, não é oferta.

Com uma oferta, mostra no alto da tela uma janela de acessibilidade (`TYPE_ACCESSIBILITY_OVERLAY`, sem a permissão de sobrepor apps): **R$/km** = valor ÷ (km até o passageiro + km da viagem), **R$/h** = valor ÷ (tempo até o passageiro + tempo da viagem) e **lucro ≈** valor − custo por km × km total, com o custo por km dos últimos 30 dias (despesas + depreciação ÷ km, recalculado a cada 10 minutos; sem km lançado, o lucro não aparece). Cor pelo R$/km: verde a partir de R$ 2,00, amarelo a partir de R$ 1,50 e vermelho abaixo (faixas editáveis; o amarelo não pode passar do verde). A janela some 1,5 s depois de a oferta sair da tela, depois de 30 s ou ao tocar nela. **Nunca toca, aceita ou recusa** nada no outro app (o serviço não pede gestos). Nome e endereços não são guardados.

**Modo gravação** (para aprender o formato real do card de cada app): com a chave ligada, o texto das telas da Uber/99 que têm "R$" ou "km" vai para `filesDir/leitor/gravacao.txt` (com hora, app, tipo e id de cada componente), pulando telas repetidas, até 5 MB. Ajustes mostra o tamanho e permite Compartilhar (FileProvider) e Apagar; o arquivo pode ter nome de passageiro. Ainda não há histórico das ofertas aceitas e recusadas: entra depois de ajustar a leitura com as gravações.

**Relatório do mês (PDF):** botão no Histórico, para o mês da tela, com todos os lançamentos (sem os filtros). A4, com título, resumo (ganho bruto, despesas, lucro, dias trabalhados, km, horas), ganhos por plataforma, despesas por categoria e a lista de lançamentos por data (quebra de página quando não cabe). Rodapé diz que os valores foram lançados pelo próprio motorista. Salvar (seletor do Android) ou Compartilhar (arquivo temporário em `cacheDir/relatorios`, via FileProvider).
Schema do Room exportado em `app/schemas/` para versionar migrações. **Versão atual: 2.** Migrações em
`data/db/AppDatabase.kt` (`MIGRACOES`), nunca apagadas: 1 → 2 acrescenta `ganhos.inicioTurnoMinutos` (nulo nos
lançamentos antigos). O `MigracaoTest` cria um banco da versão 1 com o SQL do schema exportado e abre com o app atual.

## 7. Telas

Navegação: barra inferior com Início, Histórico, **+** (no centro), Veículo e Análises. O + abre o Novo lançamento;
na aba Veículo, pergunta se é abastecimento ou manutenção. Reservas, Meta e Ajustes abrem a partir do Início, sem a barra.

1. **Início:** data, seletor Hoje/Semana/Mês, cartão do lucro líquido do período (barra lucro × despesas, ganho bruto e despesas; depreciação quando configurada), indicadores (ganho por km, ganho por hora, custo por km, km e horas rodados), aviso de manutenção (atrasadas e próximas, abre a aba Veículo), meta do dia, aviso de backup velho, card da meta (progresso e quanto falta por dia; sem meta, o atalho para repetir a do mês anterior) e atalho de Reservas (quanto separar hoje e o próximo vencimento)
2. **Novo lançamento** (botão +; no ganho novo, atalho "Repetir o último"): seletor Ganho / Despesa / Km pessoal (só km, data e observação), valor em destaque na cor do tipo, plataforma ou categoria em chips, data com atalhos Hoje/Ontem, início e fim do turno (relógio de 24 h; as horas são calculadas e aparecem embaixo), km e corridas lado a lado, gorjeta, e o botão de salvar fixo no rodapé
3. **Histórico:** botão do relatório do mês em PDF; lista por mês filtrável por tipo (Todos/Ganhos/Despesas). Com Ganhos, filtra também por plataforma; com Despesas, por categoria. Os dois refinos são limpos ao trocar o tipo. Com um refino ativo, aparece o total e a quantidade exibidos ("Uber no mês: R$ 450,00 (3 lançamentos)"); o resumo do mês inteiro não muda. Editar/excluir continuam valendo
4. **Veículo:** odômetro em destaque ("estimado", com a última leitura e os km somados, quando há ganhos ou km pessoais depois dela), consumo médio, último litro e custo por km do mês; cartão "Km lançados × odômetro"; botões Abastecer e Manutenção; um cartão por manutenção com intervalo (selo atrasada/próxima/em dia e barra do quanto do intervalo já foi rodado); manutenções feitas, abastecimentos e uso pessoal (km pessoais, tocar edita)
5. **Reservas** (aberta pelo atalho do Início; o + do cabeçalho cadastra): despesas recorrentes e total a separar por dia
6. **Análises:** período (30 dias, 90 dias ou tudo, sempre até hoje); resumo, ganho por hora por dia da semana (segunda a domingo, com barra, dias trabalhados e média por dia), melhor dia, ganho por hora por faixa de horário (com a melhor faixa e quantos lançamentos ficaram de fora), comparativo por plataforma e evolução dos últimos 6 meses
7. **Ajustes:** depreciação por km, aparência (Automático, Claro ou Escuro), segurança (bloqueio), leitor de corridas (estado, faixas de cor do R$/km e modo gravação), lembrete de lançamento, aviso de manutenção (chave que liga e desliga as notificações; pede a permissão do Android 13+ se ainda não liberada) e backup (Exportar backup, Restaurar backup e backup automático)
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

**`dados.json`:** `formato` ("motorista-financeiro-backup"), `versaoSchema` (hoje 3; a 1 e a 2 continuam sendo lidas), `criadoEmMs`,
`depreciacaoPorKmCentavos`, `notificacoesManutencaoAtivas` (opcional: backups antigos não têm, e ao restaurá-los a chave atual é mantida) e as tabelas `ganhos` (com `corridas` opcional desde a 3), `despesas`, `abastecimentos`, `manutencoes`, `reservas`, `metas` e `kmPessoais` (desde a 3; backups antigos não têm e restauram sem km pessoais).
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

**Backup automático:** Desligado (padrão), Diário ou Semanal, numa pasta escolhida uma vez no seletor de pastas do Android (a permissão fica guardada). Uma tarefa do WorkManager confere a cada 12 h (e ao abrir o app) se é hora, com 1 h de folga, e grava `driverflow-auto-AAAA-MM-DD.zip` (o do mesmo dia é substituído). Mantém os 7 automáticos mais recentes; arquivos sem esse prefixo (como os backups manuais) nunca são apagados. Falha (pasta sem permissão, sem espaço) fica registrada e aparece em Ajustes; a próxima rodada tenta de novo. O último backup, manual ou automático, é guardado: o Início avisa quando ele tem 7 dias ou mais, ou quando nunca houve backup e já há lançamentos. A pasta só aparece no seletor se o provedor aceitar pastas (Downloads, Documentos e cartão de memória aceitam; alguns apps de nuvem não).

**Versões:** um backup mais novo que o app é recusado com pedido para atualizar. Quando o banco muda, a `versaoSchema`
sobe e o app continua lendo as versões antigas. Versão 2: ganhos com `inicioTurnoMinutos` (opcional, de 0 a 1439;
backups da versão 1 restauram com os ganhos sem horário).

## 10. Decisões em aberto

- Lançamento por corrida individual além do lançamento por turno?
- ~~Faixa de horário nas Análises (RF-13)~~ **Resolvido:** início do turno opcional no ganho (banco e backup na versão 2)
- ~~Odômetro estimado~~ **Resolvido:** última leitura + km dos ganhos dos dias seguintes (regra na seção 5), sempre ligado
- ~~Definição exata de "semana"~~ **Resolvido:** segunda a domingo
- ~~Valor padrão de depreciação por km~~ **Resolvido:** começa em R$ 0,00/km (ignorada) e o usuário define em Ajustes; teto de R$ 10,00/km
