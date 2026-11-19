# Interface Android estilo terminal

A interface padrão de instalações novas usa fundo preto, texto claro monoespaçado e botões planos. A preferência de tema já salva é preservada; para mudar, abra ⚙ e escolha “Preto e branco” (os temas verde, âmbar e claro continuam disponíveis). Abas e configurações ficam nos botões superiores.

Digite o comando e use ENVIAR, Enter no teclado físico ou a ação de concluir do teclado virtual. A barra de atalhos rola horizontalmente: ↑/↓ percorrem o histórico, ←/→ e HOME/END movem o cursor, PGUP/PGDN rolam a saída e LOCK alterna a solicitação de wake lock. Os controles têm nomes para leitores de tela e altura mínima de 48 dp. Tab físico não dispara comandos; segue a navegação de foco do Android. Salvar configurações preserva o texto digitado e a saída atual.

Os antigos botões ESC, ALT e CTRL foram retirados porque não controlavam um terminal interativo; TAB visual que enviava foi renomeado ENVIAR. Esta interface não adiciona PTY, root, shell interativo, autocompletar, emulação de teclas de terminal nem ferramentas de segurança. As limitações de comandos, Android e permissões do projeto original permanecem. Por precaução, aguarde um comando terminar antes de mudar de aba ou configurações: o comportamento assíncrono entre abas do projeto original não foi refeito.

## Verificações executadas

- `javac` compilou todos os três arquivos Java Android contra `org.robolectric:android-all:15-robolectric-12650502` e `zip4j:2.11.5` (com aviso de API obsoleta). Isso verifica fontes e tipos, não gera APK.
- Teste original `ShellRunnerTest` executado com Java: 9 verificações passaram.
- `git diff --check`: sem erros de whitespace.
- Não houve build Gradle/APK, instalação nem teste em dispositivo/emulador; conferir visual, TalkBack, teclado virtual/físico, rotação e alternância de abas no aparelho.

## Aplicação

O ZIP contém somente arquivos alterados, com caminhos relativos à raiz do projeto original. Faça backup e substitua os arquivos correspondentes no projeto extraído; não sobrescreve os arquivos Python. Alternativamente, aplique o patch na raiz do projeto original usando `git apply`.
