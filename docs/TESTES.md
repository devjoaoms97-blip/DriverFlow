# PolÃ­tica de testes (regressÃ£o)

**Regra do projeto:** tudo o que for implementado e validado ganha teste automatizado, para que
mudanÃ§as futuras nÃ£o quebrem o que jÃ¡ funciona. Isso vale para todas as etapas.

## DefiniÃ§Ã£o de pronto de uma funcionalidade

1. Regra de negÃ³cio nova â†’ teste unitÃ¡rio em `domain/` ou na funÃ§Ã£o pura correspondente
2. Acesso a dados novo (DAO/RepositÃ³rio) â†’ teste com Room em memÃ³ria (`RepositorioTest`)
3. LÃ³gica de estado de tela â†’ extrair para funÃ§Ã£o pura e testar (ex.: `montarEstadoHistorico`)
4. Bug encontrado â†’ primeiro um teste que reproduz o bug, depois a correÃ§Ã£o
5. `.\gradlew test` verde **antes** de cada commit
6. Itens de interface passam pelo roteiro manual abaixo

## Onde cada coisa Ã© testada

| O quÃª | Onde | Como roda |
|---|---|---|
| CÃ¡lculos (lucro, km/l, reserva, alertas) | `app/src/test/.../domain` | JUnit puro |
| FormataÃ§Ã£o e conversÃµes | `app/src/test/.../util`, `.../data/db` | JUnit puro |
| Estado das telas (histÃ³rico etc.) | `app/src/test/.../ui/...` | JUnit puro |
| RepositÃ³rio + Room (CRUD, vÃ­nculos, perÃ­odo) | `app/src/test/.../data` | Robolectric (no PC, sem celular) |

## Como rodar

```powershell
$env:JAVA_HOME = "C:\devtools\jdk17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = "D:\gradle-home"
.\gradlew test
```

RelatÃ³rio HTML: `app\build\reports\tests\testDebugUnitTest\index.html`.
Na primeira execuÃ§Ã£o o Robolectric baixa o Android SDK simulado (alguns minutos).

> **NÃ£o use `connectedAndroidTest` no celular com dados reais.** O Gradle desinstala o app ao final
> dos testes instrumentados, o que apaga os lanÃ§amentos guardados no aparelho.

## ConfiguraÃ§Ã£o necessÃ¡ria em `app/build.gradle.kts`

Dentro de `android { ... }`:

```kotlin
testOptions {
    unitTests.isIncludeAndroidResources = true
}
```

Dentro de `dependencies { ... }`:

```kotlin
testImplementation("org.robolectric:robolectric:4.13")
testImplementation("androidx.test:core:1.6.1")
```

## Roteiro manual de validaÃ§Ã£o (interface), a repetir a cada etapa

1. **CÃ¡lculo:** lanÃ§ar ganho Uber R$ 100,00 / 50 km / 4 h e despesa de R$ 30,00. InÃ­cio deve mostrar
   lucro R$ 70,00, ganho por km R$ 2,00, ganho por hora R$ 25,00 e custo por km R$ 0,60
2. **Data:** lanÃ§ar ganho com data de ontem e conferir em "Ontem" no HistÃ³rico
3. **Editar:** abrir um lanÃ§amento, alterar o valor, salvar e conferir o InÃ­cio
4. **Excluir:** excluir um lanÃ§amento (com confirmaÃ§Ã£o) e conferir que sumiu
5. **Filtros:** no HistÃ³rico, alternar Todos/Ganhos/Despesas e trocar de mÃªs
6. **PersistÃªncia:** fechar e reabrir o app; os dados continuam lÃ¡
