# Fluxo de trabalho

Como este projeto é desenvolvido, em parceria entre **você** (dono do projeto) e o **Claude**.
Vale para todo pedido de alteração, grande ou pequeno.

## Quem faz o quê

| Quem | O que faz |
|---|---|
| Você | Pede a alteração e testa o app pelo Android Studio (emulador ou celular) |
| Claude | Implementa a alteração com testes, roda a build e os testes, atualiza a documentação, faz o commit e o push |

Você e o Claude usam **a mesma pasta**: `D:\DriverFlow`. O Claude Code é aberto nela, e o Android Studio
também. Não há `git pull` nem comandos de build do seu lado.

O Claude tem sua autorização para fazer commit e push direto no repositório.

## Passo a passo de cada alteração

1. **Pedido.** Você descreve o que quer (ex.: "tela de meta mensal").
2. **Claude confere o estado.** `git status` e `git log` em `D:\DriverFlow`. Se houver mudança sua
   não commitada, ele avisa antes de mexer.
3. **Claude implementa.** Código, testes de regressão (ver [`TESTES.md`](TESTES.md)) e documentação
   (`ROADMAP.md`, `TESTES.md`, `ESPECIFICACAO.md`, quando mudam).
4. **Claude roda a build e os testes** (`gradlew test assembleDebug`, comandos abaixo). Falhas e erros
   devem ser 0; se não forem, ele corrige antes de seguir.
5. **Claude faz o commit e o push** e informa o total de testes e o roteiro de teste manual.
6. **Você testa pelo Android Studio:** Run (▶) no emulador ou no celular, seguindo o roteiro manual
   de [`TESTES.md`](TESTES.md). Se o Android Studio pedir, faça o Gradle Sync antes.
7. **Se algo der errado**, descreva o que viu (ou cole o erro do Android Studio). O Claude corrige num
   novo commit.

## Comandos de build (usados pelo Claude)

```powershell
cd D:\DriverFlow
$env:JAVA_HOME = "C:\devtools\jdk17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = "D:\gradle-home"
.\gradlew test assembleDebug
Get-ChildItem app\build\test-results\testDebugUnitTest\*.xml | ForEach-Object { ([xml](Get-Content $_)).testsuite | Select-Object name, tests, failures, errors }
```

O que cada bloco faz:
- `$env:...`: usam o JDK 17 e o cache do Gradle só nesta janela do PowerShell
- `gradlew test assembleDebug`: roda os testes de unidade e gera o APK de debug
- `Get-ChildItem ...`: lista quantos testes rodaram em cada classe, com falhas e erros

Alteração que só muda documentação não precisa de build.

## Mensagens de commit

Prefixos das mensagens (sem acento, para não dar problema no terminal):

| Prefixo | Para |
|---|---|
| `feat:` | funcionalidade nova |
| `fix:` | correção de bug |
| `test:` | só testes |
| `docs:` | só documentação |
| `build:` | Gradle e dependências |
| `chore:` | manutenção sem efeito no app |

## Regras do projeto

- **Testes de regressão para tudo** que for implementado e validado (política em [`TESTES.md`](TESTES.md)).
- **Nada vai para o GitHub sem build e testes passando.** O teste manual pelo Android Studio é a
  verificação final do comportamento na tela.
- **Um commit por pedido**, com os arquivos de código, testes e documentação juntos.
- **Arquivos do Gradle** (`build.gradle.kts`, `libs.versions.toml`) só mudam quando a alteração exige,
  e o Claude avisa de forma explícita.
- O commit leva só arquivos do projeto (`app/src/...`, `docs/...`), nunca `build/`, `.gradle/`,
  `local.properties` ou `.apk`.
- **A documentação acompanha o código**: `ROADMAP.md`, `TESTES.md` (roteiro manual) e `ESPECIFICACAO.md`.

## Ambiente local (lembretes)

- Projeto em pasta **sem acento e sem espaço**: `D:\DriverFlow`. O Android Gradle Plugin falha com acento
  (por isso o projeto não pode ficar em `C:\Users\João Marcos da Silva\...`).
- **JDK 17 só na sessão do PowerShell.** O JDK padrão da máquina é o 25, que não funciona com este projeto.
- `GRADLE_USER_HOME` em `D:\gradle-home` (o caminho padrão tem acento e espaço).
- `local.properties` com `sdk.dir=D:/android-sdk`. O arquivo é ignorado pelo Git.
- **Celular:** depuração USB ligada; em Xiaomi, também "Instalar via USB".
- **Nunca rode `connectedAndroidTest` com o celular que tem seus dados reais.** O Gradle desinstala o app
  no fim e apaga os lançamentos e as fotos. Prefira o emulador para testes instrumentados.
- Run do Android Studio (ou `installDebug`) por cima do app instalado **mantém os dados**. Desinstalar
  o app apaga tudo (lançamentos, configurações e fotos).

## Problemas conhecidos e soluções

| Sintoma | Causa e solução |
|---|---|
| Android Studio não lista o celular | Rode `D:\android-sdk\platform-tools\adb.exe devices`: lista vazia = cabo, porta ou modo USB; `unauthorized` = aceite a autorização na tela do celular; `offline` = `adb kill-server` e `adb start-server` |
| `INSTALL_FAILED_USER_RESTRICTED` | O celular bloqueou a instalação via USB. Ligue "Instalar via USB" em Opções do desenvolvedor e deixe a tela desbloqueada |
| Android Studio mostra código antigo ou erros que somem no build | Faça File > Sync Project with Gradle Files |
| `Could not find org.robolectric:robolectric:...` | Versão errada na dependência. A correta é `org.robolectric:robolectric:4.13` |
| `project path contains non-ASCII characters` | O projeto está numa pasta com acento. Use `D:\DriverFlow` |
| `ClassNotFoundException` em todos os testes | Caminho com acento (projeto ou `GRADLE_USER_HOME`). Use `D:\DriverFlow` e `D:\gradle-home` |
| Build com JDK 25 falha | Ative o JDK 17 na sessão (comandos acima) |

## Para o Claude: checklist ao receber um pedido

1. Conferir `git status`, `git log` e o estado de `app/src` (e dos `docs/`) em `D:\DriverFlow`
2. Implementar partindo do código real, nunca de memória
3. Escrever os testes junto e atualizar o roteiro manual e o roadmap
4. Rodar `gradlew test assembleDebug` e conferir 0 falhas e 0 erros
5. Fazer o commit (prefixos acima) e o push
6. Entregar o total de testes e o roteiro de teste manual pelo Android Studio
