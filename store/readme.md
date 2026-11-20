# Satanachia Store

A Store tem pacotes ZIP individuais para ferramentas gerais de desenvolvimento, terminal, arquivos, sincronização, rede básica e bibliotecas Python. Veja [catálogo completo (100 pacotes)](catalogo.md).

No Satanachia: `satanachia -store`; depois `satanachia -pick <nome>` e `satanachia -st <nome>`. Leia `LEIA-ME.md` dentro da pasta extraída e execute `bash install.sh` somente no Termux, Debian/Parrot ou Arch, após revisar o script. A extração não instala nem executa programas.

Termux usa `pkg`; Debian/Parrot usa `apt`; Arch usa `pacman`. A disponibilidade depende de release, arquitetura e repositórios configurados. Bibliotecas Python usam pip dentro de `~/.venvs/ncrox-store-python`. Instalar ferramentas em Termux não as disponibiliza automaticamente no APK Satanachia. Os pacotes não são imagens completas de Kali, Parrot, Debian ou Arch.

ZIPs usam a convenção de senha `<nome>ncrox`; esta senha previsível não verifica integridade/autenticidade. Confira origem e conteúdo antes de executar qualquer instalador.
