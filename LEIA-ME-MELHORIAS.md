# Satanachia ncrox: versão ampliada

Base: https://github.com/N3CR0-X/ncrox, commit 208e3c2964671ebc738a8fa0b5051f25eaf9caed. Esta é uma cópia modificada do código-fonte, não uma versão oficial publicada.

## Termux

Instale Python e Git pelo gerenciador do Termux, extraia o ZIP e execute `python satanachia.py`. O comando `help` mostra os recursos. Exemplos:

- `sysinfo`, `disk`, `fileinfo arquivo`, `hash arquivo` e `uuid`
- `findtext "palavra" arquivo.txt`, `which python` e `netcheck example.org 443`
- `github repo N3CR0-X/ncrox`, `github issues N3CR0-X/ncrox`, `github releases N3CR0-X/ncrox`, `github user N3CR0-X`
- `pick N3CR0-X/ncrox` clona o repositório usando Git. Consulte a licença e revise o conteúdo antes de executá-lo.
- Comandos do sistema e pipelines usam o shell realmente instalado. A disponibilidade dos programas e as permissões dependem do ambiente.

Detalhes e mais comandos: README.md. Testes no computador: `python -m unittest -v`.

## Android

A pasta android contém o código-fonte do aplicativo, NÃO um APK pronto. Os novos comandos `github repo`, `github user`, `github issues`, `github releases` e `github search` consultam a API pública. `pick` requer Git disponível ao aplicativo. O shell é o do Android, limitado às permissões do app, não é Termux, nem oferece Debian, Parrot, root ou apt por padrão. Veja android/README.md para limites, build e testes.

Não houve compilação nem teste do APK em dispositivo Android nesta entrega. Shell não é um terminal interativo completo, não tem PTY e pode não funcionar com programas que aguardam entrada. O executável Android limita tempo e saída. Não há garantia de que programas baixados funcionem no APK.

Por segurança, a loja deixa de substituir pastas existentes: faça backup e renomeie antes de extrair novamente. O código não instala ou executa automaticamente conteúdo clonado. O repositório original não apresenta arquivo de licença; esclareça a licença antes de redistribuir.
