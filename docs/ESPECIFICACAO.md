# Especificação: Motorista Financeiro

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
| RF-01 | Lançar ganho: data, plataforma, valor bruto, gorjeta/bônus, km, horas online | 1 |
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
- minSdk 26 (Android 8.0), tema claro/escuro, cores dinâmicas no Android 12+
- Lógica de cálculo em `domain/` sem dependência de UI, coberta por testes unitários

## 5. Regras de negócio e fórmulas

| Indicador | Fórmula |
|---|---|
| Ganho bruto | Σ (valor + gorjeta/bônus) dos ganhos |
| Lucro líquido | ganho bruto − Σ despesas |
| Lucro após depreciação | lucro líquido − (km × depreciação por km) |
| Ganho por km | ganho bruto ÷ km rodados |
| Ganho por hora | ganho bruto ÷ horas online |
| Custo por km | (despesas + depreciação) ÷ km rodados |
| Consumo médio | pelo método do tanque cheio: km entre dois "cheios" ÷ litros abastecidos entre eles (incluindo o último) |
| Km restantes (manutenção) | odômetro da última + intervalo − odômetro atual (negativo = atrasada) |
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

**Despesas automáticas:** abastecimento e manutenção criam uma despesa vinculada (`origem` + `origemId`),
evitando lançamento duplo. Ao excluir o abastecimento/manutenção, a despesa vinculada também é excluída.

**Odômetro atual:** maior odômetro registrado entre abastecimentos e manutenções.

**Notificações de manutenção (RF-10):** vêm **ligadas** por padrão; o usuário desliga em Ajustes. No Android 13+ o app pergunta a permissão uma única vez ao abrir (se negar, Ajustes mostra como liberar). Avisa quando um serviço fica **próximo** (500 km ou menos) ou **atrasado**; "em dia" nunca avisa. Cada alerta é identificado pelo registro mais recente do serviço mais o status, então avisa **uma vez por estado**: próxima e depois atrasada são dois avisos, e uma nova troca começa um ciclo novo. A notificação mostra todos os alertas urgentes de uma vez (uma só, que substitui a anterior) e some quando nada mais está urgente ou quando a chave é desligada. Se o Android não permitir notificar, o alerta não é marcado como avisado e a verificação tenta de novo depois. A verificação roda (WorkManager) a cada mudança em manutenções, abastecimentos ou na chave, e uma vez por dia como rede de segurança. Como o odômetro só avança com abastecimento ou manutenção, é nesses momentos que o aviso aparece. Tocar na notificação abre a aba Veículo. A chave entra no backup (backups antigos, sem ela, mantêm a chave atual); ao restaurar, a lista de alertas já avisados é zerada, porque os ids dos registros podem ter mudado.

## 6. Modelo de dados (Room)

| Tabela | Campos |
|---|---|
| `ganhos` | id, data, plataforma, valorCentavos, gorjetaBonusCentavos, km, minutosOnline, observacao |
| `despesas` | id, data, categoria, valorCentavos, observacao, fotoUri, origem (MANUAL/ABASTECIMENTO/MANUTENCAO), origemId |
| `abastecimentos` | id, data, litros, valorTotalCentavos, odometroKm, tanqueCheio |
| `manutencoes` | id, tipo, data, odometroKm, valorCentavos, intervaloKm, observacao |
| `reservas` | id, nome, valorCentavos, periodicidade, proximoVencimento |
| `metas` | mes ("2026-10"), valorAlvoLucroCentavos |

Datas armazenadas como `epochDay` (Long) via `TypeConverter`. Enums armazenados pelo nome.

**Comprovantes:** o campo `fotoUri` da despesa guarda apenas o NOME do arquivo da foto, que fica em `filesDir/comprovantes/` (pasta privada do app). Fotos são reduzidas para cerca de 1.600 px e regravadas em JPEG, com a rotação do EXIF aplicada. Arquivos que nenhuma despesa usa são apagados na abertura do app, desde que tenham mais de 24 h.
**Configurações** (depreciação por km, aviso de manutenção ligado/desligado, alertas já avisados, se a permissão já foi perguntada etc.) ficam no DataStore, fora do Room.
Schema do Room exportado em `app/schemas/` para versionar migrações.

## 7. Telas

1. **Início:** lucro líquido (hoje/semana/mês), indicadores, card da meta (progresso e quanto falta por dia)
2. **Novo lançamento** (botão +): abas Ganho / Despesa
3. **Histórico:** lista por mês filtrável por tipo (Todos/Ganhos/Despesas). Com Ganhos, filtra também por plataforma; com Despesas, por categoria. Os dois refinos são limpos ao trocar o tipo. Com um refino ativo, aparece o total e a quantidade exibidos ("Uber no mês: R$ 450,00 (3 lançamentos)"); o resumo do mês inteiro não muda. Editar/excluir continuam valendo
4. **Veículo:** consumo médio, custo por km, abastecimentos, manutenções e alertas
5. **Reservas:** despesas recorrentes e total a separar por dia
6. **Análises:** período (30 dias, 90 dias ou tudo, sempre até hoje); resumo, ganho por hora por dia da semana (segunda a domingo, com barra, dias trabalhados e média por dia), melhor dia e comparativo por plataforma. Faixa de horário ainda não existe (ver decisões em aberto)
7. **Ajustes:** depreciação por km, aviso de manutenção (chave que liga e desliga as notificações; pede a permissão do Android 13+ se ainda não liberada) e backup (Exportar backup, Restaurar backup)
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

**`dados.json`:** `formato` ("motorista-financeiro-backup"), `versaoSchema` (hoje 1), `criadoEmMs`,
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

**Versões:** um backup mais novo que o app é recusado com pedido para atualizar. Quando o banco mudar (por exemplo, o
horário do turno), a `versaoSchema` sobe e o app continua lendo as versões antigas.

## 10. Decisões em aberto

- Lançamento por corrida individual além do lançamento por turno?
- Faixa de horário nas Análises (RF-13): o ganho hoje só tem data, sem horário. Para analisar por faixa é preciso registrar o horário de início do turno (campo novo no ganho, migração do Room da versão 1 para a 2, sem perder os dados atuais)
- Odômetro estimado: hoje o km do carro só avança com abastecimento ou manutenção, então o alerta (e a notificação) só muda nesses registros. Somar os km dos ganhos desde a última leitura do odômetro faria o alerta andar sozinho, mas é uma estimativa (km pessoais não entram)
- ~~Definição exata de "semana"~~ **Resolvido:** segunda a domingo
- ~~Valor padrão de depreciação por km~~ **Resolvido:** começa em R$ 0,00/km (ignorada) e o usuário define em Ajustes; teto de R$ 10,00/km
