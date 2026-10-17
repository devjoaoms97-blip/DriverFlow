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
| RF-10 | Alertar manutenção próxima/atrasada (notificação) | 4 |
| RF-11 | Cadastrar despesas recorrentes e exibir reserva diária total | 4 |
| RF-12 | Definir meta mensal e mostrar progresso e quanto falta por dia | 5 |
| RF-13 | Análise de ganho por hora por dia da semana e faixa de horário | 6 |
| RF-14 | Comparativo entre plataformas | 6 |
| RF-15 | Exportar e importar backup JSON | 6 |
| RF-16 | Ajustes: depreciação por km, categorias, parâmetros | 6 |

## 4. Requisitos não funcionais

- **Offline por padrão**, sem permissões no MVP
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
| Progresso da meta | lucro líquido do mês ÷ meta |

**Despesas automáticas:** abastecimento e manutenção criam uma despesa vinculada (`origem` + `origemId`),
evitando lançamento duplo. Ao excluir o abastecimento/manutenção, a despesa vinculada também é excluída.

**Odômetro atual:** maior odômetro registrado entre abastecimentos e manutenções.

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
**Configurações** (depreciação por km, etc.) ficam no DataStore, fora do Room.
Schema do Room exportado em `app/schemas/` para versionar migrações.

## 7. Telas

1. **Início:** lucro líquido (hoje/mês), indicadores, progresso da meta
2. **Novo lançamento** (botão +): abas Ganho / Despesa
3. **Histórico:** lista filtrável, com editar/excluir
4. **Veículo:** consumo médio, custo por km, abastecimentos, manutenções e alertas
5. **Reservas:** despesas recorrentes e total a separar por dia
6. **Análises:** melhores dias/horários, Uber × 99
7. **Ajustes:** backup/restauração, depreciação por km

## 8. Arquitetura

```
UI (Compose) → ViewModel (StateFlow) → Repositorio → DAOs (Room)
                       ↓
                domain/ (cálculos puros)
```

- Injeção de dependência manual via `MotoristaApp` e `viewModelFactory` (Hilt pode entrar depois, se quiser)
- Um `ViewModel` por tela
- Fluxos reativos (`Flow`) do banco até a UI

## 9. Backup (JSON)

Exportar todas as tabelas em um único `.json` com campo `versaoSchema`, usando o seletor de arquivos do sistema
(Storage Access Framework, sem permissão extra). Importar substitui os dados atuais após confirmação.

## 10. Decisões em aberto

- Lançamento por corrida individual além do lançamento por turno?
- Definição exata de "semana" (segunda a domingo é a sugestão)
- ~~Valor padrão de depreciação por km~~ **Resolvido:** começa em R$ 0,00/km (ignorada) e o usuário define em Ajustes; teto de R$ 10,00/km
