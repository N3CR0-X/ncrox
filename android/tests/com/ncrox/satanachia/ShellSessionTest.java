package com.ncrox.satanachia;

import java.io.File;

/** Host regression checks for persistent shell semantics; does not emulate Android or a PTY. */
public final class ShellSessionTest {
  private static int tests;
  private static void check(boolean ok, String message) {
    tests++;
    if (!ok) throw new AssertionError(message);
  }
  public static void main(String[] args) throws Exception {
    File bash = new File("/bin/bash");
    if (!bash.canExecute()) { System.out.println("SKIP: /bin/bash unavailable"); return; }
    File root = new File(System.getProperty("java.io.tmpdir"), "ncrox-shell-session-test-" + System.nanoTime());
    check(root.mkdirs(), "create isolated HOME");
    File work = new File(root, "work");
    check(work.mkdir(), "create working directory");
    try (ShellSession shell = new ShellSession(bash.getAbsolutePath(), new File(root, "home"), 500)) {
      ShellSession.Result r = shell.run("export NCROX_SESSION_TEST=preserved", work);
      check(r.status == 0, "export command succeeded");
      r = shell.run("printf '%s' \"$NCROX_SESSION_TEST\"", work);
      check("preserved".equals(r.output), "environment survives between shell commands");
      r = shell.run("alias ncrox_alias='printf alias-ok'", work);
      check(r.status == 0, "alias definition succeeds");
      r = shell.run("ncrox_alias", work);
      check("alias-ok".equals(r.output), "Bash aliases work in the persistent session");
      File nested = new File(work, "nested");
      check(nested.mkdir(), "create nested directory");
      r = shell.run("cd nested", work);
      check(nested.getCanonicalPath().equals(new File(r.pwd).getCanonicalPath()), "shell cd changes persistent cwd");
      r = shell.run("pwd", nested);
      check(nested.getCanonicalPath().equals(new File(r.output).getCanonicalPath()), "pwd sees persistent cwd");
      r = shell.run("printf out; printf err >&2; false", nested);
      check(r.output.contains("outerr") && r.status != 0, "stdout, stderr and status captured");
      r = shell.run("exec sleep 5", nested);
      check(r.timedOut && r.output.contains("tempo limite excedido"), "timeout terminates and resets hung session");
      r = shell.run("printf recovered", nested);
      check("recovered".equals(r.output), "session restarts after timeout");
      String home = new File(root, "home").getCanonicalPath();
      r = shell.run("printf '%s' \"$HOME\"", nested);
      check(home.equals(r.output), "shell HOME is isolated to app-owned directory");
      try (ShellSession isolated = new ShellSession(bash.getAbsolutePath(), new File(root, "other-home"), 500)) {
        r = isolated.run("printf '%s' \"${NCROX_SESSION_TEST-unset}\"", work);
        check("unset".equals(r.output), "separate tab/session does not inherit variables");
      }
    } finally {
      remove(root);
    }
    System.out.println("OK: " + tests + " persistent shell checks");
  }
  private static void remove(File f) {
    File[] children = f.listFiles(); if (children != null) for (File child : children) remove(child); f.delete();
  }
}
