# Android: quatro ferramentas integradas

Este projeto contém código-fonte, não um APK. `FerramentasSeguras.java` está conectado a `MainActivity.java`, incluindo os comandos e sua ajuda. A `submit()` despacha as operações no worker, fora da interface. O `PATH` é recebido por compatibilidade, mas não é consultado: as ferramentas usam APIs Java, sem shell. `cwd()` é usado para arquivos relativos. Argumentos suportam aspas simples/duplas e escape por barra invertida; não há expansão de variáveis, til ou curingas. As quatro ferramentas também estão disponíveis na versão Python; veja o README principal para diferenças.

## Uso

Use exclusivamente com autorização explícita do proprietário. Não há varredura de redes ou enumeração em massa, elevação de privilégios, root ou PTY. Vigia verifica portas TCP solicitadas em um único endereço.

* `vigia localhost 80,443` conecta sequencialmente no primeiro IP resolvido de um único host. Máximo 32 itens, portas de 1 a 65535; duplicatas removidas. Padrão 80,443. Cada tentativa tem timeout de 2 s. Conexão estabelecida não confirma qual serviço está lá; recusa, erro e timeout não devem ser confundidos com uma conclusão definitiva de segurança.
* `oraculo example.com` mostra IPs distintos reportados pelo resolvedor do sistema; não consulta nem lista todos os tipos de registros DNS.
* `bastiao example.com 443` faz TLS com timeout de 2 s para conexão e leitura, usando a confiança TLS padrão da plataforma, verificação HTTPS do nome e validade do certificado. Mostra protocolo, cifra, titular, emissor, validade e SHA-256 do certificado. Não envia requisição HTTP. Erros de confiança ou hostname não são contornados. Em produção, mantenha a fábrica/trust store padrão sem verificadores permissivos.
* `selo "arquivo com espaços.txt" [sha256-em-64-hex]` calcula SHA-256 em fluxo e compara opcionalmente sem diferenciar maiúsculas. É uma comparação de conteúdo, não prova de autoria/autenticidade. O app precisa ter acesso de leitura ao arquivo; caminhos `content://` não são suportados. O arquivo deve permanecer estável durante a leitura.
* `<comando> --help` mostra instruções detalhadas. Nome de comando sem argumentos retorna ajuda. Sem banners/arte ASCII.

## Limites e testes

A resolução DNS Java/Android não oferece timeout configurável nesta implementação; ela pode ultrapassar 2 s. A duração total de `vigia` pode atingir aproximadamente 64 s, além do DNS, com 32 timeouts sequenciais. TLS, consultas e leitura de arquivos podem ser afetados por rede, relógio do aparelho, certificados e permissões do Android. Um host pode ter múltiplos IPs; vigia e bastiao usam apenas o primeiro. Não há HTTP, UDP, detecção de serviço, nem coleta de dados de outros dispositivos além da conexão explicitamente solicitada. Nenhum resultado sozinho é uma auditoria de segurança completa.

O manifesto da base já tem `android.permission.INTERNET`. O minSdk 30 declarado na base é mantido. Nenhuma compilação Android/Gradle ou teste em aparelho/emulador foi realizada neste complemento; valide no ambiente Android antes de distribuir, em especial as verificações TLS e permissões de arquivo.

Para reproduzir os testes de JVM local com JDK 11+ e `keytool`, execute `android/tests/run-ferramentas.sh` a partir da raiz do complemento. Ele compila a classe para Java 8, gera certificados efêmeros em diretório temporário e usa loopback: verifica SHA-256, parsing, validações, TCP, DNS local e inexistente, TLS confiável, certificado não confiável e nome incorreto. O trust store temporário existe somente no processo de teste e nunca é incluído no código de produção. Na execução de referência, 22 verificações passaram com OpenJDK 21. JDK de desktop não substitui testes reais no provedor TLS Android.
