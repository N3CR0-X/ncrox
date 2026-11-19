package com.ncrox.satanachia;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class BannerArtTest {
  private static void check(boolean ok, String what) { if (!ok) throw new AssertionError(what); }
  public static void main(String[] args) throws Exception {
    String input = "\u001b[38;2;255;100;4m  ╭─á▀  \u001b[0m\n\u001b[38;2;0;2;4m └─終\u001b[0m\n";
    String plain = BannerArt.read(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
    check(plain.equals("  ╭─á▀  \n └─終\n"), "ANSI e preservação exata");
    check(BannerArt.read(null).isEmpty(), "ausente");
    for (String name : new String[]{"vigia", "oraculo", "bastiao", "selo"}) {
      check(BannerArt.command(name.toUpperCase()+" --help").equals(name), "roteamento " + name);
      check(BannerArt.color(name) == (name.equals("vigia") ? 0xffffb000 : 0xff3b82f6), "cor " + name);
    }
    check(BannerArt.command("echo vigia").isEmpty(), "comando externo");
    check(BannerArt.command("vigia; echo oi").isEmpty(), "shell não reconhecido");
    check(BannerArt.read(new ByteArrayInputStream("\u001b[2J\u001b]0;title\u0007abc".getBytes(StandardCharsets.UTF_8))).equals("abc"), "outros controles");
    try { BannerArt.read(new ByteArrayInputStream(new byte[]{(byte)0xff})); throw new AssertionError("UTF-8 inválido"); }
    catch (IOException expected) { }
    System.out.println("BannerArtTest: OK");
  }
}
