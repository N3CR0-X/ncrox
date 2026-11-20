# Satanachia Store

A Store tem pacotes ZIP individuais para ferramentas gerais de desenvolvimento, terminal, arquivos, sincronização, rede básica e bibliotecas Python. Esses ZIPs contêm instaladores para Termux/Linux, não binários prontos para o shell do APK. Veja [catálogo completo (100 pacotes)](catalogo.md).

No Satanachia: `satanachia -store`; depois `satanachia -pick <nome>` e `satanachia -st <nome>`. Leia `LEIA-ME.md` dentro da pasta extraída. Os `install.sh` desses pacotes são para Termux, Debian/Parrot ou Arch — execute-os dentro desse ambiente, após revisar o script. **Não execute o instalador no Bash embutido do APK:** o Android não tem `pkg`, `apt` ou `pacman`, e extrair ZIP não instala executáveis no Satanachia.

Termux usa `pkg`; Debian/Parrot usa `apt`; Arch usa `pacman`. A disponibilidade depende de release, arquitetura e repositórios configurados. Bibliotecas Python usam pip dentro de `~/.venvs/ncrox-store-python`. Instalar ferramentas em Termux não as disponibiliza automaticamente no APK Satanachia. O comando `pack` do APK serve para consultar o índice oficial do Termux, não para instalar; use `pkg install <pacote>` dentro do app Termux. Para editar arquivos diretamente no APK, use `nano <arquivo>` (editor simples integrado, não o pacote GNU nano). Para editar arquivos no APK, use o comando `nano <arquivo>` integrado: ele abre um editor simples do app, não o GNU nano. Os pacotes não são imagens completas de Kali, Parrot, Debian ou Arch.

ZIPs usam a convenção de senha `<nome>ncrox`; esta senha previsível não verifica integridade/autenticidade. Confira origem e conteúdo antes de executar qualquer instalador.
