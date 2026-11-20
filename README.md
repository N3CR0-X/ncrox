# Satanachia ncrox — comandos próprios e ambiente local

**Começando e testando?** Veja o [Guia de uso e plano de testes](GUIA-DE-USO-E-TESTES.md), com comandos de exemplo, permissões Android e teste de clone Git pelo atalho `pick`.

## Banners
Os comandos vigia, oraculo, bastiao e selo mostram suas artes em UTF-8 antes do resultado. As artes ficam em `banners/`. No terminal interativo, azul e laranja usam ANSI truecolor; em saída redirecionada ou na GUI Python, são exibidas sem códigos de escape. No Android, as cores são aplicadas ao texto e há rolagem horizontal. A fonte e a largura da tela podem afetar o alinhamento dos caracteres Braille; o resultado no Android ainda precisa ser verificado em aparelho. As três artes em Braille foram transcritas da conversa e precisam de conferência visual contra os originais.

## Catálogo da Store: 100 pacotes individuais

A Satanachia Store inclui [100 pacotes em ZIPs separados](store/catalogo.md): ferramentas gerais de desenvolvimento, bibliotecas Python, editores, terminal, arquivos e rede básica. Cada ZIP tem instruções e um instalador para rodar dentro do Termux, Debian/Parrot ou Arch. Extraí-lo no Satanachia não instala o programa no APK: Android isola o app dos gerenciadores `pkg`, `apt` e `pacman`. No Termux, instale com `pkg install <pacote>` diretamente nele ou use o encaminhador `pkg` no Satanachia depois de instalar/autorizar a integração RUN_COMMAND. A lista não é todo o catálogo dessas distribuições; nomes e disponibilidade variam por versão, arquitetura e repositórios. Leia `store/catalogo.md` e o guia antes de executar um instalador.

No Satanachia: `satanachia -store`, `satanachia -pick <nome>`, `satanachia -st <nome>`. Extração não executa o instalador. Bibliotecas Python são instaladas em `~/.venvs/ncrox-store-python`; veja o guia de instalação do Termux abaixo.

## Instalar ferramentas do Termux pela Store (Android)

O pacote `dev-tools` prepara **o app Termux**, não instala Git/Python dentro do APK Satanachia. No Android, cada app fica isolado. É necessário ter o Termux oficial instalado e inicializado; o pacote não inclui nem instala o bootstrap do Termux. Se ainda não tiver Termux, instale-o por uma fonte oficial (F-Droid ou GitHub oficial), abra-o uma vez e conclua a configuração inicial.

### 1. Criar uma pasta compartilhada no Satanachia

No APK Satanachia, conceda acesso ao armazenamento compartilhado quando o Android solicitar ou pelo botão de acesso a arquivos do app. Depois, execute uma linha por vez:

```text
cd /storage/emulated/0/
make ncroxpack
cd /storage/emulated/0/ncroxpack
satanachia -pick dev-tools
satanachia -st dev-tools
```

`make ncroxpack` cria a pasta compartilhada; os dois comandos `satanachia` baixam `dev-tools.zip` da pasta `store/` deste repositório e o extraem em `/storage/emulated/0/ncroxpack/dev-tools/`. Se `cd /storage/emulated/0/` falhar ou não listar/criar a pasta, primeiro conceda ao Satanachia a permissão de acesso aos arquivos compartilhados. Não use a pasta privada do app: o Termux não consegue ler os arquivos privados do Satanachia.

### 2. Autorizar o armazenamento compartilhado no Termux

Abra o app Termux oficial e execute:

```sh
termux-setup-storage
```

Aceite a permissão de arquivos solicitada pelo Android. Esse comando configura no Termux o atalho `~/storage/shared`, que aponta para o armazenamento compartilhado do aparelho. Pode ser executado no diretório inicial do Termux; em seguida, entre na pasta criada pelo Satanachia:

```sh
cd ~/storage/shared/ncroxpack/dev-tools
ls
```

Deve aparecer `install_termux.sh` e `LEIA-ME.txt`. O caminho Android equivalente é `/storage/emulated/0/ncroxpack/dev-tools`.

### 3. Instalar os pacotes selecionados

Ainda no Termux, rode:

```sh
bash install_termux.sh
```

O instalador verifica que está no Termux, atualiza os índices de pacotes e pergunta se você quer atualizar os pacotes existentes. Depois, oferece categorias: conjunto básico, rede/diagnóstico, linguagens de desenvolvimento, ferramentas de terminal/arquivos ou todas as categorias oferecidas pelo script. Leia a lista e confirme; o `pkg` instala pacotes dos repositórios configurados no Termux. A opção “todas” significa todas as categorias incluídas neste pacote, **não todos os pacotes existentes no enorme catálogo do Termux**. Também é possível iniciar ações limitadas por `pkg` no Satanachia quando a integração RUN_COMMAND estiver autorizada; a instalação continua ocorrendo no Termux.

O conjunto básico inclui Git, Python, pip, Nano, Go, Clang, make, cmake, wget, curl, `which` e OpenSSH. As categorias opcionais incluem ferramentas como nmap, netcat, ferramentas DNS/IP, Node.js, Ruby, Perl, PHP, Rust, Vim, tmux, tree, jq, ripgrep, fd, rsync e utilitários ZIP/TAR. O instalador também cria um ambiente Python virtual em `~/.venvs/ncrox-tools` e instala nele BeautifulSoup (`beautifulsoup4`, importado como `bs4`). Para ativá-lo:

```sh
source ~/.venvs/ncrox-tools/bin/activate
python -c 'from bs4 import BeautifulSoup; print("BeautifulSoup OK")'
deactivate
```

`urllib` já faz parte da biblioteca padrão do Python; não é preciso instalar um pacote `urllib` separado. O comando `which` é o pacote `which` do Termux.

### Limites importantes

- Se o Termux/bootstrap ainda não estiver instalado, este pacote não pode instalá-lo por dentro do Satanachia: instale e inicialize o app Termux oficial primeiro.
- Se o instalador disser que precisa rodar dentro do Termux, confirme que abriu o app Termux e que `PREFIX`/`pkg` estão disponíveis; não execute o script no APK Satanachia.
- Git, Python, Nano, Go e demais ferramentas ficam no ambiente Termux. A instalação **não os torna disponíveis automaticamente ao APK Satanachia**; `pick` no APK só funciona se Git estiver acessível ao app, o que não é garantido.
- `termux-setup-storage` dá acesso ao Termux ao armazenamento compartilhado conforme a permissão Android, mas não abre dados privados de outros apps.
- Os nomes e a disponibilidade dos pacotes dependem da arquitetura, da versão do Termux e dos repositórios habilitados. O instalador pede confirmação antes da instalação.
- Senha convencional do ZIP da Store: `dev-toolsncrox`. A senha previsível não é proteção criptográfica forte; confira os arquivos e a origem antes de executar o instalador.

## Comandos do terminal
`help` lista os comandos próprios e varre os diretórios de sistema para mostrar todos os executáveis visíveis no aparelho; a quantidade muda por marca/versão e pode passar de 400, mas não é garantida. O comando `pack` consulta o catálogo oficial do Termux com `search`, `show` e `list-all` (até 100 itens); `pkg` encaminha ações limitadas ao Termux oficial instalado, quando sua integração RUN_COMMAND está autorizada. No APK há uma sessão Bash persistente por aba para comandos externos (variáveis, funções e `cd` no Bash persistem naquela aba), além de `bash --version`, `bash -c <script>` e `bash <arquivo>`. A sessão não tem PTY nem entrada interativa: prompts, `vim`, `top` e ferramentas que exigem terminal real podem não funcionar; cada comando tem timeout de 30 s e saída limitada a 64 KiB. `nano <arquivo>` abre o editor simples integrado, não o GNU nano. A disponibilidade de comandos do Android varia por aparelho, PATH e permissões; a seção abaixo diferencia comandos próprios de executáveis de sistema. No Android, `command -v <nome>` verifica uma ferramenta específica.

### Shell persistente e encaminhamento `pkg`

O shell externo do APK mantém uma instância Bash por aba: `export X=...`, funções e `cd` no Bash persistem naquela aba. Se o shell terminar, exceder timeout ou o app fechar, a sessão será reiniciada. Não há PTY nem stdin interativo, então editores de terminal, `top`, prompts e jobs em foreground não são garantidos. Limites: timeout 30 s por comando e 64 KiB de saída. O APK usa um HOME privado do app e o PATH do Android mais `$HOME/usr/bin`; não lê o prefixo privado do Termux. Para gerenciar pacotes Termux, `pkg install/search/show/list-installed/upgrade` encaminha somente ações permitidas à sessão oficial do Termux via `com.termux.RUN_COMMAND`. É necessário instalar Termux, conceder RUN_COMMAND ao Satanachia e definir `allow-external-apps=true` em `~/.termux/termux.properties`; as instalações ocorrem no Termux, nunca no sandbox do Satanachia. Veja [RUN_COMMAND oficial](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent).

### Bash no APK Android

O APK embute GNU Bash 5.3 para as arquiteturas Android arm64-v8a, armeabi-v7a, x86_64 e x86 (GPLv3-or-later; veja `android/third_party/bash/NOTICE.md`). Ele executa comandos do sistema no sandbox do app; não instala uma distribuição Linux nem concede root. O `cd` e os comandos próprios do ncrox continuam controlados pelo app. Cada aba mantém uma sessão Bash para comandos externos, com variáveis, funções e diretório do Bash preservados. É um shell persistente sem PTY/terminal real: programas interativos e leitura interativa não estão disponíveis; o shell reinicia se terminar ou exceder o limite de 30 s, e a saída é limitada a 64 KiB. `pkg` pode encaminhar ações permitidas ao Termux instalado por RUN_COMMAND, mas não coloca programas no APK.

### `pack` e `pkg`: catálogo e integração com Termux

No APK, `pack` consulta pela internet o índice oficial do Termux para a arquitetura do aparelho. `pack search <termo>`, `pack show <pacote>` e `pack list-all` (até 100 resultados) são consultas somente leitura. Para executar o gerenciador real, `pkg <ação>` encaminha ações permitidas ao Termux instalado via RUN_COMMAND; veja a seção abaixo.

**Limites importantes:** nem `pack` nem `pkg` instalam programas no sandbox do Satanachia. Android isola o prefixo privado do Termux; extrair `nano.zip` só deixa os arquivos do ZIP visíveis, não cria GNU nano no PATH do APK. `pkg` encaminha para o app Termux instalado as ações limitadas `search`, `install`, `show`, `list-installed`, `list-all`, `update`, `upgrade`, `full-upgrade`, `reinstall`, `uninstall`, `remove`, `autoremove`, `clean`, `files`, `help` e `version`. O encaminhamento requer o Termux oficial, permissão RUN_COMMAND concedida ao Satanachia e `allow-external-apps=true` em `~/.termux/termux.properties`. Confirmações e saída acontecem numa nova sessão do Termux. Argumentos aceitos são simples e não podem conter opções, pipes, shell ou comandos compostos; não há `-y` nem confirmação automática. Se a integração não estiver configurada, rode o comando diretamente no Termux. Os pacotes continuam no Termux, não no APK. Consulte a [documentação oficial do RUN_COMMAND](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent).

Também incluí `nano <arquivo>` como editor simples integrado ao Satanachia para abrir e salvar arquivos acessíveis ao app; ele não é o GNU nano.

### Lista de comandos do Satanachia Android

O `help` lista os comandos próprios e, em seguida, os executáveis que este aparelho expõe em seus diretórios de sistema. Por isso a lista muda de um celular para outro; não existe um catálogo universal garantido de 400 comandos. O Satanachia agora varre os executáveis visíveis em `/system/bin`, `/system/xbin`, `/vendor/bin`, `/vendor/xbin`, `/product/bin` e diretórios de runtime/APEX. Estar na lista não garante que o Android permita todas as opções nem que um programa interativo funcione sem PTY. Os comandos em seguida usam o diretório atual da aba quando falam em arquivos; permissões Android continuam valendo.

**Terminal e arquivos**

- `help` ou `ajuda`: mostra comandos próprios, executáveis visíveis no aparelho e limites. `command -v nome`, `type nome` e `which nome` ajudam a checar se um comando está disponível; `which` pode não vir no Android.
- `pkg help`: mostra o encaminhador limitado para Termux. Exemplos: `pkg search git`, `pkg install nano`, `pkg show python`, `pkg list-installed`, `pkg upgrade`. Requer app Termux, permissão RUN_COMMAND e `allow-external-apps=true`; a instalação sempre ocorre dentro do Termux, nunca no APK. A ação abre uma sessão Termux para mostrar prompts/resultados. Não aceita opções arbitrárias, `-y`, pipes nem comandos compostos.
- `pwd`: mostra o diretório atual da aba. `cd [pasta]`: troca a pasta da aba (`cd` sem argumento volta ao diretório inicial); caminho relativo parte da pasta atual. Não muda o diretório de outras abas.
- `ls [opções] [pasta]`, `cat arquivo`, `cp origem destino`, `mv origem destino`, `rm arquivo`, `mkdir -p pasta`, `find`, `grep`, `sed`, `head`, `tail`, `wc`, `df`, `ps`: se o Android fornecer o binário e as permissões, rodam no diretório atual; opções variam. `rm` apaga, então confira o caminho. Esses exemplos são programas do aparelho, não comandos implementados pelo Satanachia. A sessão Bash usa o PATH do Android mais `$HOME/usr/bin` privado do app; não tem permissões root.
- `make [ -p ] pasta`: comando próprio que cria uma pasta (com `-p`, cria pastas-pai); não é GNU Make e não roda Makefile. `tree [pasta]`: lista recursivamente, com profundidade limitada; caminho relativo usa a pasta atual.
- `readmd arquivo.md`: mostra Markdown como texto, somente leitura; arquivo deve ser acessível no diretório atual. `nano arquivo` ou `edit arquivo`: abre o editor de texto simples integrado; salva o arquivo acessível ao app, até 1 MiB. Não é o GNU nano do pacote Store.
- `clear` ou `cls`: limpa a tela visível do terminal. `tabs`: informa quantas abas há. `start`: abre uma aba nova do Satanachia, preservando a aba atual.
- `exit`, `logout` ou `quit`: fecha o Satanachia, para o serviço e encerra as sessões Bash. Não use como `exit` de shell se a intenção for só voltar ao prompt; no APK encerra o app.
- `bash --version`: mostra a versão do Bash embutido. `bash -c 'comandos'`: roda uma expressão Bash; `bash arquivo.sh`: executa um script no diretório atual em modo separado; `bash -c` e `bash arquivo.sh` não alteram a sessão padrão. Limites: 30 s/64 KiB; sem PTY e entrada interativa. O APK não embute `pkg`/`apt` nem tem root; o comando `pkg` encaminha ações autorizadas para o app Termux instalado.
- `pick dono/repo` ou `pick URL`: tenta `git clone` no diretório atual; requer Git já acessível ao app e rede. Não instala Git.
- `satanachia -store`: lista ZIPs publicados. `satanachia -pick nome`: baixa ZIP para o diretório atual. `satanachia -st nome`: extrai em uma subpasta segura; não executa instalador nem instala binário no PATH.
- `docs -ext`: procura arquivos pela extensão no armazenamento compartilhado (por exemplo `docs -pdf`); requer permissão de arquivos. `applist [-sudozi|-apk]`: lista apps visíveis ao Android, ou procura APKs no armazenamento compartilhado.

**Busca na web e rede**

Os links que aparecem em `search` e `web links` ficam clicáveis na tela; abrir usa o app/navegador padrão do Android. A busca online consulta o Bing em modo RSS e mostra os títulos/trechos devolvidos por ele.

- `search termo` (também `search _termo_com_espacos_`): consulta resultados públicos do Bing, resume os trechos dos três primeiros quando disponíveis e mostra até três links. Toque no endereço na saída para abrir. Requer internet; o texto é resumo automático dos trechos retornados, não validação de páginas.
- `search -o termo`: abre a busca no navegador padrão configurado no aparelho.
- `web open URL`: abre HTTP/HTTPS no navegador padrão. `web get URL` ou `web source URL`: busca o conteúdo de texto/HTML (saída limitada a 1 MiB); só HTTP/HTTPS.
- `web title URL`: lê o título HTML, se existir. `web links URL`: exibe até 20 links absolutos HTTP/HTTPS. `web status URL`: consulta o status HTTP com HEAD. `web headers URL`: mostra cabeçalhos HTTP. Alguns sites bloqueiam HEAD, automação ou acesso sem JavaScript.
- `dns domínio` ou `web dns domínio`: resolve endereços pelo resolvedor Android; não faz enumeração DNS. `whois domínio` ou `web rdap domínio`: consulta dados de registro via serviço RDAP público; os resultados dependem do registrador/serviço.
- `github repo dono/repo`, `github user nome`, `github releases dono/repo`, `github issues dono/repo`, `github search termos`: consultas públicas somente leitura à API do GitHub. Limites, internet e repositórios públicos se aplicam.
- `net`, `ip a`, `ifconfig`, `network`: endereços/interfaces locais ou tipo de conexão que o Android deixa ver. `ondeuto -ip`: localização aproximada do IP público, envia consulta a ipapi.co. `ondeuto -info`: tenta localização do aparelho após permissão; depende de GPS/provedor e pode ser imprecisa.
- `oraculo domínio`: DNS. `vigia host [porta,porta,...]`: teste TCP limitado de portas em um único host (padrão e limites aparecem em `help`); use somente com autorização. `bastiao host [porta]`: conexão TLS com validação de certificado. `selo arquivo [sha256]`: calcula SHA-256 de arquivo local.

**Utilitários de celular (25 comandos)**

Os comandos abaixo leem dados que o Android expõe ou apenas abrem a tela correspondente; nenhum muda secretamente uma configuração. Alguns fabricantes podem não oferecer a tela solicitada.

- `device`: modelo, versão Android, ABI e nome de pacote do app. Não é número de série nem identificador único.
- `battery`: porcentagem e estado carregando/descarregando que o Android informa; pode não reportar temperatura/saúde.
- `storage`: espaço total/livre da partição do app/sistema; não mede todo o armazenamento compartilhado.
- `screen`: resolução, densidade e escala de tela atual.
- `uptime`: tempo desde a inicialização mais recente, aproximado pelo relógio monotônico do Android.
- `locale`: idioma/região ativos no app. `timezone`: fuso horário atual do sistema.
- `volume`: níveis atuais/máximos de mídia, alarme, toque, notificações e chamada; não muda volume.
- `ringer`: modo normal/silencioso/vibração; não muda o perfil.
- `network`: tipo da conexão ativa (Wi-Fi/celular/Ethernet) e indicador de internet validada; não lê senha Wi-Fi.
- `wifi-settings`: abre configurações de Wi-Fi. `sound-settings`: abre configurações de som. `display-settings`: abre tela/brilho. `location-settings`: abre localização.
- `app-settings nome-ou-pacote`: abre a página de informações do app correspondente, se o Android o localizar.
- `airplane-settings`: abre configurações de modo avião (não ativa sozinho). `bluetooth-settings`: abre configurações Bluetooth. `nfc-settings`: abre configurações NFC se disponíveis.
- `keyboard-settings`: abre métodos de entrada/teclado. `accessibility-settings`: abre acessibilidade. `security-settings`: abre segurança. `apps-settings`: abre a lista de aplicativos.
- `battery-settings`: abre a tela de economia de bateria. `date-settings`: abre data e hora. `language-settings`: abre idioma/região quando suportado.
- `flash -on|-off|-blink|-os -time N`: controla o flash da câmera com permissão e aparelho compatível; limite e duração aparecem em `help`. `say texto`: lê texto com a voz disponível no Android. `open -a app` ou `open .app`: tenta abrir aplicativo instalado e visível.
- `tasklist -sudozi`: lista limitada de processos que a API fornece, após os requisitos e confirmação. `taskkill -sudozi app`: abre informações do app; é o Android que oferece a opção de forçar parada.
- `acquire lock` / `release lock`: liga/desliga o wake lock do serviço do terminal. `settings`: preferências de tema/prompt. `avrcp`: informa limite da API; não inventa uma versão Bluetooth.

**Diretórios, abas e limites**

Comandos de arquivo, comandos externos do Bash, `web get`, `web title` e `web links` não mudam a pasta da aba; arquivos relativos usam a pasta atual. `pack`, `pkg` (encaminhado ao Termux), `search`, `github`, `dns`, `whois`, `network`, utilitários de dispositivo e telas de configurações não dependem da pasta atual. `start` cria nova aba com o diretório inicial do app. Cada aba mantém a sessão e o diretório Bash independentes. Acessos a `/storage/emulated/0` exigem a permissão do Android; diretórios privados de Termux/outros apps não ficam visíveis. `exit`, `logout` ou `quit` fecha tudo do app, enquanto o botão de aba e `tabs` só gerenciam sessões.

A lista real de executáveis do celular é a que `help` varre na hora. Um nome existente pode ser bloqueado por SELinux/permissões ou não funcionar sem PTY; `vi` pode emitir caracteres de controle em vez de abrir uma sessão interativa. Um ZIP da Store não se transforma em programa executável ao ser extraído. Para pacotes Termux, use `pack search/show/list-all` no Satanachia e execute `pkg install nome` no Termux, diretamente ou pelo encaminhador `pkg` do Satanachia depois de autorizar a integração. Essa separação segue as [limitações e instruções oficiais de gerenciamento de pacotes do Termux](https://wiki.termux.com/wiki/Package_Management).

### Android
- `flash -on`, `flash -off`, `flash -blink` (quatro flashes; intervalo de 450 ms) e `flash -os -time N` (1–300 s). Requer câmera/flash suportados e autorização do usuário.
- `tasklist -sudozi`: só prossegue com Opções do desenvolvedor habilitadas e após confirmação `s/n`. Mostra a lista limitada de processos que a API Android fornece e memória total/disponível; não mede memória de cada app nem todos os serviços.
- `taskkill -sudozi <app|pacote>` abre informações do app. O Android não autoriza apps comuns a forçar o encerramento de outros processos; a pessoa decide na tela do sistema.
- `avrcp`: indica que o Android não expõe com confiabilidade a versão AVRCP negociada.
- `ondeuto -ip`: consulta ipapi.co; a consulta envia o IP público ao serviço e só dá geolocalização aproximada, não comprova VPN. `ondeuto -info`: pede permissão e busca localização do aparelho; resultado traz precisão do provedor, pode demorar e não é necessariamente exato.
- `ip a`, `ifconfig`, `net faces`: mostra interfaces/endereço quando disponíveis.
- `applist`: apps de usuário que o PackageManager mostra; `applist -sudozi`: apps de sistema e usuário que o PackageManager permite enumerar; `applist -apk`: procura arquivos `.apk` no armazenamento compartilhado.
- `docs -zip` ou `docs -<ext>` procura arquivos pela extensão no armazenamento compartilhado; requer permissão de arquivos.
- `say <texto>` usa síntese de voz do Android.
- `open -a <app>` ou `open .<app>` tenta abrir um app instalado que tenha atividade inicializável.
- `satanachia -store` lista ZIPs em `store/`; `satanachia -pick <nome>` baixa um ZIP; `satanachia -st <nome>` baixa se necessário e extrai usando `<nome>ncrox`. Nomes são comparados sem diferenciar maiúsculas/minúsculas, o SHA-256 do download é mostrado, caminhos de ZIP são validados e nada é executado. Se a pasta de destino existir, a instalação é recusada; faça backup e renomeie a pasta antes de tentar novamente. A extração ocorre em uma pasta temporária. O SHA-256 mostrado só autentica o pacote quando comparado com um valor obtido por fonte confiável.
- `readmd <arquivo.md>` exibe o conteúdo como texto puro.
- `vigia`, `oraculo`, `bastiao` e `selo`: ferramentas de auditoria real integradas. Use `help` ou `<comando> --help`. No Android, `vigia` usa portas 80 e 443 por padrão e timeout de 2 s por conexão. Consulte `android/LEIA-ME-FERRAMENTAS.md`.

### Desktop / Termux
No APK Android, comandos externos usam GNU Bash 5.3 em sessão persistente por aba, sem PTY/entrada interativa, com limites de 30 s e 64 KiB. No terminal Python/Windows, o shell do sistema é usado. O Bash do APK não inclui distribuição Linux, Termux, root ou pacotes extras. Também oferece `docs`, `applist`, `tasklist`, `taskkill -sudozi` (pede confirmação), `say`, `open` e os comandos `satanachia` que usam a mesma pasta de pacotes do repositório. Flash/AVRCP/lock de Android e localização precisa são específicos do APK.

Execute `python satanachia.py` na pasta do projeto e use `help` (ou `python satanachia.py --gui` onde Tk estiver disponível). Não instale pacotes para usar os comandos abaixo: usam a biblioteca padrão do Python. Execute testes offline com `python -m unittest discover -v` na pasta do projeto. As ferramentas de auditoria só devem ser usadas em alvos próprios ou com autorização explícita; geram consultas/conexões reais e podem aparecer em logs. As quatro ferramentas também foram integradas ao código Android; veja `android/LEIA-ME-FERRAMENTAS.md` para diferenças. O workflow compila APK/EXE; a instalação e os testes no aparelho físico ainda dependem de validação no dispositivo. As utilidades extras abaixo são exclusivas da versão Python.

- `vigia <host> [portas]`: conecta por TCP a um único host, até 32 portas numéricas separadas por vírgulas, sem intervalos (ex.: `vigia 127.0.0.1 22,80,443`). Sem lista, usa 22,80,443,8080. Resolve uma vez e verifica somente o primeiro endereço retornado; 1 s por tentativa de conexão. Apresenta aberta, recusada, timeout ou erro; uma porta aberta não garante segurança.
- `oraculo <domínio>`: resolve IPv4/IPv6 pelo resolvedor do sistema (ex.: `oraculo localhost`); não consulta registros MX/TXT nem informa TTL.
- `bastiao <host> [porta]`: conecta via TLS (porta padrão 443), valida certificado, nome e cadeia usando autoridades confiáveis do sistema, e informa protocolo, cifra e certificado. Exemplo: `bastiao localhost 8443`. Falha se o certificado não for confiável, tiver expirado ou não corresponder ao nome. A validação não garante ausência de vulnerabilidades, nem faz varredura de versões ou revogação adicional.
- `selo <arquivo> [sha256]`: calcula SHA-256 em blocos, opcionalmente compara com 64 caracteres hexadecimais (ex.: `selo documento.pdf`, acrescente o hash conhecido para comparar). Um hash só confirma integridade/autenticidade se o valor esperado vier de fonte confiável; comparação por hash não é assinatura digital.
- As resoluções DNS dependem do sistema e podem demorar mais que os timeouts das conexões; o timeout TLS é de 5 s por operação de socket, não um prazo global. Conexões a nomes com múltiplos endereços podem demorar mais por tentativas da biblioteca padrão. Nenhuma verificação é silenciosa ou prova conformidade.
- Utilidades extras: `rede 192.168.1.10/24` calcula parâmetros de rede IPv4/IPv6 sem conexão; `base64 codificar "olá mundo"` e `base64 decodificar b2zDoQ==` convertem texto UTF-8 (Base64 não é criptografia); `urlinfo https://example.org/caminho` decompõe URL sem acessá-la; `jsoncheck dados.json` valida UTF-8/JSON de até 2 MiB; `comparar a.bin b.bin` compara SHA-256 de dois arquivos locais. Use aspas para caminhos e textos com espaços.

Outros comandos da versão Python (não do APK):
- `hash <arquivo>` calcula SHA-256 local em blocos; `disk [caminho]` mostra espaço total, usado e livre em bytes; `sysinfo` mostra sistema, arquitetura, hostname, Python e diretório corrente.
- `which <programa> [...]` e `where <programa> [...]` procuram executáveis no PATH; `findtext <texto literal> <arquivo UTF-8>` mostra linhas correspondentes (até 1000). Use aspas para argumentos com espaços.
- `fileinfo <caminho>` mostra tipo, tamanho e modificação em UTC; `uuid` gera UUID aleatório; `netcheck <host> <porta>` tenta uma conexão TCP de até 3 s. Isso pode gerar registros no destino e não verifica se um serviço é confiável.
- `github repo dono/repo`, `github issues dono/repo`, `github releases dono/repo` e `github user usuário` consultam a API REST oficial, somente leitura. Issues rotulam pull requests; as listas mostram até 20 itens da primeira página. A API tem limites de acesso. Opcionalmente configure `GH_TOKEN` ou `GITHUB_TOKEN` no ambiente para acessar recursos permitidos por seu token. Não coloque tokens na linha de comando nem em capturas de tela: variáveis de ambiente podem ser visíveis a processos autorizados. O token é enviado apenas a `api.github.com` e redirecionamentos são bloqueados.
- `pick dono/repo [destino]` clona com Git; também aceita URL HTTPS ou `git@host:repo`. Requer Git e acesso de rede, não executa nem instala arquivos clonados e o Git recusa destino existente não vazio. Revise o conteúdo e a licença antes de usar. Não aceita opções arbitrárias.
- `ls` usa o executável do sistema em vez de simular uma árvore; `tree [pasta]` resolve caminhos relativos ao diretório atual do ncrox. No APK Android, pipes, redirecionamentos e combinações com `;`, `&&` etc. usam o GNU Bash embutido; comandos internos específicos do ncrox continuam sendo tratados pelo app fora dessas expressões. No terminal Python/Windows, vale o shell disponível no sistema. Em Unix/Termux o ncrox procura `$PREFIX/bin/sh` ou `sh` no PATH, sem iniciar shell de login a cada comando. A execução captura a saída: programas interativos, autenticação Git interativa e sessões de longa duração podem não funcionar; use o terminal nativo para eles. `cd` sem operadores altera o diretório do ncrox; mudanças dentro de comandos compostos não persistem.

## Permissões e limitações
O Android pergunta por câmera/localização e usa configuração especial para acesso amplo aos arquivos. `QUERY_ALL_PACKAGES` e `MANAGE_EXTERNAL_STORAGE` são sensíveis; este APK é experimental/sideload e exige avaliação de política antes de publicação em loja. Android não oferece root, memória por processo ou encerramento arbitrário a apps comuns. Permissão de arquivos compartilhados não permite ler dados privados de outros aplicativos. Downloads de ferramentas são pacotes de terceiros do repositório e devem ser revisados antes de uso; a extração não executa arquivos.

A Store atualmente contém os 100 pacotes individuais listados em `store/catalogo.md`, além de `dev-tools.zip`. Use `satanachia -store` para consultar os ZIPs publicados; a disponibilidade pode mudar. Os pacotes individuais trazem scripts de instalação para Termux/Linux: baixar e extrair não instala esses programas no APK. Rode o instalador dentro do Termux ou da distribuição apropriada. Para editar texto no app, use `nano <arquivo>` (editor simples integrado, não o GNU nano).
