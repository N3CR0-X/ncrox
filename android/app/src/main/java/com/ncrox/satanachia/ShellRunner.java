package com.ncrox.satanachia;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** Non-interactive command runner. Always drain the pipe, even after the display limit. */
final class ShellRunner {
  static final int OUTPUT_LIMIT = 65536;
  static String run(String shell, String command, File cwd, long timeoutMillis) {
    return runDirect(new String[]{shell, "-c", command}, cwd, timeoutMillis);
  }

  static String runDirect(String[] argv, File cwd, long timeoutMillis) {
    Process p = null;
    try {
      p = new ProcessBuilder(argv).directory(cwd).redirectErrorStream(true).start();
      p.getOutputStream().close(); // no terminal/stdin is attached
      final InputStream in = p.getInputStream();
      final ByteArrayOutputStream captured = new ByteArrayOutputStream();
      final boolean[] truncated = {false};
      Thread reader = new Thread(() -> {
        byte[] buf = new byte[4096];
        try {
          int n;
          while ((n = in.read(buf)) != -1) {
            synchronized (captured) {
              int keep = Math.min(n, OUTPUT_LIMIT - captured.size());
              if (keep > 0) captured.write(buf, 0, keep);
              if (keep < n) truncated[0] = true;
            }
          }
        } catch (java.io.IOException ignored) { /* stream closed at timeout */ }
      }, "ncrox-shell-output");
      reader.setDaemon(true);
      reader.start();
      boolean finished = p.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
      if (!finished) {
        p.destroy();
        if (!p.waitFor(500, TimeUnit.MILLISECONDS)) p.destroyForcibly();
      }
      // A background child might inherit the pipe, so don't wait forever for EOF.
      reader.join(1000);
      // Closing a pipe while another thread is blocked in read can also block.
      // The daemon reader exits when any background child closes the pipe.
      String output;
      synchronized (captured) {
        output = captured.toString("UTF-8").trim();
        if (truncated[0]) output += "\n[saída truncada em 65536 bytes; restante descartado]";
      }
      if (!finished) output += "\n[tempo limite excedido; processo principal encerrado]";
      else if (p.exitValue() != 0) output += "\n[Código de saída: " + p.exitValue() + "]";
      return output.trim();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      if (p != null) p.destroyForcibly();
      return "Erro: execução interrompida";
    } catch (Exception e) {
      if (p != null) p.destroyForcibly();
      return "Erro: " + e.getMessage();
    }
  }
}
