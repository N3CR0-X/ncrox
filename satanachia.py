#!/usr/bin/env python3
"""Satanachia ncrox — local command runner for desktop and Termux."""
import os
import platform
import shlex
import subprocess
import sys
import threading
import urllib.request
import json
import zipfile
import hashlib
import shutil
from pathlib import Path
from ncrox_tools import github, local_command
from ncrox_audit import command as audit_command
from ncrox_banners import banner

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
            lines.append(prefix + "╰── [sem permissão]")
            return
        for i, item in enumerate(entries):
            last = i == len(entries) - 1
            lines.append(prefix + ("╰── " if last else "──╮─ ") + item.name + ("/" if item.is_dir() else ""))
            if item.is_dir() and not item.is_symlink():
                walk(item, prefix + ("    " if last else "│   "), depth + 1)
    walk(root)
    return "\n".join(lines)


class Shell:
    def __init__(self, color_banners=False):
        self.color_banners = color_banners
        self.cwd = Path.cwd()
        self.system = platform.system().lower()
        self.pending_install = None
        self.pending_kill = None
        self.pending_tasklist = False

    def prompt(self):
        return "0───────~$[ "

    def run(self, raw):
        raw = raw.strip()
        if not raw:
            return ""
        answer = raw.lower()
        if self.pending_install:
            name, self.pending_install = self.pending_install, None
            if answer in ("s", "sim", "y", "yes"):
                return self._install_store_package(name, replace=True)
            return "Instalação cancelada."
        if self.pending_kill:
            pid, process, self.pending_kill = self.pending_kill[0], self.pending_kill[1], None
            if answer in ("s", "sim", "y", "yes"):
                return self._terminate_process(pid, process)
            return "Interrupção cancelada."
        if self.pending_tasklist:
            self.pending_tasklist = False
            if answer not in ("s", "sim", "y", "yes"):
                return "Operação cancelada."
            return self._process_list()
        # Operators belong to the system shell, not our built-in command parser.
        # Respect quotes and backslash escapes when detecting them.
        quote = None
        escaped = False
        for char in raw:
            if escaped:
                escaped = False
            elif char == "\\" and quote != "'":
                escaped = True
            elif quote:
                if char == quote:
                    quote = None
            elif char in "'\"":
                quote = char
            elif char in "|&;<>\n":
                return self._exec_raw(raw)
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
            return ("NCROX: help, clear, pwd, cd, make, tree, pick, readmd, docs -<ext>, applist, "
                    "tasklist, taskkill, net, say, open, satanachia.\n"
                    "Utilidades: hash <arquivo>, disk [caminho], sysinfo, which|where <comando>, "
                    "findtext <texto> <arquivo>, fileinfo <caminho>, netcheck <host> <porta>, uuid, "
                    "github repo|issues|releases <dono/repo>, github user <usuário>.\n"
                    "Auditoria autorizada: vigia <host> [portas], oraculo <domínio>, "
                    "bastiao <host> [porta], selo <arquivo> [sha256].\n"
                    "Extras: rede <IP/CIDR>, base64 codificar|decodificar <texto>, "
                    "urlinfo <URL>, jsoncheck <arquivo>, comparar <arquivo1> <arquivo2>.\n"
                    "Outros comandos usam o shell local e seus programas instalados; disponibilidade e permissões variam.")
        result = audit_command(cmd, rest, self.cwd)
        if result is not None:
            art = banner(cmd, self.color_banners)
            return art + ('' if art.endswith('\n') else '\n') + result if art else result
        if cmd == "github":
            return github(rest)
        result = local_command(cmd, rest, self.cwd)
        if result is not None:
            return result
        if cmd in ("clear", "cls", "limpar"):
            return "\x1b[2J\x1b[H"
        if cmd == "pwd":
            return str(self.cwd)
        if cmd == "readmd":
            if not rest: return "Uso: readmd <arquivo.md>"
            target = Path(rest[0]).expanduser()
            if not target.is_absolute(): target = self.cwd / target
            if target.suffix.lower() != ".md": return "readmd: o arquivo precisa ter extensão .md"
            try: return target.read_text(encoding="utf-8")
            except FileNotFoundError: return f"readmd: arquivo não encontrado: {target}"
            except UnicodeDecodeError: return "readmd: arquivo não é UTF-8 válido"
            except OSError as e: return f"readmd: {e}"
        if cmd == "cd":
            target = Path(rest[0]).expanduser() if rest else Path.home()
            if not target.is_absolute(): target = self.cwd / target
            if target.is_dir(): self.cwd = target.resolve(); return str(self.cwd)
            return f"cd: diretório não encontrado: {target}"
        if cmd == "make":
            if not rest: return "Uso: make [-p|--parents] <pasta>"
            parents = "-p" in rest or "--parents" in rest
            names = [x for x in rest if x not in ("-p", "--parents")]
            if not names: return "Uso: make [-p|--parents] <pasta>"
            target = Path(names[-1]).expanduser()
            if not target.is_absolute(): target = self.cwd / target
            try: target.mkdir(parents=parents, exist_ok=parents); return f"Pasta criada: {target}"
            except Exception as e: return f"make: {e}"
        if cmd == "tree":
            if len(rest) > 1: return "Uso: tree [pasta]"
            target = Path(rest[0]).expanduser() if rest else self.cwd
            return tree(target if target.is_absolute() else self.cwd / target)
        if cmd == "pick":
            if not 1 <= len(rest) <= 2: return "Uso: pick <URL-do-repositório ou usuario/repo> [destino]"
            repo = rest[0]
            if repo.startswith('-') or (len(rest) == 2 and rest[1].startswith('-')):
                return "pick: opções não são aceitas."
            if "://" not in repo and not repo.startswith("git@"):
                import re
                if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', repo):
                    return "pick: use usuario/repo ou URL Git."
                repo = "https://github.com/" + repo.removesuffix(".git") + ".git"
            if not (repo.startswith('https://') or repo.startswith('git@')):
                return "pick: use HTTPS ou SSH no formato git@host:repo."
            return self._exec(["git", "clone", "--", repo, *rest[1:]])
        if cmd == "net": return self._net(rest)
        if cmd == "ip" and rest[:1] == ["a"]: return self._net(["-show", "-all"])
        if cmd == "ifconfig": return self._net(["faces"])
        if cmd == "docs": return self._find_files(rest)
        if cmd == "applist": return self._app_list(rest)
        if cmd == "tasklist":
            if rest and rest[0].lower() == "-sudozi":
                self.pending_tasklist = True
                return "Listar processos visíveis ao sistema? (s/n)"
            return self._process_list()
        if cmd == "taskkill":
            if len(rest) < 2 or rest[0].lower() != "-sudozi": return "Uso: taskkill -sudozi <nome-ou-pid>"
            match = self._find_process(rest[1])
            if not match: return f"Processo não encontrado: {rest[1]}"
            self.pending_kill = match
            return f"Interromper {match[1]} (PID {match[0]})? (s/n)"
        if cmd == "flash": return "Controle físico do flash só está disponível no Android e depende do aparelho/permissão."
        if cmd == "avrcp": return "A versão AVRCP ativa não é exposta por este sistema."
        if cmd == "ondeuto":
            if not rest: return "Uso: ondeuto -ip | ondeuto -info"
            if rest[0].lower() == "-ip": return self._ip_location()
            return "Localização precisa depende de uma API de localização do sistema; este build desktop não a consulta."
        if cmd == "say":
            if not rest: return "Uso: say <texto>"
            text = " ".join(rest)
            if self.system == "darwin": return self._exec(["say", text])
            if self.system == "windows":
                ps = "Add-Type -AssemblyName System.Speech; (New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('" + text.replace("'", "''") + "')"
                return self._exec_raw("powershell -NoProfile -Command " + shlex.quote(ps))
            return self._exec(["espeak", text])
        if cmd == "open":
            if not rest: return "Uso: open -a <app> | open .<app>"
            name = rest[-1].lstrip(".")
            if self.system == "windows": return self._exec_raw("start \"\" " + shlex.quote(name))
            if self.system == "darwin": return self._exec(["open", "-a", name])
            return self._exec(["xdg-open", name])
        if cmd == "satanachia":
            if not rest: return "Uso: satanachia -store | -pick <nome> | -st <nome>"
            sub = rest[0].lower()
            if sub == "-store": return self._store_list()
            if sub == "-pick" and len(rest) > 1: return self._download_store_package(rest[1])
            if sub == "-st" and len(rest) > 1: return self._install_store_package(rest[1])
            return "Uso: satanachia -store | -pick <nome> | -st <nome>"
        if cmd == "acquire" and rest[:1] == ["lock"]: return "Wake lock disponível somente no APK Android."
        if cmd == "release" and rest[:1] == ["lock"]: return "Wake lock disponível somente no APK Android."
        return self._exec_raw(raw)

    def _find_files(self, args):
        if not args: return "Uso: docs -<extensão>"
        ext = args[0].lower().lstrip("-")
        if not ext.startswith("."): ext = "." + ext
        found=[]
        for root, dirs, files in os.walk(self.cwd):
            dirs[:] = [d for d in dirs if not d.startswith(".")]
            for name in files:
                if name.lower().endswith(ext):
                    found.append(str(Path(root)/name))
                    if len(found)>=1000: return "\n".join(found)+"\n[limite 1000]"
        return "\n".join(found) if found else f"Nenhum arquivo *{ext} encontrado."

    def _app_list(self,args):
        if args and args[0].lower()=="-apk": return self._find_files([".apk"])
        if self.system=="windows": return self._exec_raw('powershell -NoProfile -Command "Get-StartApps | Sort-Object Name | Format-Table -AutoSize"')
        roots=[Path("/Applications"),Path.home()/"Applications"] if self.system=="darwin" else [Path("/usr/share/applications"),Path.home()/".local/share/applications"]
        items=[]
        for root in roots:
            if root.exists():
                items.extend(str(p) for p in root.rglob("*.app" if self.system=="darwin" else "*.desktop"))
        return "\n".join(sorted(set(items),key=str.lower)) if items else "Nenhum atalho de app encontrado nas pastas padrão."

    def _process_list(self):
        return self._exec_raw("tasklist" if self.system=="windows" else "ps aux")

    def _find_process(self,token):
        try:
            if self.system=="windows":
                import csv,io
                raw=self._exec_raw("tasklist /FO CSV /NH")
                for row in csv.reader(io.StringIO(raw)):
                    if len(row)>1 and (row[1]==token or row[0].lower()==token.lower()): return (row[1],row[0])
            else:
                p=subprocess.run(["ps","-eo","pid=,comm="],capture_output=True,text=True,errors="replace")
                for line in p.stdout.splitlines():
                    parts=line.strip().split(None,1)
                    if len(parts)==2 and (parts[0]==token or Path(parts[1]).name.lower()==token.lower()): return (parts[0],parts[1])
        except Exception: return None
        return None

    def _terminate_process(self,pid,name):
        try:
            if int(pid) <= 1 or int(pid) == os.getpid():
                return "Recusado: não vou interromper processo do sistema ou o próprio ncrox."
        except ValueError:
            return "PID inválido."
        if self.system=="windows": return self._exec(["taskkill","/PID",str(pid),"/F"])
        return self._exec(["kill",str(pid)])

    def _ip_location(self):
        try:
            req=urllib.request.Request("https://ipapi.co/json/",headers={"User-Agent":"Satanachia-ncrox"})
            with urllib.request.urlopen(req,timeout=8) as r: data=json.loads(r.read().decode("utf-8"))
            return "Localização aproximada pelo IP público (consulta enviada a ipapi.co; não confirma VPN):\nIP: "+data.get("ip","?")+"\nCidade/região: "+data.get("city","?")+", "+data.get("region","?")+"\nPaís: "+data.get("country_name","?")
        except Exception as e: return f"Consulta de IP indisponível: {e}"

    def _store_files(self):
        req = urllib.request.Request(
            "https://api.github.com/repos/N3CR0-X/ncrox/contents/store",
            headers={"User-Agent": "Satanachia-ncrox", "Accept": "application/vnd.github+json"})
        try:
            with urllib.request.urlopen(req, timeout=10) as r:
                data = json.loads(r.read().decode("utf-8"))
            if isinstance(data, dict) and data.get("message") == "Not Found":
                return []
            return [x for x in data if isinstance(x, dict) and x.get("type") == "file" and x.get("name", "").lower().endswith(".zip")]
        except Exception:
            return []

    def _store_list(self):
        files = self._store_files()
        return "loja vazia" if not files else "Ferramentas na store/:\n" + "\n".join("╰── " + x["name"] for x in files)

    def _download_store_package(self, name):
        wanted = name.lower() if name.lower().endswith(".zip") else name.lower() + ".zip"
        item = next((x for x in self._store_files() if x.get("name", "").lower() == wanted), None)
        if not item:
            return "loja vazia" if not self._store_files() else f"Ferramenta não encontrada: {name}"
        try:
            url = item.get("download_url")
            if not url:
                return "Pacote sem URL de download."
            req = urllib.request.Request(url, headers={"User-Agent": "Satanachia-ncrox"})
            with urllib.request.urlopen(req, timeout=30) as response:
                data = response.read(100 * 1024 * 1024 + 1)
            if len(data) > 100 * 1024 * 1024:
                return "Download recusado: pacote acima de 100 MB."
            dest = self.cwd / item["name"]
            dest.write_bytes(data)
            return f"Baixado {dest.name} (SHA-256 {hashlib.sha256(data).hexdigest()})"
        except Exception as e:
            return f"Download falhou: {e}"

    def _install_store_package(self, name, replace=False):
        safe_name = Path(name).name
        if safe_name.lower().endswith(".zip"):
            safe_name = safe_name[:-4]
        archive = self.cwd / (safe_name + ".zip")
        target = self.cwd / safe_name
        actual = next((x for x in self.cwd.iterdir() if x.is_file() and x.name.lower() == archive.name.lower()), None)
        if actual is None:
            fetched = self._download_store_package(safe_name)
            if fetched.startswith(("loja vazia", "Ferramenta não encontrada", "Download falhou")):
                return fetched
            actual = next((x for x in self.cwd.iterdir() if x.is_file() and x.name.lower() == archive.name.lower()), None)
            if actual is None:
                return f"ZIP não encontrado: {archive.name}"
        archive = actual
        existing = next((x for x in self.cwd.iterdir() if x.name.lower() == target.name.lower()), None)
        if existing is not None:
            target = existing
        if target.exists():
            return f"{safe_name} já existe. Por segurança, renomeie ou faça backup da pasta antes de instalar novamente."
        stage = self.cwd / (".ncrox-stage-" + str(os.getpid()) + "-" + str(__import__("time").time_ns()))
        try:
            with zipfile.ZipFile(archive) as zf:
                base = stage.resolve()
                for info in zf.infolist():
                    dest = (stage / info.filename).resolve()
                    if dest != base and base not in dest.parents:
                        return "ZIP recusado: caminho de extração sai da pasta de destino."
                    mode = info.external_attr >> 16
                    if mode & 0o170000 == 0o120000:
                        return "ZIP recusado: links simbólicos não são aceitos."
                stage.mkdir(parents=True, exist_ok=True)
                zf.extractall(stage, pwd=(safe_name + "ncrox").encode("utf-8"))
            if target.exists():
                return "Destino apareceu durante a extração; instalação cancelada para preservar os arquivos."
            stage.rename(target)
            return f"Extraído em {target}. Nenhum arquivo foi executado."
        except RuntimeError:
            return "Não foi possível extrair: senha inválida ou ZIP incompatível; conteúdo existente foi mantido."
        except Exception as e:
            return f"Não foi possível extrair o pacote: {e}; conteúdo existente foi mantido."
        finally:
            if stage.exists():
                import shutil
                shutil.rmtree(stage, ignore_errors=True)
    def available_commands(self):
        names = set()
        for directory in os.environ.get("PATH", "").split(os.pathsep):
            try:
                for entry in Path(directory).iterdir():
                    if entry.is_file() and (os.name == "nt" or os.access(entry, os.X_OK)):
                        names.add(entry.name)
            except (OSError, PermissionError):
                continue
        return ", ".join(sorted(names, key=str.lower)) or "nenhum detectado"

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
                # Android/Termux often has no /bin/sh. Avoid a login shell, which
                # can reset PATH and source user startup files on every command.
                prefix = os.environ.get("PREFIX", "")
                termux_sh = str(Path(prefix) / "bin" / "sh") if prefix else None
                shell = (termux_sh if termux_sh and os.path.isfile(termux_sh) and os.access(termux_sh, os.X_OK)
                         else shutil.which("sh") or "/bin/sh")
                p = subprocess.run([shell, "-c", raw], cwd=self.cwd, capture_output=True, text=True, errors="replace")
            output = (p.stdout + p.stderr).rstrip()
            if p.returncode != 0:
                return (output + "\n" if output else "") + f"Código de saída: {p.returncode}"
            return output or "Concluído."
        except Exception as e:
            return f"Erro: {e}"


def cli():
    shell = Shell(color_banners=sys.stdout.isatty())
    print(f"{APP} — terminal local (help para comandos, exit para sair)")
    while True:
        try:
            result = shell.run(input(shell.prompt()))
            if result:
                if result == "\x1b[2J\x1b[H":
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
    out = ScrolledText(root, bg="#080b08", fg="#7dff63", insertbackground="#7dff63", font=("Consolas", 11), wrap="none", relief="flat", padx=14, pady=12)
    out.pack(fill="both", expand=True, padx=10, pady=(10, 4))
    horizontal = tk.Scrollbar(root, orient="horizontal", command=out.xview)
    horizontal.pack(fill="x", padx=10)
    out.configure(xscrollcommand=horizontal.set)
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
