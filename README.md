# Satanachia ncrox — comandos próprios e ambiente local

**Começando e testando?** Veja o [Guia de uso e plano de testes](GUIA-DE-USO-E-TESTES.md), com comandos de exemplo, permissões Android e teste de clone Git pelo atalho `pick`.

## Banners
Os comandos vigia, oraculo, bastiao e selo mostram suas artes em UTF-8 antes do resultado. As artes ficam em `banners/`. No terminal interativo, azul e laranja usam ANSI truecolor; em saída redirecionada ou na GUI Python, são exibidas sem códigos de escape. No Android, as cores são aplicadas ao texto e há rolagem horizontal. A fonte e a largura da tela podem afetar o alinhamento dos caracteres Braille; o resultado no Android ainda precisa ser verificado em aparelho. As três artes em Braille foram transcritas da conversa e precisam de conferência visual contra os originais.

## Catálogo da Store: 100 pacotes individuais

A Satanachia Store inclui [100 pacotes em ZIPs separados](store/catalogo.md): ferramentas gerais de desenvolvimento, bibliotecas Python, editores, terminal, arquivos e rede básica. Cada ZIP contém instruções e um instalador que usa `pkg` no Termux, `apt` no Debian/Parrot ou `pacman` no Arch existente. A lista não é literalmente o catálogo inteiro dessas distribuições; disponibilidade/nome varia conforme release, arquitetura e repositórios. Não são imagens completas de sistemas operacionais nem ferramentas que o APK instala dentro de si. Leia `store/catalogo.md` e o `LEIA-ME.md` do pacote antes de executar `bash install.sh`.

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

O instalador verifica que está no Termux, atualiza os índices de pacotes e pergunta se você quer atualizar os pacotes existentes. Depois, oferece categorias: conjunto básico, rede/diagnóstico, linguagens de desenvolvimento, ferramentas de terminal/arquivos ou todas as categorias oferecidas pelo script. Leia a lista e confirme; o `pkg` instala pacotes dos repositórios configurados no Termux. A opção “todas” significa todas as categorias incluídas neste pacote, **não todos os pacotes existentes no enorme catálogo do Termux**.

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
`help` lista os comandos próprios. Não existe listagem portátil de absolutamente todos os comandos: built-ins do shell e ferramentas disponíveis variam conforme o aparelho e permissões. No Android, `command -v <nome>` verifica uma ferramenta específica.

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
O shell encaminha comandos para os programas locais; executáveis variam por sistema. Também oferece `docs`, `applist`, `tasklist`, `taskkill -sudozi` (pede confirmação), `say`, `open` e os comandos `satanachia` que usam a mesma pasta de pacotes do repositório. Flash/AVRCP/lock de Android e localização precisa são específicos do APK.

Execute `python satanachia.py` na pasta do projeto e use `help` (ou `python satanachia.py --gui` onde Tk estiver disponível). Não instale pacotes para usar os comandos abaixo: usam a biblioteca padrão do Python. Execute testes offline com `python -m unittest discover -v` na pasta do projeto. As ferramentas de auditoria só devem ser usadas em alvos próprios ou com autorização explícita; geram consultas/conexões reais e podem aparecer em logs. As quatro ferramentas também foram integradas ao código Android; veja `android/LEIA-ME-FERRAMENTAS.md` para diferenças. Ainda não há APK compilado ou teste em aparelho. As utilidades extras abaixo são exclusivas da versão Python.

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
- `ls` usa o executável do sistema em vez de simular uma árvore; `tree [pasta]` resolve caminhos relativos ao diretório atual do ncrox. Pipes, redirecionamentos e combinações com `;`, `&&` etc. são executados pelo shell do sistema, portanto os comandos internos específicos do ncrox não ficam disponíveis nesses pipelines. Em Unix/Termux o ncrox procura `$PREFIX/bin/sh` ou `sh` no PATH, sem iniciar shell de login a cada comando. A execução captura a saída: programas interativos, autenticação Git interativa e sessões de longa duração podem não funcionar; use o terminal nativo para eles. `cd` sem operadores altera o diretório do ncrox; mudanças dentro de comandos compostos não persistem.

## Permissões e limitações
O Android pergunta por câmera/localização e usa configuração especial para acesso amplo aos arquivos. `QUERY_ALL_PACKAGES` e `MANAGE_EXTERNAL_STORAGE` são sensíveis; este APK é experimental/sideload e exige avaliação de política antes de publicação em loja. Android não oferece root, memória por processo ou encerramento arbitrário a apps comuns. Permissão de arquivos compartilhados não permite ler dados privados de outros aplicativos. Downloads de ferramentas são pacotes de terceiros do repositório e devem ser revisados antes de uso; a extração não executa arquivos.

A Store atualmente contém os 100 pacotes individuais listados em `store/catalogo.md`, além de `dev-tools.zip`. Use `satanachia -store` para consultar os ZIPs publicados; a disponibilidade pode mudar.
