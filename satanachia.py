#!/usr/bin/env python3
"""Satanachia ncrox — local command runner for desktop and Termux."""
import os
import platform
import shlex
import subprocess
import sys
import threading
from pathlib import Path

APP = "Satanachia ncrox"


def tree(path=".", max_depth=8):
    root = Path(path).expanduser().resolve()
    if not root.exists():
        return f"tree: caminho não encontrado: {root}"
    if not root.is_dir():
        return str(root)
    lines = [str(root)]
    def walk(folder, prefix="", depth=0):
        if depth >= max_depth:
            return
        try:
            entries = sorted(folder.iterdir(), key=lambda p: (not p.is_dir(), p.name.lower()))
        except PermissionError:
            lines.append(prefix + "└── [sem permissão]")
            return
        for i, item in enumerate(entries):
            last = i == len(entries) - 1
            lines.append(prefix + ("└── " if last else "├── ") + item.name + ("/" if item.is_dir() else ""))
            if item.is_dir() and not item.is_symlink():
                walk(item, prefix + ("    " if last else "│   "), depth + 1)
    walk(root)
    return "\n".join(lines)


class Shell:
    def __init__(self):
        self.cwd = Path.cwd()
        self.system = platform.system().lower()

    def prompt(self):
        return f"{self.cwd} $ "

    def run(self, raw):
        raw = raw.strip()
        if not raw:
            return ""
        try:
            args = shlex.split(raw, posix=(os.name != "nt"))
        except ValueError as e:
            return f"Erro ao ler comando: {e}"
        if not args:
            return ""
        cmd, rest = args[0].lower(), args[1:]
        if cmd in ("exit", "quit", "sair"):
            raise SystemExit
        if cmd in ("help", "ajuda"):
            return ("Comandos ncrox: help, clear, pwd, cd [pasta], make [opções] pasta, "
                    "tree [pasta], pick <URL-ou-usuario/repo>, net faces, net faces -root, "
                    "net -show -all.\nOs demais comandos são executados pelo shell do sistema, "
                    "no diretório atual. Use apenas comandos e arquivos que você confia.")
        if cmd in ("clear", "cls", "limpar"):
            return "\x1b[2J\x1b[H"
        if cmd == "pwd":
            return str(self.cwd)
        if cmd == "cd":
            target = Path(rest[0]).expanduser() if rest else Path.home()
            if not target.is_absolute():
                target = self.cwd / target
            if target.is_dir():
                self.cwd = target.resolve()
                return str(self.cwd)
            return f"cd: diretório não encontrado: {target}"
        if cmd == "make":
            if not rest:
                return "Uso: make [-p|--parents] <nome-da-pasta>"
            # Consistent mkdir-style behavior across Windows, Unix and Termux.
            parents = "-p" in rest or "--parents" in rest
            names = [x for x in rest if x not in ("-p", "--parents")]
            if not names:
                return "Uso: make [-p|--parents] <nome-da-pasta>"
            try:
                target = Path(names[-1]).expanduser()
                if not target.is_absolute():
                    target = self.cwd / target
                target.mkdir(parents=parents, exist_ok=parents)
                return f"Pasta criada: {target}"
            except Exception as e:
                return f"make: {e}"
        if cmd in ("tree", "ls"):
            return tree(rest[0] if rest else self.cwd)
        if cmd == "pick":
            if not rest:
                return "Uso: pick <URL-do-repositório ou usuario/repositorio> [opções do git clone]"
            repo = rest[0]
            if "://" not in repo and not repo.startswith("git@"):
                repo = "https://github.com/" + repo.removesuffix(".git") + ".git"
            return self._exec(["git", "clone", repo, *rest[1:]])
        if cmd == "net":
            return self._net(rest)
        # Requested ergonomic alias. Other system commands remain native.
        if cmd == "mkdir":
            return self._exec(["mkdir", *rest])
        return self._exec_raw(raw)

    def _net(self, args):
        if args[:1] == ["faces"]:
            # `-root` is accepted as a compatibility alias, not a privilege escalation.
            # Deliberately enumerate network interfaces only; never reveal saved keys.
            if self.system == "windows":
                return self._exec(["ipconfig"])
            if self.system == "darwin":
                return self._exec(["ifconfig"])
            return self._exec(["ip", "-brief", "address"])
        if args[:2] == ["-show", "-all"]:
            if self.system == "windows":
                return self._exec(["ipconfig", "/all"])
            if self.system == "darwin":
                return self._exec(["ifconfig"])
            return self._exec(["ip", "address", "show"])
        return "Uso: net faces | net faces -root | net -show -all\nEste atalho mostra interfaces locais, não senhas/perfis Wi-Fi."

    def _exec(self, argv):
        try:
            p = subprocess.run(argv, cwd=self.cwd, capture_output=True, text=True, errors="replace")
            return (p.stdout + p.stderr).rstrip() or ("Concluído." if p.returncode == 0 else f"Código de saída: {p.returncode}")
        except FileNotFoundError:
            return f"Comando não encontrado: {argv[0]}"
        except Exception as e:
            return f"Erro: {e}"

    def _exec_raw(self, raw):
        try:
            if os.name == "nt":
                p = subprocess.run(raw, cwd=self.cwd, shell=True, capture_output=True, text=True, errors="replace")
            else:
                p = subprocess.run(["/bin/sh", "-lc", raw], cwd=self.cwd, capture_output=True, text=True, errors="replace")
            return (p.stdout + p.stderr).rstrip() or ("Concluído." if p.returncode == 0 else f"Código de saída: {p.returncode}")
        except Exception as e:
            return f"Erro: {e}"


def cli():
    shell = Shell()
    print(f"{APP} — terminal local (help para comandos, exit para sair)")
    while True:
        try:
            result = shell.run(input(shell.prompt()))
            if result:
                if result.startswith("\x1b"):
                    print(result, end="", flush=True)
                else:
                    print(result)
        except (EOFError, KeyboardInterrupt, SystemExit):
            print("\nAté mais.")
            return


def gui():
    import tkinter as tk
    from tkinter.scrolledtext import ScrolledText
    shell = Shell()
    root = tk.Tk()
    root.title(APP)
    root.geometry("900x600")
    root.configure(bg="#080b08")
    try:
        icon = tk.PhotoImage(file=str(Path(__file__).with_name("icon.png")))
        root.iconphoto(True, icon)
    except Exception:
        icon = None
    out = ScrolledText(root, bg="#080b08", fg="#7dff63", insertbackground="#7dff63", font=("Consolas", 11), wrap="word", relief="flat", padx=14, pady=12)
    out.pack(fill="both", expand=True, padx=10, pady=(10, 4))
    out.insert("end", f"{APP} | comandos locais do sistema\nDigite help para ajuda.\n\n")
    entry = tk.Entry(root, bg="#101510", fg="#7dff63", insertbackground="#7dff63", font=("Consolas", 11), relief="flat")
    entry.pack(fill="x", padx=10, pady=(4, 10), ipady=9)
    def submit(_event=None):
        raw = entry.get()
        entry.delete(0, "end")
        out.insert("end", f"{shell.prompt()}{raw}\n")
        if not raw.strip():
            return "break"
        def work():
            try:
                result = shell.run(raw)
            except SystemExit:
                root.after(0, root.destroy)
                return
            def show():
                if result:
                    out.insert("end", result + "\n")
                out.see("end")
            root.after(0, show)
        threading.Thread(target=work, daemon=True).start()
        return "break"
    entry.bind("<Return>", submit)
    entry.focus_set()
    root.mainloop()

if __name__ == "__main__":
    if "--gui" in sys.argv or getattr(sys, "frozen", False):
        gui()
    else:
        cli()
