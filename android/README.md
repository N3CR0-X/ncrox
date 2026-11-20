# NCROX Android: comandos e limites

O aplicativo usa GNU Bash 5.3 embutido no APK para executar comandos externos quando disponível para a ABI do aparelho; não é uma distribuição Termux, Debian ou Parrot. `help` mostra os comandos do Satanachia; `command -v nome` verifica se um executável já existe no PATH do app. Executar `pack search <termo>`, `pack show <pacote>` ou `pack list-all` consulta o índice oficial Termux para a arquitetura atual, sem instalar nada. `pack install <pacote>` não instala dentro do APK: abra o Termux e execute `pkg install <pacote>` lá. Exemplos que dependem do aparelho: `ls -la`, `mkdir -p pasta`, `cat arquivo`, `cp`, `mv`, `grep`, `find`, `ps` e `df`. `ls` e `mkdir` vão ao shell do sistema, preservando suas opções quando suportadas. O comando integrado `make` continua criando diretórios; não é GNU Make.

O shell não é interativo: stdin é fechado, não há PTY, entrada durante a execução, nem suporte adequado a editores interativos, senhas ou Ctrl+C. Para mexer num arquivo de texto pelo próprio app, `nano <arquivo>` abre um editor simples integrado (não o programa GNU nano), limitado a 1 MiB. É um processo novo por comando; `cd` integrado mantém o diretório da aba, mas variáveis e outras alterações de ambiente não persistem. A maioria dos comandos integrados usa separação simples por espaços; em especial, `cd` e `tree` não interpretam aspas como um shell. As ferramentas `vigia`, `oraculo`, `bastiao` e `selo` têm parser próprio que aceita aspas em argumentos. Uma linha cujo primeiro comando é integrado não é uma sequência shell geral; por exemplo, use `bash -c 'pwd'` para executar uma expressão do Bash. Para comandos externos executados pelo shell, há limite de 30 segundos, e até 64 KiB da saída combinada stdout/stderr são exibidos; o restante é descartado, mas drenado para evitar bloqueio. As ferramentas internas têm limites próprios e o DNS pode demorar mais. A truncagem pode cortar um caractere UTF-8. Se a pasta de trabalho tiver sido removida, o executor procura a pasta pai existente mais próxima antes de iniciar Bash. O limite encerra o processo principal; filhos em segundo plano podem sobreviver e manter descritores abertos. Não use para tarefas longas ou não confiáveis. Comandos são executados com as permissões do aplicativo e podem alterar ou apagar arquivos acessíveis.

## GitHub

- `github repo N3CR0-X/ncrox`: metadados de um repositório público.
- `github user N3CR0-X`: perfil público.
- `github releases N3CR0-X/ncrox`: até 10 releases.
- `github issues N3CR0-X/ncrox`: issues abertas da primeira página de 10 resultados; pull requests dessa página são omitidos.
- `github search termos de busca`: até 10 repositórios públicos.

Consultas acessam a API pública do GitHub por HTTPS, sem token, e estão sujeitas a rede, limites de taxa, paginação, acesso público e disponibilidade do serviço. Não baixam nem executam código. A loja já existente tem seus próprios comandos de download/extração sob pedido; uma extração não executa os arquivos. `pick dono/repo` solicita explicitamente um `git clone`: só funciona se um executável `git` utilizável estiver instalado no PATH e pode falhar pela rede, pelo tempo limite, por autenticação ou por permissão. O APK não instala Git nem pacotes, não tem `apt` garantido e não eleva privilégios.

A permissão de câmera é solicitada para o flash e a de localização para `ondeuto -info`; elas dependem da concessão pelo usuário, hardware, configurações e política do Android. A localização por IP envia uma consulta ao serviço de terceiros indicado na interface. A permissão ampla para arquivos compartilhados é opcional e exige aprovação especial; mesmo concedida, não remove restrições do Android, como acesso a dados privados de outros apps. A visibilidade de apps e processos continua limitada. Nem opções de desenvolvedor nem esses comandos concedem root.

## Testes locais do executor

Em um host Unix com JDK 8+ e ferramentas básicas `/bin/sh`, `yes`, `head`, `tr` e `sleep`:

```sh
mkdir -p /tmp/ncrox-test
javac -d /tmp/ncrox-test app/src/main/java/com/ncrox/satanachia/ShellRunner.java tests/com/ncrox/satanachia/ShellRunnerTest.java
java -cp /tmp/ncrox-test com.ncrox.satanachia.ShellRunnerTest
```

Rode a partir de `android/`. Esses testes verificam o executor Java no host; não substituem build, instalação ou testes em Android. Para compilar o APK é necessário um Gradle compatível com Android Gradle Plugin 8.7.3, JDK adequado, Android SDK 35 e acesso às dependências Gradle; este repositório não inclui Gradle wrapper. Testar em dispositivo/emulador câmera, localização, armazenamento, shell Android e API GitHub, além de considerar target SDK e requisitos atuais de publicação antes de distribuir.


## Bash próprio no APK

O APK compila e embute GNU Bash 5.3 para arm64-v8a, armeabi-v7a, x86_64 e x86 (GPLv3-or-later; veja `android/third_party/bash/NOTICE.md`), usando o Android NDK e o fonte oficial do GNU com patches 5.3 verificados por SHA-256. Os comandos comuns do terminal são executados por esse Bash quando há binário para a ABI do aparelho; a implementação Android de `cd`, `make`, `tree`, `pick` e outros comandos nativos do ncrox continua sendo própria e não é substituída por Bash.

Esse Bash é não interativo e roda cada comando com os limites existentes de 30 s e 64 KiB de saída. Ele não inclui um ambiente Linux, `apt`, `pkg`, Termux, root nem ferramentas extras: só interpreta comandos e scripts dentro do sandbox/permissões do Satanachia.

Para reproduzir a compilação: instale o Android NDK 27.2.12479018, defina `ANDROID_NDK_HOME` e execute `bash android/build-bash.sh`; depois faça `gradle --no-daemon assembleDebug` na pasta `android`. A primeira geração dos executáveis é validada pelo workflow Android; teste de instalação e execução em aparelho real ainda é necessário.


## `pack` e comandos que não vêm no Android

`pack` é um navegador simples do índice do repositório principal oficial do Termux, carregado ao vivo do endpoint `packages.termux.dev`. Ele implementa `search`, `show`/`info` e uma listagem limitada (100 itens) para consultar nomes, versões e descrições; não é idêntico ao `pkg` e não faz instalação, upgrade, remoção, leitura do estado instalado, gestão de dependências nem verificação de assinaturas como o apt/dpkg do Termux. Os outros argumentos de gerenciamento explicam que precisam ser executados no próprio Termux. Consulte a documentação oficial: `https://wiki.termux.com/wiki/Package_Management`.

O APK não tem o ambiente e os repositórios do Termux, e não consegue pôr aplicativos extraídos de ZIP no PATH do seu sandbox. Para editar texto há o comando `nano <arquivo>`, um editor simples construído na interface do Satanachia (não GNU nano), limitado a arquivos de 1 MiB. O executor volta para uma pasta pai existente se a pasta de trabalho atual tiver sido removida durante extração/renomeação; isso evita a mensagem Bash de diretório corrente apagado. Comandos não encontrados agora recebem uma explicação sobre o ZIP/Termux.
