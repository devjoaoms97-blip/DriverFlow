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
| Cálculos (lucro, km/l, reserva, alertas de manutenção, resumo do veículo, meta mensal, análises) | `app/src/test/.../domain` | JUnit puro |
| Formatação e conversões | `app/src/test/.../util`, `.../data/db` | JUnit puro |
| Configurações (DataStore) | `app/src/test/.../data/ConfiguracoesTest` | JUnit puro, arquivo temporário |
| Tema (paleta própria, fundo igual ao da abertura, contraste mínimo de 4,5:1 em todos os pares texto/fundo, sem cores dinâmicas) | `app/src/test/.../ui/theme/EsquemaDeCoresTest` | JUnit puro |
| Ícone, nome e abertura (recursos presentes e referenciados, tema da splash, `installSplashScreen` antes do `onCreate`) | `app/src/test/.../app/IconeDoAppTest`, `IdentidadeVisualTest`, `AnimacaoAberturaTest` (o JSON do Lottie), `ComprovanteManifestTest` | JUnit (o JSON usa Robolectric) puro |
| Arquivos de comprovante (criar, apagar, órfãos) | `app/src/test/.../data/ComprovanteStorageTest` | JUnit puro, pasta temporária |
| Notificações de manutenção (regra de avisar uma vez por estado, textos, verificador com Room e permissão negada, chave e alertas avisados no DataStore, estado da chave em Ajustes, manifest e ícone) | `app/src/test/.../domain/NotificacoesManutencaoTest`, `.../notificacoes`, `.../data/ConfiguracoesNotificacoesTest`, `.../ui/ajustes/EstadoNotificacaoTest`, `.../app/NotificacaoManifestTest` | JUnit puro; o verificador usa Robolectric com Room em memória |
| Backup (leitura e validação do JSON, arquivo .zip, exportar/restaurar, textos) | `app/src/test/.../data/backup`, `.../data/RepositorioBackupTest`, `.../ui/ajustes` | JSON, zip e restauração com Robolectric; datas, fotos e textos em JUnit puro |
| Estado e regras das telas (início, semana, histórico e seus filtros, veículo, alertas, textos da meta, análises) | `app/src/test/.../ui/...` | JUnit puro |
| Novo visual (fonte Manrope e licença, números tabulares, escala de texto e cantos; textos do Início, siglas do Histórico, selo e barra das manutenções, barras e percentuais das Análises, data e prefixo R$ do lançamento; abas da barra inferior e telas sem barra) | `app/src/test/.../ui/theme/TipografiaTest`, `.../ui/home/TextoInicioTest`, `.../ui/historico/SiglasHistoricoTest`, `.../ui/veiculo/AlertasTextoTest`, `.../ui/analises/TextoAnalisesTest`, `.../ui/lancamento/CamposLancamentoTest`, `.../ui/navigation/BarraInferiorTest` | JUnit puro |
| Repositório + Room (CRUD, vínculos, período) | `app/src/test/.../data` | Robolectric (no PC, sem celular) |
| Odômetro estimado (leitura + km dos ganhos dos dias seguintes; fluxo do Repositório; notificação disparada por ganho) | `app/src/test/.../domain/OdometroTest`, `.../data/RepositorioOdometroTest`, `.../notificacoes/VerificadorDeManutencaoTest`, `.../ui/veiculo/TextoOdometroTest` | JUnit puro; Repositório e verificador com Robolectric |
| Faixas de horário (minutos por faixa, turno que passa da meia-noite, rateio do ganho, melhor faixa, textos) e repetir a meta | `app/src/test/.../domain/FaixasHorarioTest`, `.../domain/MetasTest`, `.../ui/analises/TextoAnalisesTest`, `.../ui/meta/TextoMetaTest`, `.../ui/home/HomeEstadoTest` | JUnit puro |
| Migração do banco (v1 criado com o SQL do schema exportado, dados mantidos, campo novo grava; cadeia de migrações até a versão atual) e backup v2 | `app/src/test/.../data/db/MigracaoTest`, `.../data/backup/BackupJsonTest` | Robolectric |
| Horas do turno pelo início e fim (meia-noite, só um dos horários, fim igual ao início, lançamento antigo sem horário) | `app/src/test/.../ui/lancamento/TurnoTest` | JUnit puro |
| Formulários no visual novo (esqueleto comum, sem campos antigos) | `app/src/test/.../ui/componentes/FormulariosNoVisualTest`, `FormularioTest` | JUnit puro |
| **Interface (Compose):** lançar ganho e despesa, Salvar desabilitado sem valor, filtrar, editar e excluir no Histórico, Reservas pelo Início e voltar, Abastecer, Análises vazia | `app/src/test/.../ui/FluxosPrincipaisTest` | Robolectric com `createComposeRule` e o app de verdade (banco limpo a cada teste); só na variante debug, porque a Activity de teste vem do `ui-test-manifest` |

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
19. **Ícone e nome:** na tela inicial do celular, o app aparece com o nome "DriverFlow" e o ícone da letra D azul com
    estrada e barras verdes sobre fundo azul-marinho. Em Android 13 ou mais novo com ícones temáticos ligados,
    o ícone vira uma silhueta com as cores do tema do aparelho
    19b. **Abertura animada:** abrir o app com ele fechado: fundo azul-marinho, o logo do ícone (D azul, estrada e barras
    verdes) aparece com um leve zoom, depois o nome "DriverFlow", o lema "Sua operação sob controle." e uma barrinha
    azul que enche da esquerda para a direita. O nome e o lema ficam logo abaixo do logo (e não colados no logo ou fora da tela). Dura 2,5 s e termina com
    um fade para a tela Início, sem piscar de branco. Tocar na tela durante a animação pula direto para o Início.
    No Android 13+, na primeira abertura, a pergunta de permissão de notificação aparece por cima da animação
    19c. **Abertura só no início do app:** girar a tela depois que a animação acabou não toca de novo. Tocar numa
    notificação de manutenção com o app fechado abre direto a aba Veículo, sem a animação. Fechar o app de verdade (deslizar
    nos apps recentes) ou sair pelo botão voltar e abrir de novo: a animação toca outra vez. Minimizar e voltar sem fechar não toca
20. **Reservas:** no Início, tocar no atalho Reservas e, na tela que abre, no + do cabeçalho; escolher o atalho IPVA (vem anual). Valor 3650,00 e um vencimento
    daqui a alguns meses. O card "Separar por dia" deve mostrar R$ 10,00
21. **Total:** cadastrar também um Seguro mensal de R$ 300,00. O total passa a R$ 20,00 por dia e R$ 140,00 por semana
22. **Vencimento passado:** cadastrar uma parcela mensal com vencimento num mês que já passou. A lista deve mostrar
    o próximo vencimento futuro, com "Vence em N dias", e a ordem fica pelo vencimento mais próximo
23. **Editar e excluir:** abrir uma reserva, mudar o valor (o "por dia" acompanha) e excluí-la com confirmação
24. **Câmera:** em uma nova despesa, tocar em "Tirar foto", fotografar um papel e confirmar na câmera. A miniatura
    aparece no formulário. Salvar a despesa
25. **Comprovante no Histórico:** a despesa mostra "Com comprovante". Abrir, tocar na miniatura e conferir que a
    foto amplia em uma janela com o botão Fechar
26. **Galeria:** em outra despesa, tocar em "Galeria" e escolher uma foto grande. A miniatura aparece e a despesa salva
27. **Orientação:** tirar uma foto com o celular na vertical e conferir que a miniatura não aparece deitada
28. **Campos preservados:** preencher valor e observação, abrir a câmera e voltar. Os campos continuam preenchidos
29. **Trocar e remover:** editar uma despesa com foto, trocar por outra e salvar; depois editar de novo, tocar em
    "Remover" e salvar. A despesa deixa de mostrar "Com comprovante"
30. **Cancelar a câmera:** abrir a câmera e voltar sem fotografar. Nenhuma miniatura aparece e nada quebra
31. **Excluir despesa com foto:** excluir e conferir que ela some do Histórico (o arquivo da foto também é apagado)
32. **Meta sem definir:** em um mês sem meta, o Início mostra o card "Meta do mês" com o botão "Definir meta"
33. **Definir meta:** tocar em "Definir meta" (a barra inferior some), digitar 5000 e conferir o aviso
    "≈ R$ 161,29 por dia" (outubro, 31 dias). Salvar volta ao Início
34. **Card da meta:** deve mostrar a barra, "R$ lucro de R$ 5.000,00 (N%)", "Faltam R$ X por dia." e
    "Faltam R$ Y em N dias (contando hoje)." A conta é (meta - lucro do mês) ÷ dias restantes, arredondada
    para cima. Ex.: em 19/10, com lucro do mês de R$ 70,00, restam 13 dias e faltam R$ 379,24 por dia
35. **Progresso acompanha os lançamentos:** lançar um ganho de R$ 200,00. O lucro, o percentual e o "por dia" mudam;
    excluir o ganho faz tudo voltar
36. **Editar meta:** tocar em "Editar meta", mudar para 4000 e salvar. O card passa a usar a nova meta
37. **Validação:** digitar "abc", "0" ou "-5" mostra o erro e desabilita Salvar. Campo vazio também deixa Salvar desabilitado
38. **Meta batida:** definir uma meta menor que o lucro do mês (ex.: 10,00). O card mostra
    "Meta batida! Você passou R$ X da meta.", a barra cheia e some a linha de dias restantes
39. **Remover meta:** em "Editar meta", tocar em "Remover meta" e confirmar. O card volta a "Definir meta"
    e os lançamentos continuam iguais. "Cancelar" na confirmação não remove nada
40. **Persistência:** fechar e reabrir o app mantém a meta definida
41. **Semana no Início:** entre "Hoje" e "Este mês" aparece o card "Esta semana (12/10 a 18/10)", com as datas
    de segunda a domingo da semana atual. Lançar um ganho de R$ 100,00 hoje: Hoje, Esta semana e Este mês sobem juntos
42. **Limite da semana:** lançar um ganho com data do domingo anterior à semana atual. Ele não entra em
    "Esta semana", mas entra em "Este mês" se for do mesmo mês. Excluir esse ganho de teste depois
43. **Semana na virada do mês** (só quando a semana atual cruza dois meses): "Esta semana" inclui os dias do mês
    anterior, e "Este mês" continua só com o mês atual
44. **Filtro por plataforma:** no Histórico, tocar em Ganhos. Aparece uma linha de chips (Todas, Uber, 99, InDrive,
    Particular, Outra). Escolher Uber mostra só os ganhos da Uber, o saldo de cada dia muda para o que está na tela e
    surge "Uber no mês: R$ X (N lançamentos)". Escolher "Todas" volta tudo
45. **Filtro por categoria:** tocar em Despesas. A linha de chips das categorias rola para o lado. Escolher
    Combustível mostra só essas despesas e o total ("Combustível no mês: R$ X (N lançamentos)")
46. **Trocar o tipo limpa o refino:** com Uber escolhido em Ganhos, ir para Despesas e voltar para Ganhos. O chip
    volta em "Todas". No tipo Todos não aparecem chips nem o total
47. **Mês e resumo:** com um refino ativo, trocar de mês mantém o refino, e o "Lucro líquido do mês" no topo
    continua o do mês inteiro (não muda com o filtro)
48. **Sem resultado:** escolher uma plataforma ou categoria sem lançamentos mostra "Nenhum lançamento neste período"
    e o total R$ 0,00 (0 lançamentos)
49. **Aba Análises:** aparecem os chips 30 dias, 90 dias e Tudo. Em um período sem ganhos, mostra
    "Nenhum ganho lançado neste período."
50. **Resumo:** com apenas o ganho Uber de R$ 100,00 / 50 km / 4 h e um ganho 99 de R$ 60,00 / 30 km / 2 h no período,
    o "Resumo do período" mostra ganho bruto R$ 160,00, ganho por hora R$ 26,67 e ganho por km R$ 2,00
51. **Por dia da semana:** são 7 linhas, de segunda a domingo, cada uma com barra, "R$ X/h · N dias · média R$ Y/dia".
    Dia sem lançamento mostra "Sem lançamentos"; dia com ganho mas sem horas mostra "sem horas"
52. **Melhor dia:** com horas informadas em pelo menos 2 dias da semana diferentes, aparece "Melhor dia por hora: ...".
    Com horas em um dia só, a frase não aparece
53. **Por plataforma:** um card para cada plataforma que tem ganho (Uber, 99...), com ganho bruto, lançamentos, média por
    lançamento, ganho por hora e por km, do que mais rendeu para o que menos rendeu. Plataforma sem ganho não aparece
54. **Horas não inflam o valor:** lançar um ganho Uber de R$ 50,00 sem horas e sem km. O ganho bruto da Uber sobe,
    mas o ganho por hora e por km dela continuam iguais. Excluir esse ganho de teste depois
55. **Período:** um ganho de 40 dias atrás entra em "90 dias" e "Tudo", mas não em "30 dias". Os números mudam ao trocar o chip
56. **Atualiza sozinha:** lançar um ganho novo (botão +) e voltar para a aba Análises: os números já incluem o lançamento
57. **Exportar:** Ajustes > "Exportar backup". O seletor do sistema abre com o nome sugerido
    `motorista-financeiro-backup-AAAA-MM-DD.zip`. Salvar em uma pasta e conferir a mensagem em verde
    ("Backup salvo: N ganhos, N despesas... e N fotos.")
58. **Conteúdo do arquivo:** copiar o .zip para o computador e abrir: tem o `dados.json` (texto legível) e a pasta
    `comprovantes` com as fotos das despesas
59. **Restaurar no mesmo aparelho:** lançar um ganho de teste, ir em "Restaurar backup", confirmar o aviso e escolher o .zip.
    O ganho de teste some, os dados do backup voltam e o Início confere. A depreciação em Ajustes mostra o valor do backup
60. **Cancelar:** em "Restaurar backup", tocar em Cancelar no aviso, ou abrir o seletor e voltar sem escolher arquivo.
    Nada muda e nenhuma mensagem de erro aparece
61. **Arquivo errado:** tentar restaurar um arquivo que não é backup (uma foto, ou outro .zip). Aparece uma mensagem em
    vermelho terminando em "Nada foi alterado." e os dados continuam iguais
62. **Fotos:** despesas com comprovante continuam com a foto depois de restaurar, e a foto abre ampliada
63. **Vínculos:** depois de restaurar, editar um abastecimento e conferir que a despesa de combustível dele acompanha;
    excluir esse abastecimento remove a despesa dele
64. **Lançar depois de restaurar:** criar um ganho e uma despesa novos funciona normalmente e aparece no Histórico
65. **Reinstalar (o teste que importa):** só depois de copiar o .zip para FORA do celular (computador ou Drive).
    Desinstalar o app, instalar de novo (`.\gradlew installDebug`), abrir Ajustes > "Restaurar backup" e escolher o .zip.
    Lançamentos, veículo, reservas, metas, depreciação e fotos voltam. (`installDebug` por cima do app instalado NÃO apaga os
    dados; só desinstalar apaga)
66. **Aviso já vem ligado:** na primeira abertura (app recém-instalado) no Android 13 ou mais novo aparece a pergunta de
    permissão de notificação; permitir. Ajustes mostra "Avisar sobre manutenção" com a chave ligada e sem aviso em vermelho
67. **Desligar e ligar:** desligar a chave e ligar de novo. A chave fica ligada e a pergunta de permissão não volta
68. **Notificação próxima:** com a troca de óleo do passo 11 e a chave ligada, registrar um abastecimento de teste com
    odômetro 19.700. Chega a notificação "Manutenção próxima: Troca de óleo — Faltam 300 km" (pode levar alguns segundos;
    puxe a barra de notificações). Se não chegar no Xiaomi: Configurações > Apps > DriverFlow > Notificações
69. **Não repete:** fechar e abrir o app e editar qualquer lançamento. Nenhuma notificação igual chega de novo
70. **Atrasada:** registrar um abastecimento de teste com odômetro 20.200. A notificação é substituída por
    "Manutenção atrasada: Troca de óleo — Atrasada em 200 km" (continua uma só na barra)
71. **Tocar:** com o app fechado, tocar na notificação abre direto a aba Veículo e ela some da barra. Repetir com o app aberto
    em outra aba: também vai para Veículo. Girar a tela depois não reabre a aba
72. **Resolvida:** registrar uma nova troca de óleo no odômetro 20.200. A notificação (se ainda estiver na barra) some e o
    alerta fica em dia. Excluir os abastecimentos de teste depois
73. **Desligar:** com um alerta próximo ou atrasado de novo, desligar a chave. A notificação some e nada novo chega
74. **Permissão negada:** negar a pergunta de permissão (ou bloquear as notificações do app nas configurações do Android)
    com a chave ligada. Aparece o aviso em vermelho com o botão "Abrir configurações de notificação", que abre a tela certa do Android.
    Liberar e voltar ao app: o aviso some. Ligar a chave de novo e conferir que a notificação chega
75. **Backup guarda a chave:** desligar a chave, Exportar backup, ligar a chave e Restaurar o backup. A chave volta desligada
    e, se havia alerta próximo ou atrasado, a notificação chega de novo uma vez (a lista de alertas já avisados é zerada)
76. **Tema escuro (padrão):** todas as telas (Início, Histórico, Veículo, Reservas, Metas, Análises, Ajustes e os formulários) em azul-marinho com
    texto claro, botões azuis, ganhos em verde e despesas em vermelho. A barra de status e a de navegação do Android ficam na cor do
    fundo, com ícones claros. Conferir também com o celular em modo claro e com outro papel de parede: o app não muda de cor.
    Ao abrir, a passagem da animação para o Início não pisca de branco. Anotar qualquer texto difícil de ler
77. **Aparência:** Ajustes > Aparência mostra três opções (Automático, Claro, Escuro) com Escuro marcada. Escolher Claro: o app
    inteiro muda na hora (fundo claro, texto escuro, ganhos em verde-escuro, despesas em vermelho-escuro, barra de status com
    ícones escuros). Escolher Escuro: volta. Escolher Automático e trocar o modo escuro do celular com o app aberto: o app acompanha
78. **Aparência lembrada:** com Claro escolhido, fechar o app e abrir de novo: a animação continua azul-marinho e, depois dela,
    o app aparece claro. Passar por todas as telas no modo Claro e anotar qualquer texto difícil de ler
79. **Teclado no modo Claro:** abrir um formulário e o teclado: não aparece faixa escura atrás dele
80. **Fonte e números:** todas as telas em Manrope (letras arredondadas, sem serifa). No Histórico, os valores da direita
    ficam alinhados e não "pulam" de largura entre linhas
81. **Barra inferior:** quatro abas (Início, Histórico, Veículo, Análises) e o + azul no centro, um pouco acima da barra.
    A aba atual fica azul. Não há mais aba Reservas nem botão flutuante sobre o conteúdo. Nos formulários e em Reservas,
    Meta e Ajustes a barra some
82. **Início:** a data de hoje aparece acima de "Início". O seletor Hoje/Semana/Mês troca o cartão do lucro e os quatro
    indicadores (com o roteiro do passo 1: Hoje mostra R$ 70,00, R$ 2,00/km, R$ 25,00/h, R$ 0,60/km e "50 km · 4 h").
    A barra verde e vermelha do cartão mostra quanto do bruto virou lucro e quanto foi despesa. Girar a tela mantém o período
83. **Aviso de manutenção no Início:** com uma manutenção atrasada ou próxima, aparece o aviso âmbar "N manutenção(ões)
    pede(m) atenção" com as duas mais urgentes; tocar abre a aba Veículo. Sem alertas, o aviso não aparece
84. **Atalho de Reservas:** sem reservas, o atalho convida a cadastrar; com reservas, mostra "Reservas: separe R$ X hoje" e a
    próxima a vencer. Tocar abre Reservas, com o botão voltar no topo
85. **Novo lançamento:** o + abre com Ganho escolhido (verde). O valor grande começa com "R$ 0,00" apagado; digitar 248,90.
    Escolher a plataforma nos chips, conferir os atalhos Hoje/Ontem da data e preencher km, horas e gorjeta lado a lado.
    O botão "Salvar ganho" fica fixo embaixo e só habilita com valor. Trocar para Despesa: tudo fica vermelho, as
    categorias viram chips e aparece o cartão do comprovante (Tirar foto e Galeria). O X do topo fecha sem salvar
86. **Editar lançamento:** no Histórico, tocar num ganho abre "Editar ganho" com o valor preenchido, "Salvar alterações" no
    rodapé e "Excluir lançamento" (em vermelho) no fim dos campos
87. **Histórico:** troca de mês num cartão no topo, três totais do mês (ganhos em verde, despesas em vermelho, líquido em azul),
    filtros em chips e um cartão por dia, com o saldo do dia ao lado da data. Cada linha tem um quadradinho com a sigla
    (U para Uber, 99, CB para combustível...)
88. **Veículo:** odômetro grande no topo com consumo, último litro e custo/km. Os botões Abastecer e Manutenção abrem os
    formulários direto (o + da barra continua perguntando). Com a troca de óleo do passo 11, o cartão da manutenção mostra
    o selo (Em dia / Em N km / Atrasada) na cor certa e a barra enchendo conforme o odômetro avança; tocar abre a manutenção
89. **Análises:** seletor 30 dias / 90 dias / Tudo, seis indicadores, gráfico de barras de segunda a domingo com o melhor
    dia em azul e o valor por hora em cima, a lista com dias e média de cada dia, e o cartão Por plataforma com a barra dividida
    e o percentual de cada uma (somando 100%)
90. **Modo Claro no novo visual:** repetir os passos 81 a 89 com Ajustes > Aparência > Claro e anotar qualquer texto difícil de ler
91. **Formulários secundários:** Abastecer, Manutenção (no Veículo), Nova reserva (Reservas) e Meta abrem com o valor grande no
    topo, botões de escolha, campos em cartões e o botão de salvar fixo no rodapé (X no topo para cadastro, seta para edição).
    Excluir/Remover aparecem em vermelho no fim e pedem confirmação. No Abastecimento, o preço por litro aparece embaixo do valor
92. **Ajustes:** seções Depreciação, Aparência (seletor Automático/Claro/Escuro), Notificações (chave) e Backup em cartões.
    Salvar depreciação volta ao Início; Exportar/Restaurar continuam funcionando (Restaurar pede confirmação)
93. **Odômetro estimado:** com um abastecimento registrado hoje no odômetro X, lançar um ganho com 120 km com data de amanhã
    (ou esperar o dia seguinte). No Veículo aparece "Odômetro estimado" com X + 120 e "Última leitura: X km em dd/MM + 120 km
    dos ganhos". Um ganho com data de hoje não muda o odômetro. Registrar um novo abastecimento faz voltar a "Odômetro atual"
94. **Alerta andando sozinho:** com a troca de óleo do passo 11 (intervalo 10.000), lançar ganhos com data depois da última
    leitura até faltar menos de 500 km: o aviso âmbar aparece no Início, o cartão da manutenção muda para "Em N km" e, com a chave
    ligada, chega a notificação (sem registrar abastecimento). Excluir esses ganhos de teste depois
95. **Repetir meta:** com meta no mês passado e nenhuma neste mês, o card da meta no Início mostra "Repetir a de <mês>: R$ X"
    (grava na hora e o progresso aparece) e "Definir outro valor". Na tela da meta, sem meta, o botão de atalho preenche o campo
96. **Início e fim do turno:** no Novo lançamento (Ganho), não há mais campo de horas. Tocar em "Início do turno" abre um
    relógio de 24 h; escolher 17:00 e, em "Fim do turno", 20:00: aparece "3 h de turno". Testar também 22:00 às 02:00 (4 h).
    Só o início: aviso de que fica sem horas, mas salva. Só o fim, ou fim igual ao início: aviso em vermelho e Salvar desabilitado.
    No Histórico a linha mostra "3 h · desde 17:00". Editar o ganho: início e fim vêm preenchidos; "Limpar" no relógio tira o
    horário. Um ganho antigo (com horas e sem horário) mostra "N h online (lançado sem horário)" e mantém as horas ao salvar
97. **Faixas nas Análises:** com o ganho do passo 96 (R$ 90,00, das 17:00 às 20:00), o cartão "Ganho por hora por horário" mostra
    R$ 30,00/h na Tarde e na Noite (1 turno em cada). Sem nenhum ganho com horário, o cartão explica como preencher; com
    lançamentos sem horário, avisa quantos ficaram de fora
98. **Atualização sem perder dados:** instalar esta versão por cima da anterior (banco versão 1): todos os lançamentos, metas,
    reservas e fotos continuam lá, e os ganhos antigos aparecem sem horário
99. **Backup v2:** Exportar backup e Restaurar o mesmo arquivo: os horários de início voltam. Um backup antigo (feito antes desta
    versão) restaura normalmente, com os ganhos sem horário
