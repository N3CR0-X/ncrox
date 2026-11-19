"""Portable, local utilities and read-only GitHub API commands for ncrox."""
import hashlib
import json
import os
import platform
import re
import shutil
import socket
import urllib.error
import urllib.request
import uuid
from datetime import datetime, timezone
from pathlib import Path


def local_command(cmd, args, cwd):
    """Return output for our commands, or None if cmd is not ours."""
    if cmd not in ('hash', 'disk', 'sysinfo', 'which', 'where', 'findtext', 'fileinfo', 'netcheck', 'uuid'):
        return None
    try:
        if cmd == 'uuid':
            return 'Uso: uuid' if args else str(uuid.uuid4())
        if cmd == 'netcheck':
            if len(args) != 2 or not args[1].isdigit() or not 1 <= int(args[1]) <= 65535:
                return 'Uso: netcheck <host> <porta TCP 1-65535>'
            with socket.create_connection((args[0], int(args[1])), timeout=3):
                return f'TCP conectado a {args[0]}:{args[1]}'
        if cmd == 'fileinfo':
            if len(args) != 1:
                return 'Uso: fileinfo <caminho>'
            path = Path(args[0]).expanduser()
            if not path.is_absolute():
                path = cwd / path
            stat = path.stat()
            modified = datetime.fromtimestamp(stat.st_mtime, timezone.utc).isoformat()
            kind = 'diretório' if path.is_dir() else 'arquivo' if path.is_file() else 'outro'
            return f'{path}: {kind}, {stat.st_size} bytes, modificado {modified}'
        if cmd == 'hash':
            if len(args) != 1:
                return 'Uso: hash <arquivo> (SHA-256)'
            path = Path(args[0]).expanduser()
            if not path.is_absolute():
                path = cwd / path
            digest = hashlib.sha256()
            with path.open('rb') as stream:
                for block in iter(lambda: stream.read(1024 * 1024), b''):
                    digest.update(block)
            return f'{digest.hexdigest()}  {path}'
        if cmd == 'disk':
            if len(args) > 1:
                return 'Uso: disk [caminho]'
            path = Path(args[0]).expanduser() if args else cwd
            if not path.is_absolute():
                path = cwd / path
            total, used, free = shutil.disk_usage(path)
            return f'{path}: total={total} bytes, usado={used} bytes, livre={free} bytes'
        if cmd == 'sysinfo':
            if args:
                return 'Uso: sysinfo'
            return (f'Sistema: {platform.system()} {platform.release()}\n'
                    f'Arquitetura: {platform.machine()}\nHost: {socket.gethostname()}\n'
                    f'Python: {platform.python_version()}\nDiretório: {cwd}')
        if cmd in ('which', 'where'):
            if not args:
                return f'Uso: {cmd} <executável> [...]'
            lines = []
            for name in args:
                # shutil.which interprets a relative path relative to the process cwd.
                candidate = str(cwd / name) if (os.sep in name and not os.path.isabs(name)) else name
                found = shutil.which(candidate)
                lines.append(f'{name}: {found or "não encontrado"}')
            return '\n'.join(lines)
        if len(args) != 2:
            return 'Uso: findtext <texto literal> <arquivo UTF-8>'
        path = Path(args[1]).expanduser()
        if not path.is_absolute():
            path = cwd / path
        matches = []
        with path.open('r', encoding='utf-8') as stream:
            for number, line in enumerate(stream, 1):
                if args[0] in line:
                    matches.append(f'{number}: {line.rstrip()}')
                    if len(matches) >= 1000:
                        matches.append('[limite de 1000 resultados]')
                        break
        return '\n'.join(matches) if matches else 'Nenhuma ocorrência.'
    except (OSError, UnicodeError, ValueError) as exc:
        return f'{cmd}: {exc}'


class _NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        # Never forward tokens to a redirect destination.
        return None


def github(args):
    """Read a single GitHub REST API endpoint; never run or install content."""
    usage = 'Uso: github repo|issues|releases <dono/repo> | github user <usuário>'
    if len(args) != 2 or args[0] not in ('repo', 'issues', 'releases', 'user'):
        return usage
    kind, name = args
    if kind == 'user':
        if not re.fullmatch(r'[A-Za-z0-9](?:[A-Za-z0-9-]{0,37}[A-Za-z0-9])?', name):
            return 'github: usuário inválido.'
        endpoint = '/users/' + name
    else:
        if not re.fullmatch(r'[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+', name):
            return 'github: use dono/repo, sem URL, parâmetros ou barras extras.'
        owner, repo = name.split('/')
        if owner in ('.', '..') or repo in ('.', '..'):
            return 'github: repositório inválido.'
        endpoint = '/repos/' + name
        if kind in ('issues', 'releases'):
            endpoint += '/' + kind + '?per_page=20'
    headers = {'Accept': 'application/vnd.github+json', 'User-Agent': 'Satanachia-ncrox',
               'X-GitHub-Api-Version': '2022-11-28'}
    token = os.environ.get('GH_TOKEN') or os.environ.get('GITHUB_TOKEN')
    if token:
        if '\n' in token or '\r' in token:
            return 'github: token inválido no ambiente.'
        headers['Authorization'] = 'Bearer ' + token
    request = urllib.request.Request('https://api.github.com' + endpoint, headers=headers)
    try:
        opener = urllib.request.build_opener(_NoRedirect())
        with opener.open(request, timeout=10) as response:
            data = response.read(2 * 1024 * 1024 + 1)
        if len(data) > 2 * 1024 * 1024:
            return 'github: resposta excede 2 MiB.'
        result = json.loads(data.decode('utf-8'))
        if kind == 'user' and isinstance(result, dict):
            return f"{result.get('login', '?')} — {result.get('name') or 'sem nome'}\n{result.get('html_url', '')}\nRepositórios públicos: {result.get('public_repos', '?')}"
        if kind == 'repo' and isinstance(result, dict):
            return (f"{result.get('full_name', '?')} — {result.get('description') or 'sem descrição'}\n"
                    f"{result.get('html_url', '')}\nBranch: {result.get('default_branch', '?')} | "
                    f"Estrelas: {result.get('stargazers_count', '?')} | Privado: {result.get('private', '?')}")
        if kind in ('issues', 'releases') and isinstance(result, list):
            if kind == 'issues':
                # GitHub includes pull requests in this endpoint; label them explicitly.
                lines = [f"#{x.get('number', '?')} {'PR' if 'pull_request' in x else 'issue'}: {x.get('title', '')} — {x.get('html_url', '')}" for x in result if isinstance(x, dict)]
            else:
                lines = [f"{x.get('tag_name', '?')}: {x.get('name') or ''} — {x.get('html_url', '')}" for x in result if isinstance(x, dict)]
            return '\n'.join(lines) if lines else 'Nenhum resultado.'
        return 'github: resposta inesperada da API.'
    except urllib.error.HTTPError as exc:
        return f'github: API retornou HTTP {exc.code} (verifique acesso, nome ou limite de requisições).'
    except (urllib.error.URLError, OSError, ValueError, UnicodeError):
        return 'github: falha de rede ou resposta inválida.'
