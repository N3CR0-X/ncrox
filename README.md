# Satanachia ncrox

Terminal local em Python para desktop e APK Android básico. O app Android usa `/system/bin/sh` sem root, com diretório de trabalho privado no sandbox. Isso NÃO é Termux dentro de APK: programas Termux (git, nmap etc.) não ficam acessíveis ao app por padrão. No desktop, o programa encaminha comandos ao shell instalado.

## Comandos incluídos
- `make [-p] pasta`: criar diretórios.
- `tree [pasta]`: listar em árvore.
- `pick URL|usuario/repo`: clonar via `git clone`, se git estiver instalado.
- `net faces`, `net faces -root`, `net -show -all`: informações de interfaces locais, nunca senhas Wi-Fi.
- `cd`, `pwd`, `help`, `clear`, `exit`; demais comandos seguem para shell local.

## Downloads automáticos
O GitHub Actions compila Android APK e Windows EXE ao enviar para `main` ou iniciar manualmente o workflow. Ao sucesso, publica um Release `build-N` com os dois arquivos. Consulte a aba Releases no repositório.

## Limites importantes
O APK é um terminal básico em sandbox Android: sem root, sem persistência de ambiente Linux completo, e só pode executar comandos que existam no sistema Android ou dentro do espaço do app. Não faz instalação de Kali/NetHunter, Metasploit ou ferramentas de intrusão; não recupera chaves Wi-Fi. O `pick` Android depende de `git` disponível no PATH, o que não é padrão. EXE gera-se pelo workflow Windows.
