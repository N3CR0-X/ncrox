PACOTE dev-tools para Termux

Este instalador deve ser executado dentro do aplicativo Termux oficial. Ele NAO instala executáveis dentro do APK Satanachia nem inicializa um Termux dentro dele; Android isola os aplicativos.

Como usar no Satanachia:
1. satanachia -pick dev-tools
2. satanachia -st dev-tools
3. A pasta dev-tools será criada no diretório atual. Para que Termux leia o script, use uma pasta compartilhada, por exemplo Download; se a pasta atual do Satanachia for privada, baixe o pacote pelo Termux ou mova-o para uma área compartilhada.
4. No Termux, entre em dev-tools e execute: bash install_termux.sh
5. Escolha uma categoria e confirme cada instalação. Pacotes são obtidos pelo pkg e repositórios configurados no Termux.

O conjunto básico inclui Git, Python, pip, Nano, Go, Clang, make, cmake, wget, curl, which e OpenSSH. Categorias adicionais opcionais oferecem nmap/netcat/DNS/IP/WHOIS; Node.js/Ruby/Perl/PHP/Rust; Vim/tmux/tree/jq/ripgrep/fd/rsync/file/zip/unzip/tar/less. A opção “todas” inclui os pacotes declarados no instalador, não literalmente todo pacote existente no catálogo Termux.

BeautifulSoup (pacote Python beautifulsoup4, import bs4) fica isolado em ~/.venvs/ncrox-tools. Ative com source ~/.venvs/ncrox-tools/bin/activate. urllib.request já vem na biblioteca padrão do Python: não instale pacote “urllib” separado. O comando which é um pacote Termux, selecionado no conjunto básico.

Se o Termux/bootstrap não existir, instale o app oficial Termux pelo F-Droid ou GitHub oficial e conclua sua primeira inicialização. Este ZIP não distribui nem instala o bootstrap do Termux. Não misture APKs do Termux e plugins de origens de assinatura diferentes. A senha ZIP segue a convenção da loja: dev-toolsncrox.
