# Motorista Financeiro

App Android **de uso pessoal** para controle financeiro de motorista de aplicativo (Uber, 99 e outros):
ganhos, despesas, lucro líquido real, custo por km, consumo do carro, reservas e metas.

Dados 100% locais no aparelho (sem login, sem servidor, sem nuvem).

## Stack

Kotlin · Jetpack Compose (Material 3) · Room (SQLite) · MVVM com StateFlow · Navigation Compose · DataStore · WorkManager

## Como abrir

1. Abra a pasta no **Android Studio** (Koala ou mais novo) e aguarde o *Gradle Sync*.
   O Android Studio gera o `gradle wrapper` automaticamente; se quiser versionar o wrapper no Git, rode `gradle wrapper` uma vez.
2. Use JDK 17.
3. Rode no emulador ou no celular (minSdk 26 / Android 8.0).

> **Atenção:** este esqueleto foi escrito sem acesso a um ambiente Android, então ainda **não foi compilado**.
> Pode haver pequenos ajustes de import/versão no primeiro sync. Os testes de `domain/` (`./gradlew test`) cobrem a lógica de cálculo.

## Documentação

- [`docs/ESPECIFICACAO.md`](docs/ESPECIFICACAO.md): escopo, requisitos, regras de negócio, modelo de dados e telas
- [`docs/ROADMAP.md`](docs/ROADMAP.md): etapas e checklist do que já está pronto

## Estrutura

```
app/src/main/java/br/com/motoristafinanceiro/
├── MotoristaApp.kt          # Application: cria banco e repositório
├── MainActivity.kt
├── data/
│   ├── model/               # Entidades Room e enums
│   ├── db/                  # AppDatabase, DAOs, conversores
│   └── Repositorio.kt       # único ponto de acesso a dados
├── domain/                  # Regras puras de cálculo (testáveis)
├── util/                    # Formatação de dinheiro
└── ui/
    ├── theme/
    ├── navigation/          # NavHost + barra inferior
    ├── home/                # Tela inicial (lucro, indicadores, meta)
    └── lancamento/          # Formulário de ganho/despesa
```
