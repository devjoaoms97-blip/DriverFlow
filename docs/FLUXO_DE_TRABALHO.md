# Fluxo de trabalho

Como este projeto é desenvolvido, em parceria entre **você** (dono do projeto) e o **Claude**.
Vale para todo pedido de alteração, grande ou pequeno.

## Quem faz o quê

| Quem | O que faz |
|---|---|
| Você | Pede a alteração, baixa a versão nova (`git pull`), roda a build e testa no celular |
| Claude | Trabalha num clone do repositório, implementa a alteração com testes, atualiza a documentação, faz o commit e o push |

O Claude tem sua autorização para fazer commit e push direto no repositório.

## Passo a passo de cada alteração

1. **Pedido.** Você descreve o que quer (ex.: "tela de meta mensal").
2. **Claude atualiza o clone.** Ele parte do último commit de `https://github.com/devjoaoms97-blip/DriverFlow`,
   então a alteração é feita sobre o código real.
   - Se você mudou algo direto na sua pasta, **faça o commit e o push antes de pedir a próxima alteração**,
     ou avise. O que não foi enviado ao GitHub o Claude não enxerga.
3. **Claude implementa.** Código, testes de regressão (ver [`TESTES.md`](TESTES.md)) e documentação
   (`ROADMAP.md`, `TESTES.md`, `ESPECIFICACAO.md`, quando mudam).
4. **Claude faz o commit e o push** e informa o total de testes esperado.
5. **Você baixa a versão nova e roda a build** (comandos abaixo).
6. **Você confere os testes.** O número esperado vem na resposta do Claude; falhas e erros devem ser 0.
7. **Você testa no celular**, seguindo o roteiro manual de [`TESTES.md`](TESTES.md).
8. **Se algo der erro**, cole aqui o trecho `What went wrong` da build ou o nome do teste que falhou.
   O Claude corrige num novo commit.

## Comandos para baixar a versão nova e rodar a build

```powershell
cd D:\MotoristaFinanceiro
git pull
$env:JAVA_HOME = "C:\devtools\jdk17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME = "D:\gradle-home"
.\gradlew test installDebug
Get-ChildItem app\build\test-results\testDebugUnitTest\*.xml | ForEach-Object { ([xml](Get-Content $_)).testsuite | Select-Object name, tests, failures, errors }
```

O que cada bloco faz:
- `git pull`: baixa os commits que o Claude enviou
- `$env:...`: usam o JDK 17 e o cache do Gradle só nesta janela do PowerShell
- `gradlew test installDebug`: roda os testes e instala o app no celular conectado
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
- **O Claude não compila nem roda o app.** O ambiente dele não tem Android SDK. A sua build local, com os
  testes e o teste no celular, é a verificação de verdade. Por isso um erro de compilação no primeiro build
  de uma alteração é possível, e a correção vem rápido se você colar a mensagem.
- **Um commit por pedido**, com os arquivos de código, testes e documentação juntos.
- **Arquivos do Gradle** (`build.gradle.kts`, `libs.versions.toml`) só mudam quando a alteração exige,
  e o Claude avisa de forma explícita.
- O commit leva só arquivos do projeto (`app/src/...`, `docs/...`), nunca `build/`, `.gradle/`,
  `local.properties` ou `.apk`.
- **A documentação acompanha o código**: `ROADMAP.md`, `TESTES.md` (roteiro manual) e `ESPECIFICACAO.md`.

## Ambiente local (lembretes)

- Projeto em pasta **sem acento e sem espaço**: `D:\MotoristaFinanceiro` (o Android Gradle Plugin falha com acento).
- **JDK 17 só na sessão do PowerShell.** O JDK padrão da máquina é o 25, que não funciona com este projeto.
- `GRADLE_USER_HOME` em `D:\gradle-home` (o caminho padrão tem acento e espaço).
- `local.properties` com `sdk.dir=D:/android-sdk`. O arquivo é ignorado pelo Git.
- **Celular:** depuração USB ligada; em Xiaomi, também "Instalar via USB".
- **Nunca rode `connectedAndroidTest` com o celular que tem seus dados reais.** O Gradle desinstala o app
  no fim e apaga os lançamentos e as fotos.
- `installDebug` por cima do app instalado **mantém os dados**. Desinstalar o app apaga tudo
  (lançamentos, configurações e fotos).

## Problemas conhecidos e soluções

| Sintoma | Causa e solução |
|---|---|
| `No connected devices!` na instalação | O computador não enxerga o celular. Rode `D:\android-sdk\platform-tools\adb.exe devices`: lista vazia = cabo, porta ou modo USB; `unauthorized` = aceite a autorização na tela do celular; `offline` = `adb kill-server` e `adb start-server`. Os testes já passaram nesse caso, então só repita `.\gradlew installDebug` |
| `INSTALL_FAILED_USER_RESTRICTED` | O celular bloqueou a instalação via USB. Ligue "Instalar via USB" em Opções do desenvolvedor e deixe a tela desbloqueada |
| `Could not find org.robolectric:robolectric:...` | Versão errada na dependência. A correta é `org.robolectric:robolectric:4.13` |
| `fatal: not a git repository` | A pasta era uma cópia sem o `.git`. Use `git init`, `git remote add origin ...`, `git fetch origin` e `git reset origin/main` |
| `git pull` recusa por mudanças locais | Há arquivos alterados na sua pasta. Rode `git status`; descarte com `git restore .` ou faça o commit e o push antes |
| `project path contains non-ASCII characters` | O projeto está numa pasta com acento. Mova para um caminho simples |
| `ClassNotFoundException: GradleWorkerMain` | `GRADLE_USER_HOME` com espaço ou acento. Use `D:\gradle-home` |
| Build com JDK 25 falha | Ative o JDK 17 na sessão (comandos acima) |

## Para o Claude: checklist ao receber um pedido

1. Atualizar o clone (`git pull`) e conferir `git log` e o estado de `app/src` (e dos `docs/`)
2. Implementar partindo do código real, nunca de memória
3. Escrever os testes junto e atualizar o roteiro manual e o roadmap
4. Contar o total de testes esperado (testes existentes + novos)
5. Fazer o commit (prefixos acima) e o push
6. Entregar o total de testes esperado e o roteiro de teste no celular
