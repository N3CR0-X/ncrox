package com.ncrox.satanachia;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.*;
import javax.net.ssl.*;

/** Ferramentas independentes; execute fora da thread principal. Não executa shell. */
public final class FerramentasSeguras {
  private static final int TIMEOUT_MS = 2000;
  private static final String AVISO = "Use apenas em sistemas e arquivos próprios ou com autorização explícita.\n";
  private FerramentasSeguras() {}

  public static boolean reconhece(String comando) {
    return comando != null && Arrays.asList("vigia", "oraculo", "bastiao", "selo").contains(comando.toLowerCase(Locale.ROOT));
  }
  public static String ajuda() {
    return AVISO +
      "vigia <host> [porta,porta,...] — conexão TCP em um único endereço; até 32 portas (1–65535); padrão 80,443. 2 s por conexão. Aberta indica somente conexão, não identifica serviço.\n" +
      "oraculo <dominio> — resolve endereços IP pelo resolvedor do sistema; não enumera registros DNS.\n" +
      "bastiao <host> [porta] — negociação TLS com cadeia e nome verificados; padrão 443; exibe protocolo, cifra e certificado. Não envia HTTP.\n" +
      "selo <arquivo> [sha256] — calcula SHA-256 de arquivo local e compara opcionalmente com 64 dígitos hexadecimais. Caminho relativo usa o diretório atual; use aspas para espaços.\n" +
      "Use '<comando> --help' para esta ajuda. Resolução DNS depende do sistema e pode demorar além dos timeouts de conexão.";
  }
  /** cwd e PATH são fornecidos pela Activity. PATH não é usado: nenhum binário externo é necessário. */
  public static String executar(String linha, File cwd, String path) {
    try {
      List<String> a = tokens(linha == null ? "" : linha);
      if (a.isEmpty() || !reconhece(a.get(0))) return "Comando desconhecido.";
      if (a.size() == 2 && (a.get(1).equals("--help") || a.get(1).equals("-h"))) return ajuda();
      String c = a.get(0).toLowerCase(Locale.ROOT);
      if (c.equals("vigia")) {
        if (a.size() < 2 || a.size() > 3) return "Uso: vigia <host> [porta,porta,...]\n" + ajuda();
        return vigia(a.get(1), a.size() == 3 ? a.get(2) : null);
      }
      if (c.equals("oraculo")) {
        if (a.size() != 2) return "Uso: oraculo <dominio>\n" + ajuda();
        return oraculo(a.get(1));
      }
      if (c.equals("bastiao")) {
        if (a.size() < 2 || a.size() > 3) return "Uso: bastiao <host> [porta]\n" + ajuda();
        return bastiao(a.get(1), a.size() == 3 ? a.get(2) : null);
      }
      if (a.size() < 2 || a.size() > 3) return "Uso: selo <arquivo> [sha256]\n" + ajuda();
      return selo(a.get(1), a.size() == 3 ? a.get(2) : null, cwd);
    } catch (Exception e) { return "Erro: " + mensagem(e); }
  }
  public static String vigia(String host, String portas) {
    try {
      String h = host(host);
      String[] itens = (portas == null ? "80,443" : portas).split(",", -1);
      if (itens.length < 1 || itens.length > 32) throw new IllegalArgumentException("informe de 1 a 32 portas");
      LinkedHashSet<Integer> ps = new LinkedHashSet<>();
      for (String item : itens) ps.add(porta(item)); // valida tudo antes de qualquer conexão
      InetAddress alvo = InetAddress.getAllByName(h)[0];
      StringBuilder out = new StringBuilder(AVISO).append("Vigia: ").append(h).append(" (").append(alvo.getHostAddress()).append(")\n");
      for (int p : ps) {
        try (Socket socket = new Socket()) {
          socket.connect(new InetSocketAddress(alvo, p), TIMEOUT_MS);
          out.append(p).append(": conexão TCP estabelecida\n");
        } catch (SocketTimeoutException e) { out.append(p).append(": tempo esgotado (estado inconclusivo)\n");
        } catch (ConnectException e) { out.append(p).append(": conexão recusada ou indisponível\n");
        } catch (IOException | SecurityException e) { out.append(p).append(": falha/inconclusivo: ").append(mensagem(e)).append('\n'); }
      }
      return out.toString().trim();
    } catch (Exception e) { return "vigia: " + mensagem(e); }
  }
  public static String oraculo(String dominio) {
    try {
      String h = host(dominio);
      InetAddress[] ips = InetAddress.getAllByName(h);
      LinkedHashSet<String> unique = new LinkedHashSet<>();
      for (InetAddress ip : ips) unique.add(ip.getHostAddress());
      StringBuilder b = new StringBuilder(AVISO).append("Oráculo: ").append(h);
      for (String ip : unique) b.append('\n').append(ip);
      return b.toString();
    } catch (Exception e) { return "oraculo: " + mensagem(e); }
  }
  public static String bastiao(String host, String porta) {
    try {
      String h = host(host);
      int p = porta == null ? 443 : porta(porta);
      InetAddress target = InetAddress.getAllByName(h)[0];
      SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
      try (Socket raw = new Socket()) {
        raw.connect(new InetSocketAddress(target, p), TIMEOUT_MS);
        raw.setSoTimeout(TIMEOUT_MS);
        try (SSLSocket tls = (SSLSocket) factory.createSocket(raw, h, p, true)) {
          tls.setSoTimeout(TIMEOUT_MS);
          SSLParameters params = tls.getSSLParameters();
          params.setEndpointIdentificationAlgorithm("HTTPS");
          tls.setSSLParameters(params);
          tls.startHandshake(); // trust store padrão + verificação HTTPS do host; nunca trust-all
          SSLSession session = tls.getSession();
          Certificate[] chain = session.getPeerCertificates();
          if (chain.length == 0 || !(chain[0] instanceof X509Certificate)) throw new SSLPeerUnverifiedException("certificado X.509 indisponível");
          X509Certificate cert = (X509Certificate) chain[0];
          cert.checkValidity();
          return AVISO + "TLS validado para " + h + ":" + p + " (" + target.getHostAddress() + ")\n" +
            "Protocolo: " + session.getProtocol() + "\nCifra: " + session.getCipherSuite() +
            "\nTitular: " + cert.getSubjectX500Principal().getName() + "\nEmissor: " + cert.getIssuerX500Principal().getName() +
            "\nVálido desde: " + cert.getNotBefore() + "\nVálido até: " + cert.getNotAfter() +
            "\nSHA-256 do certificado: " + hex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
        }
      }
    } catch (Exception e) { return "bastiao: falha de conexão ou validação TLS; nenhum certificado foi aceito: " + mensagem(e); }
  }
  public static String selo(String arquivo, String esperado, File cwd) {
    try {
      if (arquivo == null || arquivo.isEmpty()) throw new IllegalArgumentException("informe um arquivo");
      if (esperado != null && !esperado.matches("(?i)[0-9a-f]{64}")) throw new IllegalArgumentException("SHA-256 esperado deve conter 64 dígitos hexadecimais");
      File f = new File(arquivo);
      if (!f.isAbsolute()) {
        if (cwd == null || !cwd.isDirectory()) throw new IllegalArgumentException("diretório atual inválido");
        f = new File(cwd, arquivo);
      }
      if (!f.isFile()) throw new IOException("arquivo inexistente ou não regular");
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
        byte[] buf = new byte[16384]; int n;
        while ((n = in.read(buf)) != -1) md.update(buf, 0, n);
      }
      String hash = hex(md.digest());
      return AVISO + "Arquivo: " + f.getAbsolutePath() + "\nSHA-256: " + hash + (esperado == null ? "" : "\nComparação: " + (hash.equalsIgnoreCase(esperado) ? "IGUAL" : "DIFERENTE"));
    } catch (Exception e) { return "selo: " + mensagem(e); }
  }
  private static int porta(String s) {
    if (s == null || !s.matches("[0-9]{1,5}")) throw new IllegalArgumentException("porta inválida: " + s);
    int n = Integer.parseInt(s);
    if (n < 1 || n > 65535) throw new IllegalArgumentException("porta fora de 1–65535: " + s);
    return n;
  }
  private static String host(String s) {
    if (s == null || s.isEmpty() || s.length() > 253 || s.matches(".*[\\s/,\\[\\]@?#%].*") || s.contains("://")) throw new IllegalArgumentException("informe um único host, sem URL, porta ou CIDR");
    if (s.indexOf(':') >= 0) {
      if (!s.matches("[0-9a-fA-F:.]+") || s.chars().filter(ch -> ch == ':').count() < 2) throw new IllegalArgumentException("IPv6 inválido");
      return s;
    }
    String ascii = IDN.toASCII(s, IDN.USE_STD3_ASCII_RULES);
    if (ascii.isEmpty() || ascii.length() > 253) throw new IllegalArgumentException("host inválido");
    return ascii;
  }
  private static String hex(byte[] bytes) {
    StringBuilder b = new StringBuilder(bytes.length * 2);
    for (byte v : bytes) b.append(String.format(Locale.ROOT, "%02x", v & 255));
    return b.toString();
  }
  private static String mensagem(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
  private static List<String> tokens(String line) {
    List<String> result = new ArrayList<>(); StringBuilder b = new StringBuilder();
    char quote = 0; boolean escaped = false, started = false;
    for (int i = 0; i < line.length(); i++) {
      char ch = line.charAt(i);
      if (escaped) { b.append(ch); escaped = false; started = true; }
      else if (ch == '\\' && quote != '\'') { escaped = true; started = true; }
      else if (quote != 0) { if (ch == quote) quote = 0; else b.append(ch); started = true; }
      else if (ch == '\'' || ch == '"') { quote = ch; started = true; }
      else if (Character.isWhitespace(ch)) { if (started) { result.add(b.toString()); b.setLength(0); started = false; } }
      else { b.append(ch); started = true; }
    }
    if (escaped || quote != 0) throw new IllegalArgumentException("aspas ou escape incompleto");
    if (started) result.add(b.toString());
    return result;
  }
}
