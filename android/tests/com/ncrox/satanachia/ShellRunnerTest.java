package com.ncrox.satanachia;

import java.io.File;

// Dependency-free host-side regression tests; does not emulate Android.
public final class ShellRunnerTest {
  private static int tests;
  private static void check(boolean b, String message) {
    tests++;
    if (!b) throw new AssertionError(message);
  }
  public static void main(String[] args) {
    File dir = new File(System.getProperty("java.io.tmpdir"));
    String sh = "/bin/sh";
    String s = ShellRunner.run(sh, "printf out; printf err >&2; exit 7", dir, 5000);
    check(s.contains("out") && s.contains("err") && s.contains("Código de saída: 7"), "merged streams and exit status");
    s = ShellRunner.run(sh, "pwd", dir, 5000);
    check(s.equals(dir.getAbsolutePath()), "working directory");
    long begin = System.nanoTime();
    s = ShellRunner.run(sh, "yes x | head -c 2000000", dir, 5000);
    check(s.contains("saída truncada") && s.length() < 66000, "large output cap");
    check((System.nanoTime()-begin)/1000000 < 5000, "large output must not deadlock");
    s = ShellRunner.run(sh, "head -c 65536 /dev/zero | tr '\\000' x", dir, 5000);
    check(!s.contains("saída truncada") && s.length() == 65536, "exact cap not truncated");
    s = ShellRunner.run(sh, "read line || printf eof", dir, 5000);
    check(s.equals("eof"), "stdin is closed");
    begin = System.nanoTime();
    s = ShellRunner.run(sh, "exec sleep 5", dir, 200);
    check(s.contains("tempo limite excedido"), "timeout reported");
    check((System.nanoTime()-begin)/1000000 < 3000, "timeout returns promptly");
    begin = System.nanoTime();
    s = ShellRunner.run(sh, "sleep 3 & printf done", dir, 5000);
    check(s.contains("done") && (System.nanoTime()-begin)/1000000 < 2500, "inherited pipe does not block forever");
    File bash = new File("/bin/bash");
    if (bash.canExecute()) {
      s = ShellRunner.run(bash.getAbsolutePath(), "[[ 7 -gt 4 ]] && a=(um dois) && printf '%s:%s' \"${a[0]}\" \"${a[1]}\"", dir, 5000);
      check(s.equals("um:dois"), "Bash arrays and [[ ]] syntax");
    }
    File gone = new File(dir, "ncrox-vanished-cwd-test");
    check(gone.mkdir(), "create temporary stale-cwd test directory");
    check(gone.delete(), "remove stale-cwd test directory");
    s = ShellRunner.run(sh, "pwd", gone, 5000);
    check(s.equals(dir.getAbsolutePath()), "deleted current directory falls back to existing parent");
    File bash = new File("/bin/bash");
    if (bash.canExecute()) {
      s = ShellRunner.run(bash.getAbsolutePath(), "[[ 7 -gt 4 ]] && a=(um dois) && printf '%s:%s' \"${a[0]}\" \"${a[1]}\"", dir, 5000);
      check(s.equals("um:dois"), "Bash arrays and [[ ]] syntax");
    }
    System.out.println("OK: " + tests + " checks");
  }
}
