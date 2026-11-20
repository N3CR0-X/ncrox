# GNU Bash incluído no APK Satanachia ncrox

Este projeto compila o GNU Bash 5.3 para Android a partir do código-fonte publicado pelo GNU Project:

- Fonte: https://ftp.gnu.org/gnu/bash/bash-5.3.tar.gz
- Série de correções oficiais 5.3-001 a 5.3-020: https://ftp.gnu.org/gnu/bash/bash-5.3-patches/
- Licença: GNU General Public License, versão 3 ou posterior; veja `COPYING`.
- Script de build do projeto: `android/build-bash.sh`. O script fixa os SHA-256 do tarball e dos patches usados.

O Bash é software livre distribuído sem garantia, conforme a licença. Para reconstruir os executáveis incluídos no APK, use o script de build, a versão do Android NDK indicada na documentação e os fontes/patches oficiais do GNU. A integração não altera nem remove a licença do Bash. O restante do Satanachia tem seus próprios arquivos e condições de licença.
