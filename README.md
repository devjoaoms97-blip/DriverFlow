# DriverFlow

(Antes chamado Motorista Financeiro; o pacote do app (`br.com.motoristafinanceiro`) mantém o nome antigo.)

App Android **de uso pessoal** para controle financeiro de motorista de aplicativo (Uber, 99 e outros):
ganhos, despesas, lucro líquido real, custo por km, consumo do carro, manutenção, reservas e metas.

Dados 100% locais no aparelho (sem login, sem servidor, sem nuvem).

## O que o app faz hoje

- **Ganhos e despesas** com data, plataforma, categoria, início e fim do turno e foto do comprovante (câmera ou galeria)
- **Início** com lucro líquido do dia, da semana (segunda a domingo) e do mês, ganho por km e por hora, custo por km, depreciação e alertas
- **Meta mensal e meta do dia**, com progresso, quanto falta por dia até o fim do mês e atalho para repetir a do mês anterior
- **Histórico** por mês, agrupado por dia, com filtros, edição de tudo que foi lançado e **relatório do mês em PDF** para comprovar renda
- **Veículo:** abastecimentos, consumo médio (km/l) e manutenções com alertas por km, com o odômetro estimado pelos km dos ganhos
- **Análises:** ganho por hora por dia da semana e por faixa de horário, melhores dia e horário, comparativo entre plataformas (Uber × 99) e evolução dos últimos 6 meses
- **Reservas** para despesas fixas (IPVA, seguro, parcela), com quanto separar por dia
- **Visual "Painel de bordo":** fonte Manrope, números em destaque e barra inferior com o + no centro, ao alcance do polegar
- **Ajustes:** depreciação por km, aparência, **bloqueio** com a digital, **lembrete** de lançar no fim do dia, **aviso de manutenção** e **backup** (exportar e restaurar um .zip com dados e fotos, e backup automático diário ou semanal numa pasta)

O que falta está no [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Stack

Kotlin · Jetpack Compose (Material 3) · Room (SQLite) · MVVM com StateFlow · Navigation Compose · DataStore

## Como rodar

O dia a dia do projeto (build e testes pelo Claude; teste no celular por você; commit e push pelo Claude só com sua autorização) está em
[`docs/FLUXO_DE_TRABALHO.md`](docs/FLUXO_DE_TRABALHO.md).

Para abrir pela primeira vez:

1. Use o **JDK 17** e uma pasta **sem acento e sem espaço** no caminho
2. Crie o `local.properties` com `sdk.dir=<caminho do Android SDK>` (o arquivo não vai para o Git)
3. Pelo Android Studio: abra a pasta e aguarde o Gradle Sync. Pelo terminal: `.\gradlew test installDebug`
   com o celular conectado (minSdk 26 / Android 8.0)

## Documentação

- [`docs/FLUXO_DE_TRABALHO.md`](docs/FLUXO_DE_TRABALHO.md): como desenvolvemos, fazemos commit, build e testes
- [`docs/TESTES.md`](docs/TESTES.md): política de testes de regressão e roteiro manual de validação
- [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md): escopo, requisitos, regras de negócio, modelo de dados e telas
- [`docs/ROADMAP.md`](docs/ROADMAP.md): etapas e checklist do que já está pronto

## Estrutura

```
app/src/main/java/br/com/motoristafinanceiro/
├── MotoristaApp.kt          # Application: banco, repositório, configurações e comprovantes
├── MainActivity.kt
├── data/
│   ├── model/               # Entidades Room e enums
│   ├── db/                  # AppDatabase, DAOs, conversores
│   ├── backup/              # Exportar e restaurar o backup (.zip: dados.json + fotos)
│   ├── Repositorio.kt       # único ponto de acesso aos dados do banco
│   ├── Configuracoes.kt     # ajustes do usuário (DataStore)
│   ├── ComprovanteStorage.kt, ProcessadorDeImagem.kt   # fotos dos comprovantes
├── domain/                  # Regras puras de cálculo (testáveis)
├── notificacoes/            # Aviso de manutenção: verificador, notificação e WorkManager
├── util/                    # Formatação de dinheiro, números e km
└── ui/
    ├── theme/, navigation/  # Tema (cores, Manrope, cantos) e NavHost + barra inferior
    ├── componentes/         # Cartão, indicador, seletor, chips, selos, barras e ícones do visual
    ├── home/, historico/    # Início e histórico
    ├── lancamento/          # Formulário de ganho/despesa
    ├── veiculo/             # Abastecimento, manutenção e alertas
    ├── reservas/, ajustes/  # Reservas (aberta pelo Início) e Ajustes
    ├── analises/            # Análises por dia da semana e por plataforma
    ├── meta/                # Tela da meta mensal e textos do card
    └── comprovante/         # Miniatura e ampliação da foto
```
