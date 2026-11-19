# Ferramentas de auditoria no Android

As quatro ferramentas estão integradas ao terminal Android e também disponíveis na versão Python. Execute em sistemas e arquivos próprios ou com autorização explícita.

- `vigia <host> [porta,porta,...]`: tenta conexão TCP a até 32 portas em um único endereço. Padrão Android: 80,443; Python: 22,80,443,8080. Não identifica serviços. Timeout não comprova porta fechada.
- `oraculo <domínio>`: resolve endereços IP com o resolvedor do sistema, sem enumerar registros DNS.
- `bastiao <host> [porta]`: conecta via TLS, valida cadeia e nome usando as autoridades do sistema e mostra detalhes do certificado. Padrão: 443. Isso não comprova ausência de vulnerabilidades.
- `selo <arquivo> [sha256]`: calcula SHA-256 e, opcionalmente, compara com o valor informado. Um hash só é confiável para verificação se vier de fonte confiável.

Use `help` ou `<comando> --help` para ajuda. Use aspas em caminhos com espaços. DNS depende do resolvedor do sistema e pode demorar. Java executa essas operações fora da thread da interface, sem usar shell externo. As artes ASCII ainda não foram fornecidas e não estão incluídas.

A compilação Java local e testes automatizados não equivalem a compilar um APK ou testá-lo em um aparelho. A interface depende das permissões e APIs disponíveis no Android; não oferece root, PTY completo nem garante programas como git ou apt.

Para testes Java em uma máquina com JDK: `sh android/tests/run-ferramentas.sh` a partir da raiz do projeto.
