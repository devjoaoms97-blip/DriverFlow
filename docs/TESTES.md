# Política de testes (regressão)

**Regra do projeto:** tudo o que for implementado e validado ganha teste automatizado, para que
mudanças futuras não quebrem o que já funciona. Isso vale para todas as etapas.

## Definição de pronto de uma funcionalidade

1. Regra de negócio nova → teste unitário em `domain/` ou na função pura correspondente
2. Acesso a dados novo (DAO/Repositório) → teste com Room em memória (`Repositorio*Test`)
3. Lógica de estado de tela → extrair para função pura e testar (ex.: `montarEstadoHistorico`, `montarEstadoVeiculo`, `montarEstadoHome`)
4. Bug encontrado → primeiro um teste que reproduz o bug, depois a correção
5. `.\gradlew test` verde **antes** de cada commit
6. Itens de interface passam pelo roteiro manual abaixo
7. Quando uma regra muda de propósito, o teste antigo é atualizado no mesmo commit, dizendo o porquê

## Onde cada coisa é testada

| O quê | Onde | Como roda |
|---|---|---|
| Cálculos (lucro, km/l, reserva, alertas de manutenção, resumo do veículo) | `app/src/test/.../domain` | JUnit puro |
| Formatação e conversões | `app/src/test/.../util`, `.../data/db` | JUnit puro |
| Configurações (DataStore) | `app/src/test/.../data/ConfiguracoesTest` | JUnit puro, arquivo temporário |
| Ícone e manifest (arquivos de recurso presentes e referenciados) | `app/src/test/.../app/IconeDoAppTest` | JUnit puro |
| Estado e regras das telas (início, histórico, veículo, alertas) | `app/src/test/.../ui/...` | JUnit puro |
| Repositório + Room (CRUD, vínculos, período) | `app/src/test/.../data` | Robolectric (no PC, sem celular) |

## Como rodar

```powershell
$env:JAVA_HOME = "C:\devtools\jdk17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = "D:\gradle-home"
.\gradlew test
```

Relatório HTML: `app\build\reports\tests\testDebugUnitTest\index.html`.
Para forçar a execução do zero: `.\gradlew :app:testDebugUnitTest --rerun`.

> **Não use `connectedAndroidTest` no celular com dados reais.** O Gradle desinstala o app ao final
> dos testes instrumentados, o que apaga os lançamentos guardados no aparelho.

## Configuração necessária em `app/build.gradle.kts`

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

## Roteiro manual de validação (interface), a repetir a cada etapa

1. **Cálculo:** lançar ganho Uber R$ 100,00 / 50 km / 4 h e despesa de R$ 30,00. Início deve mostrar
   lucro R$ 70,00, ganho por km R$ 2,00, ganho por hora R$ 25,00 e custo por km R$ 0,60
2. **Data:** lançar ganho com data de ontem e conferir em "Ontem" no Histórico
3. **Editar:** abrir um lançamento, alterar o valor, salvar e conferir o Início
4. **Excluir:** excluir um lançamento (com confirmação) e conferir que sumiu
5. **Filtros:** no Histórico, alternar Todos/Ganhos/Despesas e trocar de mês
6. **Persistência:** fechar e reabrir o app; os dados continuam lá
7. **Abastecimento:** na aba Veículo, tocar em + e escolher Abastecimento; registrar 40 L, R$ 240,00,
   odômetro 10.000 (tanque cheio) e depois 30 L, R$ 195,00, odômetro 10.400 (tanque cheio). O resumo deve
   mostrar consumo 13,3 km/l, odômetro 10.400 km e último litro R$ 6,50/L
8. **Vínculo:** no Histórico, a despesa de combustível aparece com "Abastecimento"; tocar nela abre o
   formulário do abastecimento (e não o de despesa)
9. **Editar abastecimento:** mudar o valor e conferir que a despesa no Histórico e o lucro no Início mudaram junto
10. **Excluir abastecimento:** excluir e conferir que a despesa de combustível também sumiu
11. **Manutenção:** com os abastecimentos do passo 7 registrados, tocar em + na aba Veículo, escolher
    Manutenção, Troca de óleo, odômetro 10.000, R$ 250,00 e intervalo 10.000 (vem sugerido). Deve aparecer
    "Alertas de manutenção: Troca de óleo, Faltam 9.600 km"
12. **Alerta próximo:** registrar um abastecimento de teste com odômetro 19.700; o alerta vira "Faltam 300 km"
    e o card "Atenção: manutenção" aparece no Início. Registrar outro com odômetro 20.200: "Atrasada em 200 km"
    (excluir esses abastecimentos de teste depois)
13. **Vínculo da manutenção:** no Histórico a despesa aparece com "Manutenção"; tocar abre o formulário da
    manutenção. Mudar o valor e conferir que a despesa acompanha
14. **Excluir manutenção:** excluir e conferir que a despesa e o alerta sumiram
15. **Ajustes:** no Início, tocar na engrenagem, informar depreciação de 0,25 e salvar
16. **Depreciação no Início:** com apenas o ganho de R$ 100,00 / 50 km e a despesa de R$ 30,00 do passo 1
    lançados hoje, o card "Hoje" deve mostrar depreciação estimada R$ 12,50, lucro após depreciação R$ 57,50
    e custo por km R$ 0,85, com o lucro líquido continuando R$ 70,00
17. **Depreciação no Veículo:** o custo por km do mês também passa a incluir a depreciação, com o aviso no
    resumo. Voltar o ajuste para vazio ou 0 e conferir que tudo volta ao valor de antes
18. **Validação e persistência:** digitar texto inválido (ex.: "abc") mostra o erro e desabilita Salvar;
    fechar e reabrir o app mantém o valor salvo
19. **Ícone:** na tela inicial do celular, o app aparece como uma moeda dourada com um volante sobre fundo
    verde, com o nome "Motorista Financeiro". Em Android 13 ou mais novo com ícones temáticos ligados,
    o ícone assume as cores do tema do aparelho
20. **Reservas:** na aba Reservas, tocar em + e escolher o atalho IPVA (vem anual). Valor 3650,00 e um vencimento
    daqui a alguns meses. O card "Separar por dia" deve mostrar R$ 10,00
21. **Total:** cadastrar também um Seguro mensal de R$ 300,00. O total passa a R$ 20,00 por dia e R$ 140,00 por semana
22. **Vencimento passado:** cadastrar uma parcela mensal com vencimento num mês que já passou. A lista deve mostrar
    o próximo vencimento futuro, com "Vence em N dias", e a ordem fica pelo vencimento mais próximo
23. **Editar e excluir:** abrir uma reserva, mudar o valor (o "por dia" acompanha) e excluí-la com confirmação
