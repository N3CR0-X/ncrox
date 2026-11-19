# Satanachia ncrox — comandos próprios e ambiente local

## Comandos do terminal
`help` lista os comandos do ncrox e executáveis detectados no PATH. Não existe listagem portátil de absolutamente todos os comandos: built-ins do shell e ferramentas disponíveis variam conforme o aparelho e permissões. No Android, `command -v <nome>` verifica uma ferramenta específica.

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
- `satanachia -store` lista ZIPs em `store/`; `satanachia -pick <nome>` baixa um ZIP; `satanachia -st <nome>` baixa se necessário e extrai usando `<nome>ncrox`. Nomes são comparados sem diferenciar maiúsculas/minúsculas, o SHA-256 do download é mostrado, caminhos de ZIP são validados e nada é executado. Se a pasta de destino existir, pede `s/n`; a extração ocorre em staging para preservar o conteúdo atual em caso de falha.
- `readmd <arquivo.md>` exibe o conteúdo como texto puro.

### Desktop / Termux
O shell encaminha comandos para os programas locais; executáveis variam por sistema. Também oferece `docs`, `applist`, `tasklist`, `taskkill -sudozi` (pede confirmação), `say`, `open` e os comandos `satanachia` que usam a mesma pasta de pacotes do repositório. Flash/AVRCP/lock de Android e localização precisa são específicos do APK.

## Permissões e limitações
O Android pergunta por câmera/localização e usa configuração especial para acesso amplo aos arquivos. `QUERY_ALL_PACKAGES` e `MANAGE_EXTERNAL_STORAGE` são sensíveis; este APK é experimental/sideload e exige avaliação de política antes de publicação em loja. Android não oferece root, memória por processo ou encerramento arbitrário a apps comuns. Permissão de arquivos compartilhados não permite ler dados privados de outros aplicativos. Downloads de ferramentas são pacotes de terceiros do repositório e devem ser revisados antes de uso; a extração não executa arquivos.

No momento, `store/` contém só `readme.md`, sem ZIPs; por isso `satanachia -store` responde `loja vazia` até que ferramentas `.zip` sejam adicionadas.
