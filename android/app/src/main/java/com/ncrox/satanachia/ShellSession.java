package com.ncrox.satanachia;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/** A persistent, non-PTY shell session. It preserves variables/functions/cwd, but is not a terminal emulator. */
final class ShellSession implements AutoCloseable {
  static final int OUTPUT_LIMIT = 65536;
  private final long commandTimeoutMs;
  private final Object lock = new Object();
  private final String marker = "__NCROX_DONE_" + UUID.randomUUID().toString().replace("-", "") + "__";
  private final String shellPath;
  private final File appHome;
  private final boolean bash;
  private volatile Process process;
  private volatile BufferedWriter writer;
  private StringBuilder output = new StringBuilder();
  private StringBuilder line = new StringBuilder();
  private boolean truncated;
  private boolean timedOut;
  private boolean done;
  private int status;
  private String pwd;
  private String startupError;
  private long processGeneration;

  ShellSession(String shellPath, File appHome) { this(shellPath, appHome, 30000); }
  ShellSession(String shellPath, File appHome, long commandTimeoutMs) {
    this.shellPath = shellPath;
    this.appHome = appHome;
    this.commandTimeoutMs = commandTimeoutMs;
    this.bash = shellPath.endsWith("libbash.so") || shellPath.endsWith("/bash");
  }

  synchronized Result run(String command, File requestedCwd) {
    try {
      File desiredDir = requestedCwd;
      while (desiredDir != null && (!desiredDir.isDirectory() || !desiredDir.canRead())) desiredDir = desiredDir.getParentFile();
      boolean cwdRecovered = desiredDir == null || !desiredDir.equals(requestedCwd);
      if (desiredDir == null) desiredDir = appHome;
      String desired = desiredDir.getCanonicalPath();
      if (process == null || !process.isAlive()) start(desiredDir);
      if (startupError != null) return new Result(startupError, desired, 127, false);
      if (pwd == null || !new File(pwd).getCanonicalPath().equals(desired)) {
        Result changed = execute(bash ? "builtin cd -- " + quote(desired) : "cd " + quote(desired), commandTimeoutMs);
        if (changed.status != 0) return new Result("Não consegui entrar em " + desired + ":\n" + changed.output, desired, changed.status, changed.timedOut);
      }
      Result result = execute(command, commandTimeoutMs);
      if (cwdRecovered) result.output = "[diretório anterior não estava acessível; usando " + desired + "]\n" + result.output;
      return result;
    } catch (Exception e) {
      destroy();
      return new Result("Falha no shell persistente: " + e.getMessage(), requestedCwd.getAbsolutePath(), 1, false);
    }
  }

  private void start(File cwd) throws Exception {
    destroy();
    File home = appHome;
    if (!home.exists() && !home.mkdirs()) throw new IllegalStateException("não consegui criar HOME privado");
    File prefix = new File(home, "usr");
    File bin = new File(prefix, "bin");
    File tmp = new File(home, "tmp");
    bin.mkdirs(); tmp.mkdirs();
    File wd = cwd;
    while (wd != null && (!wd.isDirectory() || !wd.canRead())) wd = wd.getParentFile();
    if (wd == null) wd = home;

    ProcessBuilder pb = new ProcessBuilder(shellPath, "--noprofile", "--norc");
    if (shellPath.endsWith("/sh")) pb = new ProcessBuilder(shellPath);
    pb.directory(wd);
    pb.redirectErrorStream(true);
    pb.environment().put("HOME", home.getAbsolutePath());
    pb.environment().put("PREFIX", prefix.getAbsolutePath());
    pb.environment().put("TMPDIR", tmp.getAbsolutePath());
    pb.environment().put("SHELL", shellPath);
    pb.environment().put("TERM", "dumb");
    pb.environment().put("LANG", "C.UTF-8");
    String systemPath = bin.getAbsolutePath() + ":/system/bin:/system/xbin:/vendor/bin:/vendor/xbin:/product/bin:/apex/com.android.runtime/bin:/apex/com.android.art/bin";
    String oldPath = pb.environment().get("PATH");
    pb.environment().put("PATH", systemPath + (oldPath == null || oldPath.isEmpty() ? "" : ":" + oldPath));
    pb.environment().put("PWD", wd.getAbsolutePath());
    process = pb.start();
    long generation = ++processGeneration;
    writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    Reader reader = new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8);
    Thread pump = new Thread(() -> pump(reader, generation), "ncrox-shell-reader");
    pump.setDaemon(true); pump.start();
    output.setLength(0); line.setLength(0); truncated = false; done = false; startupError = null;
    if (bash) writer.write("shopt -s expand_aliases\ntrap 'for __ncrox_pid in $(jobs -pr); do kill \"$__ncrox_pid\" 2>/dev/null; done' EXIT\n");
    writer.flush();
    Result init = execute(":", 5000);
    if (init.timedOut || !process.isAlive()) startupError = "O shell não iniciou corretamente: " + init.output;
  }

  private Result execute(String command, long timeoutMs) {
    synchronized (lock) {
      output.setLength(0); line.setLength(0); truncated = false; timedOut = false; done = false; status = 0; pwd = null;
    }
    try {
      String statusVar = "__ncrox_status_" + marker.replaceAll("[^A-Za-z0-9_]", "");
      writer.write("if " + (bash ? "builtin " : "") + "eval " + quote(command == null ? "" : command) + "; then " + statusVar + "=0; else " + statusVar + "=$?; fi\n");
      writer.write((bash ? "builtin " : "") + "printf '\n" + marker + "|%s|%s\n' \"$" + statusVar + "\" \"$PWD\"\n");
      writer.flush();
      long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
      synchronized (lock) {
        while (!done && process != null && process.isAlive()) {
          long left = end - System.nanoTime();
          if (left <= 0) break;
          TimeUnit.NANOSECONDS.timedWait(lock, left);
        }
        if (!done) {
          if (line.length() > 0 && output.length() < OUTPUT_LIMIT) {
            int keep = Math.min(line.length(), OUTPUT_LIMIT - output.length());
            output.append(line, 0, keep);
            if (keep < line.length()) truncated = true;
          }
          boolean expired = process != null && process.isAlive();
          if (expired) {
            timedOut = true;
            process.destroy();
            try { if (!process.waitFor(150, TimeUnit.MILLISECONDS)) process.destroyForcibly(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); process.destroyForcibly(); }
            process = null; writer = null; processGeneration++;
          }
          return new Result(output.toString() + (truncated ? "\n[saída truncada em 65536 bytes]" : "") + (expired ? "\n[tempo limite excedido; sessão reiniciada]" : "\n[o processo do shell terminou; sessão será recriada]"), pwd, expired ? 124 : 1, expired);
        }
          String text = output.toString();
        if (truncated) text += "\n[saída truncada em 65536 bytes; restante descartado]";
        return new Result(text, pwd, status, false);
      }
    } catch (Exception e) {
      return new Result("Erro no shell: " + e.getMessage(), pwd, 1, false);
    }
  }

  private void pump(Reader reader, long generation) {
    char[] buf = new char[4096];
    try {
      int n;
      while ((n = reader.read(buf)) != -1) {
        synchronized (lock) {
          if (generation != processGeneration) continue;
          for (int i = 0; i < n; i++) {
            char ch = buf[i];
            if (ch == '\n') {
              consumeLine(line.toString()); line.setLength(0);
            } else if (line.length() < OUTPUT_LIMIT) line.append(ch);
            else truncated = true;
          }
          lock.notifyAll();
        }
      }
    } catch (Exception ignored) { }
    synchronized (lock) { lock.notifyAll(); }
  }

  private void consumeLine(String text) {
    if (text.startsWith(marker + "|")) {
      if (output.length() > 0 && output.charAt(output.length() - 1) == '\n') output.setLength(output.length() - 1);
      String[] fields = text.substring(marker.length() + 1).split("\\|", 2);
      try { status = Integer.parseInt(fields[0]); } catch (Exception ignored) { status = 1; }
      pwd = fields.length > 1 ? fields[1] : null;
      done = true;
    } else if (output.length() < OUTPUT_LIMIT) {
      int keep = Math.min(text.length(), OUTPUT_LIMIT - output.length());
      output.append(text, 0, keep).append('\n');
      if (keep < text.length()) truncated = true;
    } else truncated = true;
  }

  private static String quote(String s) { return "'" + s.replace("'", "'\\''") + "'"; }

  private void destroy() {
    Process p = process; BufferedWriter w = writer;
    process = null; writer = null; processGeneration++;
    if (p != null) {
      try { if (w != null) { w.write("exit\n"); w.flush(); } } catch (Exception ignored) { }
      try { if (!p.waitFor(500, TimeUnit.MILLISECONDS)) p.destroy(); }
      catch (Exception e) { p.destroy(); }
      try { if (p.isAlive() && !p.waitFor(300, TimeUnit.MILLISECONDS)) p.destroyForcibly(); }
      catch (Exception e) { p.destroyForcibly(); }
      try { if (w != null) w.close(); } catch (Exception ignored) { }
    }
  }

  @Override public void close() { destroy(); }

  static final class Result {
    String output; final String pwd; final int status; final boolean timedOut;
    Result(String output, String pwd, int status, boolean timedOut) { this.output=output; this.pwd=pwd; this.status=status; this.timedOut=timedOut; }
  }
}
