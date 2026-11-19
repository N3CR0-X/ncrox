# NCROX Android: comandos e limites

O aplicativo usa `/system/bin/sh` do Android, **não** uma distribuição Termux, Debian ou Parrot. `help` lista executáveis encontrados no PATH do aparelho; confira recursos com `command -v nome`. Exemplos que dependem do aparelho: `ls -la`, `mkdir -p pasta`, `cat arquivo`, `cp`, `mv`, `grep`, `find`, `ps` e `df`. `ls` e `mkdir` vão ao shell do sistema, preservando suas opções quando suportadas. O comando integrado `make` continua criando diretórios; não é GNU Make.

O shell não é interativo: stdin é fechado, não há PTY, entrada durante a execução, nem suporte adequado a editores interativos, senhas ou Ctrl+C. É um processo novo por comando; `cd` integrado mantém o diretório da aba, mas variáveis e outras alterações de ambiente não persistem. A maioria dos comandos integrados usa separação simples por espaços; em especial, `cd` e `tree` não interpretam aspas como um shell. As ferramentas `vigia`, `oraculo`, `bastiao` e `selo` têm parser próprio que aceita aspas em argumentos. Uma linha cujo primeiro comando é integrado não é uma sequência shell geral; por exemplo, use `sh -c 'cd / && pwd'` quando disponível. Para comandos externos executados pelo shell, há limite de 30 segundos, e até 64 KiB da saída combinada stdout/stderr são exibidos; o restante é descartado, mas drenado para evitar bloqueio. As ferramentas internas têm limites próprios e o DNS pode demorar mais. A truncagem pode cortar um caractere UTF-8. O limite encerra o processo principal; filhos em segundo plano podem sobreviver e manter descritores abertos. Não use para tarefas longas ou não confiáveis. Comandos são executados com as permissões do aplicativo e podem alterar ou apagar arquivos acessíveis.

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
