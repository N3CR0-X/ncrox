# Satanachia ncrox — comandos próprios e ambiente local

## Banners
Os comandos vigia, oraculo, bastiao e selo mostram suas artes em UTF-8 antes do resultado. As artes ficam em `banners/`. No terminal interativo, azul e laranja usam ANSI truecolor; em saída redirecionada ou na GUI Python, são exibidas sem códigos de escape. No Android, as cores são aplicadas ao texto e há rolagem horizontal. A fonte e a largura da tela podem afetar o alinhamento dos caracteres Braille; o resultado no Android ainda precisa ser verificado em aparelho. As três artes em Braille foram transcritas da conversa e precisam de conferência visual contra os originais.

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

No momento, `store/` contém só `readme.md`, sem ZIPs; por isso `satanachia -store` responde `loja vazia` até que ferramentas `.zip` sejam adicionadas.
