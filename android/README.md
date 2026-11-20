# Satanachia ncrox — Android

O APK inclui GNU Bash 5.3 para `arm64-v8a`, `armeabi-v7a`, `x86_64` e `x86`. O shell padrão do terminal agora mantém **uma sessão Bash por aba** para executar comandos externos: `export`, funções e `cd` feitos no shell persistem naquela aba. As abas têm sessões e diretórios independentes; `start` abre uma aba, e `exit`, `logout` ou `quit` fecha o app, encerra as sessões, tenta terminar os processos filhos e para o serviço do terminal.

## O que o shell é — e o que não é

- É uma sessão Bash local, dentro do sandbox e das permissões do app, com `$HOME` privado em `files/shell-home` e `$HOME/usr/bin` no PATH junto com os diretórios de sistema Android.
- Não é Termux nem uma distribuição Linux, não concede root e não herda os binários do prefixo privado do Termux. Um ZIP da Store baixado ou extraído não instala seus comandos no Satanachia.
- A sessão é persistente, mas não tem PTY nem entrada interativa de terminal. Comandos de texto e scripts funcionam melhor; editores como `vim`, `top`, prompts de senha e programas que aguardam interação podem não funcionar.
- Cada comando externo tem limite de 30 s e a saída combinada é limitada a 64 KiB. O shell e seu estado são recriados se ele terminar, for fechado ou atingir o timeout. `bash -c` e `bash <arquivo>` são execuções separadas; não alteram as variáveis da sessão Bash padrão.
- Se o diretório atual tiver sido removido ou ficar inacessível, o executor volta à pasta-pai acessível mais próxima. O acesso a `/storage/emulated/0` depende da permissão de arquivos do Android.
- `nano <arquivo>` abre o editor simples integrado do app (até 1 MiB); não é o GNU nano instalado por pacote.

## `pack` e `pkg`

`pack search`, `pack show` e `pack list-all` consultam, sem instalar, o catálogo oficial de pacotes Termux. O novo `pkg` encaminha um conjunto limitado de ações ao Termux instalado usando a interface oficial `com.termux.RUN_COMMAND`. A lista inclui `search`, `install`, `show`, `list-installed`, `list-all`, `update`, `upgrade`, `full-upgrade`, `reinstall`, `uninstall`, `remove`, `autoremove`, `clean`, `files`, `help` e `version`.

O encaminhamento é intencionalmente separado do shell do APK: o Termux executa a ação e guarda os pacotes no próprio prefixo privado. Não instala binários no Satanachia. É necessário instalar e inicializar o Termux oficial, permitir ao Satanachia a permissão Android `RUN_COMMAND` e ativar `allow-external-apps=true` em `~/.termux/termux.properties` do Termux. Confirmações e resultados aparecem numa sessão do Termux. O Android pode impor restrições a iniciar telas a partir do Satanachia em segundo plano; se isso ocorrer, traga o Termux para frente. O encaminhador recusa flags/argumentos começando com hífen, pipes e comandos compostos; não envia `-y` nem confirma compras/alterações automaticamente. Se a integração não estiver disponível, rode `pkg` diretamente no Termux.

Referência: [RUN_COMMAND Intent — documentação oficial do Termux](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent).

## Build e testes automatizados

O workflow do GitHub compila Bash para as quatro ABIs, testa `ShellRunner` e `ShellSession`, monta o APK debug e verifica os binários Bash dentro dele. Para build local, use JDK, Android SDK 35, Android NDK `27.2.12479018` e o Android Gradle Plugin 8.7.3; execute `bash android/build-bash.sh` com `ANDROID_NDK_HOME` configurado e então `gradle --no-daemon assembleDebug` a partir desta pasta.

Os testes `ShellSessionTest` verificam persistência de variáveis e diretório, saída/status, timeout e recuperação da sessão no host Linux. Isso não simula Android, PTY, permissões nem o Termux real. O encaminhamento `pkg` e o funcionamento no telefone ainda precisam ser verificados com Termux instalado e a permissão concedida; compilação bem-sucedida, sozinha, não substitui esse teste em aparelho.

## Limites Android

O Android mantém os dados privados de cada app isolados. Bash no APK não pode instalar dentro do prefixo do Termux, matar processos de outros apps ou obter root. `taskkill -sudozi` abre a página de informações do app para a pessoa decidir se força a parada; `tasklist` mostra apenas o que a API disponibiliza. `vigia`, `oraculo`, `bastiao` e `selo` são verificações pontuais: use conectividade de rede apenas em sistemas próprios ou autorizados. `ondeuto -ip` consulta ipapi.co e transmite o IP público para estimativa aproximada; `ondeuto -info` requer permissão de localização.