# Satanachia ncrox

Terminal local experimental para desktop e Android. A tela Android é minimalista: prompt curto sem caminho, histórico, teclas ESC/TAB/CTRL/ALT/setas/HOME/END/PGUP/PGDN/LOCK; TAB envia o comando. Use `pwd` para exibir o caminho atual. A árvore usa `╰──` e `──╮─`.

O Android pode pedir acesso a arquivos compartilhados nas Configurações do sistema; o botão desaparece quando o acesso é concedido. Acesso amplo a arquivos é sensível e builds de teste são para instalação direta, não publicação na Play Store sem a revisão e declaração exigidas.

O app inicia um serviço Android em primeiro plano com notificação persistente para continuar ativo em segundo plano. Wake lock é opcional e fica desligado por padrão: tecla `LOCK` ou comando `acquire lock` liga; `release lock` desliga. Wake lock gasta bateria. O Android ainda pode interromper o serviço em force-stop ou por políticas do fabricante.

Comandos: `pwd`, `cd`, `make`, `tree`, `pick`, `net faces`, `acquire lock`, `release lock` e comandos disponíveis em `/system/bin/sh`. APK não é Termux: não tem root, gerenciador de pacotes Linux, nem todas as ferramentas. A permissão de arquivos compartilhados não permite ler dados privados de outros apps. Não inclui extração de credenciais nem módulos de intrusão.
