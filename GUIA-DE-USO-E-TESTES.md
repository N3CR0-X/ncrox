# Guia de uso e plano de testes — Satanachia ncrox

Este guia vale para o código atual do repositório. As funções variam entre o programa Python (Windows/Linux/macOS/Termux) e o APK Android. Use os comandos no campo de entrada do ncrox; no APK deste projeto, envie com o botão **ENVIAR** ou pela tecla **Enter**. `help` mostra a ajuda disponível naquele build.

## 1. Abrir o Satanachia

- **Windows:** execute `Satanachia-ncrox.exe`.
- **Termux/Linux/macOS:** abra a pasta do projeto e execute `python satanachia.py`.
- **Interface gráfica Python:** `python satanachia.py --gui` (precisa de Tk instalado).
- **Android:** instale o APK e abra o app. Ele usa Bash 5.3 numa sessão persistente independente por aba e as permissões do próprio Android; não é uma distribuição Linux/Termux e não tem root nem PTY. O comando `pkg` pode encaminhar ações limitadas ao Termux instalado via interface oficial RUN_COMMAND; a instalação continua no prefixo privado do Termux. Integração requer permissão RUN_COMMAND e `allow-external-apps=true` na configuração do Termux.

Comece com:

```text
help
pwd
```

`help` apresenta os comandos próprios e executáveis de sistema disponíveis. No Android, comandos externos usam uma sessão Bash persistente por aba: variáveis, funções e `cd` do Bash ficam naquela aba. É stdin não interativo, sem PTY, limite 30 s/64 KiB; `vim`/`top` e programas que pedem input interativo não são garantidos. O Bash vê `$HOME/usr/bin` privado do app e o PATH do Android; não vê o prefixo privado do Termux.

## 2. Comandos próprios — Python, Windows, Linux, macOS e Termux

- `help` / `ajuda`: ajuda.
- `pwd`: diretório de trabalho atual.
- `cd <pasta>`: muda o diretório do ncrox. `cd` sem argumento vai para a pasta pessoal.
- `make <pasta>` ou `make -p <caminho>`: cria pasta; `-p` cria também as pastas intermediárias.
- `tree [pasta]`: mostra arquivos e subpastas.
- `readmd <arquivo.md>`: imprime o Markdown como texto puro.
- `pick <dono/repo> [destino]`: faz clone GitHub — é o atalho próprio equivalente a `git clone`. Também aceita URL HTTPS/SSH. Requer Git e rede; não executa o conteúdo baixado.
- `docs -<extensão>`: procura arquivos por extensão no diretório atual e subpastas (por exemplo, `docs -md`).
- `applist`: lista atalhos/aplicativos encontrados pelo sistema; `applist -apk` procura APKs locais.
- `tasklist` / `tasklist -sudozi`: lista processos via sistema; `-sudozi` pede confirmação `s/n`.
- `taskkill -sudozi <nome-ou-pid>`: encontra processo e pede confirmação antes de tentar encerrá-lo; o sistema pode negar.
- `net faces`, `ip a`, `ifconfig`: informações de interface de rede, conforme ferramentas disponíveis.
- `say <texto>`: tenta síntese de voz oferecida pelo sistema.
- `open -a <app>` ou `open .<app>`: tenta abrir um aplicativo.
- `satanachia -store`: lista arquivos ZIP publicados na pasta `store/` do repositório.
- `satanachia -pick <nome>`: baixa um ZIP da loja para o diretório atual.
- `satanachia -st <nome>`: extrai um ZIP já baixado (ou tenta baixá-lo primeiro), para uma pasta com o nome do pacote. Se já existir uma pasta de destino, o build atual recusa substituir para preservar dados. A senha prevista é `<nome>ncrox`; a extração não executa os arquivos.
- `flash -on`, `flash -off`, `flash -blink`, `flash -os -time 3`: comandos de flash apenas no Android; requer hardware e autorização de câmera.
- `tasklist -sudozi` no Android só mostra o que o Android disponibiliza. `taskkill -sudozi` abre informações do app; o Android não permite a um app comum forçar a parada de outro.
- `ondeuto -ip`: localização aproximada pelo IP. `ondeuto -info`: localização do aparelho, se conceder permissão e se o provedor fornecer posição.
- `avrcp`: informa a limitação do Android para consultar a versão AVRCP.
- `github repo <dono/repo>`, `github user <usuário>`, `github releases <dono/repo>`, `github issues <dono/repo>` e `github search <termos>`: consultas públicas de leitura à API do GitHub.
- Ferramentas de auditoria autorizada: `vigia <host> [portas]`, `oraculo <domínio>`, `bastiao <host> [porta]`, `selo <arquivo> [sha256]`. Use apenas seus próprios sistemas/arquivos ou alvos com autorização explícita.
- Utilidades Python extras: `hash <arquivo>`, `disk [caminho]`, `sysinfo`, `which <programa>`, `where <programa>`, `findtext <texto> <arquivo>`, `fileinfo <caminho>`, `netcheck <host> <porta>`, `uuid`, `rede <IP/CIDR>`, `base64 codificar|decodificar <texto>`, `urlinfo <URL>`, `jsoncheck <arquivo>` e `comparar <arquivo1> <arquivo2>`.

**Observação de plataforma:** o APK não inclui Python nem Git. Comandos de utilidade Python podem não existir no Android. `pick` no APK depende de haver um executável `git` acessível ao app; não há garantia disso e o APK não instala Git por conta própria. `pkg` pode despachar ações de gerenciamento ao Termux se ele estiver instalado, com RUN_COMMAND autorizado e `allow-external-apps=true`; isso não instala ferramentas no Satanachia. Se `pick` responder que `git` não existe, isso é uma limitação do ambiente, não falha de sintaxe. Os comandos `github ...` fazem consultas pela API e são uma alternativa de teste de rede/API, mas não clonam o código.

## 3. Roteiro de teste básico

Rode um comando por vez e compare com o resultado esperado.

| Etapa | Comando | O que conferir |
|---|---|---|
| Ajuda | `help` | Aparece a ajuda; não fecha o app. |
| Localização no disco | `pwd` | Mostra o diretório atual. |
| Criar pasta de teste | `make -p ncrox-teste/subpasta` | Cria somente essa pasta. |
| Ver a árvore | `tree ncrox-teste` | Mostra `subpasta`. |
| Entrar na pasta | `cd ncrox-teste` | `pwd` termina em `ncrox-teste`. |
| Arquivo de teste (shell) | `echo teste Satanachia > nota.md` | Cria `nota.md` no diretório atual, se o shell aceitar redirecionamento. |
| Ler Markdown | `readmd nota.md` | Exibe `teste Satanachia` como texto. |
| Procurar extensão | `docs -md` | Inclui `nota.md` na lista. |
| Hash local (Python) | `hash nota.md` | Mostra SHA-256; no APK use `selo nota.md`. |
| Tipo/tamanho (Python) | `fileinfo nota.md` | Informa arquivo e tamanho. |
| Pesquisar texto (Python) | `findtext Satanachia nota.md` | Aponta a linha correspondente. |
| Voltar | `cd ..` | Volta ao diretório anterior. |

O app Android pode iniciar numa pasta privada do próprio aplicativo se ainda não tiver sido concedido acesso aos arquivos compartilhados. Nesse caso, use o botão de acesso do app/configurações do Android para testar `docs` em arquivos compartilhados; não tente acessar dados privados de outros apps.

## 4. Teste do clone Git com `pick`

Faça este teste numa pasta separada para não misturar com seus arquivos. O repositório público `octocat/Hello-World` é usado só como exemplo. No Python/desktop, `pick` aceita uma pasta de destino:

```text
make -p ncrox-clone-teste
cd ncrox-clone-teste
pick octocat/Hello-World
```

Resultado esperado no desktop: o Git começa a clonar e cria `Hello-World/`. Confira com `tree Hello-World` ou, se existir no sistema, `git -C Hello-World status`.

No APK, `pick` usa o diretório atual e não aceita argumento de destino nesta interface. Se houver Git acessível, faça:

```text
make -p ncrox-clone-teste
cd ncrox-clone-teste
pick octocat/Hello-World
```

Se aparecer `git: not found`, registre o resultado como **Git indisponível no APK**; os outros comandos não dependem de Git. Não existe `apt` garantido no app. Se a clonagem atingir o limite de tempo, tente no desktop/Termux com Git instalado.

Depois do teste, volte para fora da pasta e remova **somente a pasta de teste que você acabou de criar**, se tiver certeza de que não guardou nada nela. No Windows, apague pelo Explorador de Arquivos; em Unix/Termux, o comando nativo `rm -r ncrox-clone-teste` remove a pasta atual do teste. Confira o caminho com `pwd` antes de remover. O comando `pick` não executa os arquivos clonados.

## 5. Testes GitHub e ferramentas de rede

- `github repo octocat/Hello-World` — mostra metadados públicos do repositório.
- `github user octocat` — mostra perfil público.
- `github releases N3CR0-X/ncrox` — lista releases públicas.
- `github search terminal python` — busca repositórios públicos.
- `oraculo example.com` — resolve endereço(s) DNS; precisa de rede.
- `vigia 127.0.0.1 1,80,443` — testa apenas portas do próprio dispositivo. É normal receber “recusada”/“indisponível” se não houver serviço local escutando.
- `bastiao github.com 443` — tenta validar TLS do GitHub; depende de Internet, relógio correto e certificados do sistema.
- `selo nota.md` — calcula SHA-256 do arquivo de teste.

Consultas de rede geram tráfego. Não teste portas de terceiros sem autorização. Um timeout não prova que uma porta está fechada nem que o sistema é seguro.

## 6. Testes específicos do Android

- `flash -blink`: deve piscar quatro vezes e desligar; conceda câmera se Android perguntar. Depois, `flash -off`.
- `say Teste de voz`: o aparelho deve falar, se houver mecanismo TTS configurado.
- `applist`: lista aplicativos que Android permite exibir.
- `tasklist -sudozi`: no APK pode avisar que precisa das Opções do desenvolvedor; mesmo habilitadas, a lista continua limitada. Responda `s` para confirmar.
- `taskkill -sudozi <nome-do-app>`: abre a tela de informações do app e não encerra automaticamente o processo.
- `ondeuto -ip`: consulta serviço externo para estimativa aproximada. `ondeuto -info`: pede autorização de localização e pode depender de GPS/rede.
- `open -a Configurações` (ou o nome de um app exibido por `applist`): tenta abrir o aplicativo; nomes variam por aparelho.
- `ip a` e `ifconfig`: mostram interfaces que o Android disponibiliza.

Câmera, localização e acesso ao armazenamento são opcionais; negá-los deve apenas impedir o recurso correspondente. Não conceda permissão se não quiser usá-lo.

## 7. Testes automatizados no desktop

Na pasta extraída do projeto Python:

```text
python -m unittest discover -v
```

Os testes de rede locais precisam conseguir abrir sockets de loopback e podem falhar em ambientes restritos. Isso não substitui instalar o APK e testar no aparelho. Para os testes Java locais do projeto, é necessário JDK (`javac` e `java`); veja `android/tests/run-banners.sh` e `android/tests/run-ferramentas.sh`.

## 8. Como relatar um resultado

Anote: sistema/aparelho, versão do app ou build, comando digitado, saída completa e se uma permissão foi concedida. Remova tokens, senhas, dados pessoais e endereços/IP privados antes de compartilhar logs. Não publique informações de localização.

## Teste rápido do shell persistente e do encaminhador pkg

No APK buildado, use uma linha por vez: `export NCROX_TEST=ok`, depois `printf '%s\n' "$NCROX_TEST"` — deve mostrar `ok` na mesma aba. Em seguida `cd /storage/emulated/0` (com a permissão já concedida), `pwd` e `start`; na aba nova, a variável da aba anterior não deve existir. `pkg help` mostra as ações encaminháveis. Com Termux instalado, habilite `allow-external-apps=true`, autorize RUN_COMMAND ao Satanachia e teste `pkg search git`; para instalação real, `pkg install nano` abre confirmação no Termux. Confira dentro do Termux com `pkg list-installed`. Isso testa o encaminhamento, não instala o GNU nano dentro do Satanachia.
