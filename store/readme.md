Pacote dev-tools — SOMENTE Termux no Android

Instala git, Python, pip, nano, Go, curl, wget, OpenSSH, tree, jq, zip/unzip, tar, clang, make e cmake pelo gerenciador oficial pkg do Termux. Instala beautifulsoup4 no ambiente Python isolado ~/.venvs/ncrox-tools.

Como instalar: no Satanachia use satanachia -pick dev-tools e depois satanachia -st dev-tools. A extração cria a pasta dev-tools no diretório atual. Abra o Termux, entre na pasta extraída (ela precisa estar no armazenamento compartilhado acessível ao Termux) e execute bash install_termux.sh. O script mostra a lista e pede confirmação antes de instalar. Para BeautifulSoup, use source ~/.venvs/ncrox-tools/bin/activate; então importe com from bs4 import BeautifulSoup.

Senha ZIP: dev-toolsncrox. O pacote não instala ferramentas dentro do APK Satanachia. O Android isola os apps; git/python instalados no Termux ficam disponíveis no Termux, não automaticamente no APK. O APK pode baixar/extrair o pacote, mas não executar o instalador no Termux.
