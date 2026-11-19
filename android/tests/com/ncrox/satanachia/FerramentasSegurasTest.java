package com.ncrox.satanachia;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import javax.net.ssl.*;

public class FerramentasSegurasTest {
  private static int ok = 0;
  private static void check(boolean v, String name) { if (!v) throw new AssertionError(name); ok++; }
  private static SSLServerSocket server(String path) throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12");
    try (InputStream in = new FileInputStream(path)) { ks.load(in, "testpass".toCharArray()); }
    KeyManagerFactory km = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    km.init(ks, "testpass".toCharArray());
    SSLContext ctx = SSLContext.getInstance("TLS"); ctx.init(km.getKeyManagers(), null, null);
    SSLServerSocket s = (SSLServerSocket) ctx.getServerSocketFactory().createServerSocket(0, 20, InetAddress.getByName("127.0.0.1"));
    Thread t = new Thread(() -> { while (!s.isClosed()) try (SSLSocket client = (SSLSocket)s.accept()) { client.setSoTimeout(3000); client.startHandshake(); } catch (Exception ignored) {} });
    t.setDaemon(true); t.start(); return s;
  }
  public static void main(String[] args) throws Exception {
    System.setProperty("javax.net.ssl.trustStore", args[2]);
    System.setProperty("javax.net.ssl.trustStorePassword", "testpass");
    File cwd = Files.createTempDirectory("satan-test-").toFile();
    File file = new File(cwd, "com espaço.txt"); Files.write(file.toPath(), "abc".getBytes(StandardCharsets.UTF_8));
    String known = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";
    check(FerramentasSeguras.reconhece("VIGIA"), "reconhece");
    check(!FerramentasSeguras.reconhece("sh"), "nao reconhece");
    check(FerramentasSeguras.executar("selo \"com espaço.txt\" " + known.toUpperCase(), cwd, "ignored").contains("IGUAL"), "sha conhecido e arquivo com espaços");
    check(FerramentasSeguras.selo(file.getAbsolutePath(), "0".repeat(64), cwd).contains("DIFERENTE"), "sha diferente e caminho absoluto");
    check(FerramentasSeguras.selo(file.getName(), null, cwd).contains(known), "sha sem esperado");
    check(FerramentasSeguras.selo(file.getName(), "abc", cwd).contains("64"), "sha invalido");
    check(FerramentasSeguras.selo("nao-existe", null, cwd).contains("inexistente"), "arquivo inexistente");
    check(FerramentasSeguras.executar("selo \"incompleto", cwd, null).contains("incompleto"), "aspas incompletas");
    check(FerramentasSeguras.executar("vigia --help", cwd, null).contains("autorização"), "ajuda");
    check(FerramentasSeguras.oraculo("localhost").contains("127.0.0.1") || FerramentasSeguras.oraculo("localhost").contains("0:0:0:0:0:0:0:1"), "DNS localhost");
    check(FerramentasSeguras.oraculo("nao-existe.invalid").startsWith("oraculo:"), "DNS inexistente");
    check(FerramentasSeguras.vigia("127.0.0.1/24", "80").contains("único host"), "CIDR negado");
    check(FerramentasSeguras.vigia("localhost", "80," ).contains("porta inválida"), "porta vazia");
    check(FerramentasSeguras.vigia("localhost", "0").contains("fora"), "porta zero");
    check(FerramentasSeguras.vigia("localhost", "65536").contains("fora"), "porta acima");
    check(FerramentasSeguras.vigia("localhost", "80,".repeat(32) + "443").contains("32"), "limite 32 portas");
    try (ServerSocket listener = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))) {
      int p = listener.getLocalPort();
      check(FerramentasSeguras.vigia("127.0.0.1", Integer.toString(p)).contains("estabelecida"), "TCP aberto loopback");
      check(FerramentasSeguras.vigia("127.0.0.1", p + "," + p).split("estabelecida", -1).length == 2, "deduplicacao");
    }
    int closed; try (ServerSocket socket = new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1"))) { closed = socket.getLocalPort(); }
    check(!FerramentasSeguras.vigia("127.0.0.1", Integer.toString(closed)).contains("estabelecida"), "TCP fechado");
    try (SSLServerSocket trusted = server(args[0]); SSLServerSocket untrusted = server(args[1])) {
      String result = FerramentasSeguras.bastiao("localhost", Integer.toString(trusted.getLocalPort()));
      check(result.contains("TLS validado") && result.contains("SHA-256 do certificado"), "TLS confiavel/hostname localhost: " + result);
      result = FerramentasSeguras.bastiao("127.0.0.1", Integer.toString(trusted.getLocalPort()));
      check(result.startsWith("bastiao: falha") && !result.contains("TLS validado"), "TLS nome incorreto: " + result);
      result = FerramentasSeguras.bastiao("localhost", Integer.toString(untrusted.getLocalPort()));
      check(result.startsWith("bastiao: falha") && !result.contains("TLS validado"), "TLS nao confiavel: " + result);
    }
    Files.delete(file.toPath()); Files.delete(cwd.toPath());
    System.out.println(ok + " testes passaram");
  }
}
