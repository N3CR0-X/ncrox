package com.ncrox.satanachia;

import java.io.*;
import java.nio.charset.*;
import java.util.Locale;
import java.util.regex.Pattern;

/** Pure Java artwork handling; Activity provides assets and Android spans. */
public final class BannerArt {
  private static final Pattern ANSI = Pattern.compile("\\x1b\\][^\\x07\\x1b]*(?:\\x07|\\x1b\\\\)|\\x1b\\[[0-?]*[ -/]*[@-~]|\\x1b[@-_]");
  private BannerArt() {}
  public static String command(String line) {
    if (line == null || line.trim().isEmpty()) return "";
    String command = line.trim().split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
    return FerramentasSeguras.reconhece(command) ? command : "";
  }
  public static int color(String command) { return "vigia".equals(command) ? 0xffffb000 : 0xff3b82f6; }
  public static String read(InputStream input) throws IOException {
    if (input == null) return "";
    CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
    try (Reader reader = new InputStreamReader(input, decoder)) {
      StringBuilder text = new StringBuilder(); char[] buf = new char[4096]; int n;
      while ((n = reader.read(buf)) != -1) text.append(buf, 0, n);
      return ANSI.matcher(text).replaceAll("");
    }
  }
}
