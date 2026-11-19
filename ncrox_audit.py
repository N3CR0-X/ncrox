"""Auditoria autorizada e utilidades offline para Satanachia ncrox.

Somente a biblioteca padrão. Consultas de rede são iniciadas apenas por comando.
"""
import base64
import binascii
import hashlib
import hmac
import ipaddress
import json
import re
import socket
import ssl
from pathlib import Path
from urllib.parse import urlsplit

DEFAULT_PORTS = (22, 80, 443, 8080)
MAX_PORTS = 32
TCP_TIMEOUT = 1.0
TLS_TIMEOUT = 5.0


def _path(name, cwd):
    path = Path(name).expanduser()
    return path if path.is_absolute() else cwd / path


def _digest(path):
    # Reject FIFOs, device files and directories to avoid unbounded reads.
    if not path.is_file():
        raise ValueError('não é um arquivo regular ou não existe')
    sha = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            sha.update(block)
    return sha.hexdigest()


def _host(host):
    # Only one hostname or IP; no URI, list, CIDR, or options.
    if not host or len(host) > 253 or any(c.isspace() for c in host):
        raise ValueError('host inválido')
    try:
        return str(ipaddress.ip_address(host))
    except ValueError:
        pass
    if not re.fullmatch(r'(?=.{1,253}\.?$)(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)*[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.?', host):
        raise ValueError('use um único hostname ou IP, sem URL, intervalo ou lista')
    return host


def _ports(text):
    parts = text.split(',')
    if len(parts) > MAX_PORTS or not all(re.fullmatch(r'[0-9]{1,5}', part) for part in parts):
        raise ValueError('portas devem ser números separados por vírgulas, no máximo 32')
    ports = [int(p) for p in parts]
    if any(not 1 <= p <= 65535 for p in ports):
        raise ValueError('porta fora do intervalo 1–65535')
    return list(dict.fromkeys(ports))


def _resolve(host):
    # Deduplicate addresses and preserve resolver order. getaddrinfo uses the OS resolver.
    found = []
    for family, _, _, _, address in socket.getaddrinfo(host, None, type=socket.SOCK_STREAM):
        if family in (socket.AF_INET, socket.AF_INET6):
            ip = address[0]
            if ip not in found:
                found.append(ip)
    if not found:
        raise OSError('nenhum endereço IPv4/IPv6 retornado')
    return found


def _vigia(args):
    if not 1 <= len(args) <= 2:
        return 'Uso: vigia <host> [portas separadas por vírgula]'
    host = _host(args[0])
    ports = _ports(args[1]) if len(args) == 2 else list(DEFAULT_PORTS)
    addresses = _resolve(host)
    ip = addresses[0]  # Pin one address throughout, even for multi-address DNS.
    lines = [f'vigia: {host} → {ip} | TCP | até {MAX_PORTS} portas | somente uso autorizado']
    if len(addresses) > 1:
        lines.append('DNS retornou mais de um endereço; apenas o primeiro será verificado.')
    for port in ports:
        try:
            with socket.create_connection((ip, port), timeout=TCP_TIMEOUT):
                lines.append(f'{port}/tcp: aberta (conexão estabelecida)')
        except ConnectionRefusedError:
            lines.append(f'{port}/tcp: recusada')
        except (socket.timeout, TimeoutError):
            lines.append(f'{port}/tcp: sem resposta (timeout)')
        except OSError as exc:
            lines.append(f'{port}/tcp: erro de conexão ({exc})')
    return '\n'.join(lines)


def _oraculo(args):
    if len(args) != 1:
        return 'Uso: oraculo <domínio>'
    host = _host(args[0])
    addresses = _resolve(host)
    return f'oraculo: {host}\n' + '\n'.join(f'{"IPv6" if ":" in ip else "IPv4"}: {ip}' for ip in addresses)


def _name_tuple(name):
    return ', '.join(f'{key}={value}' for rdn in name for key, value in rdn) or '(não informado)'


def _bastiao(args):
    if not 1 <= len(args) <= 2:
        return 'Uso: bastiao <host> [porta]'
    host = _host(args[0])
    ports = _ports(args[1]) if len(args) == 2 else [443]
    if len(ports) != 1 or (len(args) == 2 and ',' in args[1]):
        raise ValueError('informe apenas uma porta')
    port = ports[0]
    # Default context validates certificate chain, validity and hostname/IP.
    context = ssl.create_default_context()
    with socket.create_connection((host, port), timeout=TLS_TIMEOUT) as connection:
        with context.wrap_socket(connection, server_hostname=host) as secure:
            cert = secure.getpeercert()
            cipher = secure.cipher()
            sans = ', '.join(f'{kind}:{value}' for kind, value in cert.get('subjectAltName', ())) or '(não informado)'
            return (f'bastiao: {host}:{port} | certificado validado pelo sistema\n'
                    f'TLS: {secure.version()} | cifra: {cipher[0] if cipher else "?"}\n'
                    f'Sujeito: {_name_tuple(cert.get("subject", ()))}\n'
                    f'Emissor: {_name_tuple(cert.get("issuer", ()))}\n'
                    f'Válido de: {cert.get("notBefore", "?")} até: {cert.get("notAfter", "?")}\n'
                    f'SAN: {sans}')


def _selo(args, cwd):
    if not 1 <= len(args) <= 2:
        return 'Uso: selo <arquivo> [sha256]'
    if len(args) == 2 and not re.fullmatch(r'[0-9a-fA-F]{64}', args[1]):
        return 'selo: SHA-256 esperado deve ter 64 caracteres hexadecimais.'
    path = _path(args[0], cwd)
    digest = _digest(path)
    result = f'selo: SHA-256 {digest}  {path}'
    if len(args) == 2:
        result += '\n' + ('CORRESPONDE ao valor informado.' if hmac.compare_digest(digest, args[1].lower()) else 'NÃO CORRESPONDE ao valor informado.')
    return result


def command(cmd, args, cwd):
    """Output for built-ins or None; all errors are exposed as readable text."""
    if cmd not in ('vigia', 'oraculo', 'bastiao', 'selo', 'rede', 'base64', 'urlinfo', 'jsoncheck', 'comparar'):
        return None
    try:
        if args in (['--help'], ['-h']):
            help_text = {
                'vigia': 'Uso: vigia <host> [porta,porta,...] | até 32 portas TCP em um host autorizado.',
                'oraculo': 'Uso: oraculo <domínio> | resolve endereços IP pelo sistema.',
                'bastiao': 'Uso: bastiao <host> [porta] | verifica TLS e certificado.',
                'selo': 'Uso: selo <arquivo> [sha256] | calcula e compara SHA-256.',
                'rede': 'Uso: rede <IP/CIDR>',
                'base64': 'Uso: base64 codificar|decodificar <texto UTF-8>',
                'urlinfo': 'Uso: urlinfo <URL http ou https>',
                'jsoncheck': 'Uso: jsoncheck <arquivo UTF-8>',
                'comparar': 'Uso: comparar <arquivo1> <arquivo2>',
            }
            return help_text[cmd]
        if cmd == 'vigia':
            return _vigia(args)
        if cmd == 'oraculo':
            return _oraculo(args)
        if cmd == 'bastiao':
            return _bastiao(args)
        if cmd == 'selo':
            return _selo(args, cwd)
        if cmd == 'rede':
            if len(args) != 1:
                return 'Uso: rede <IP/CIDR>'
            interface = ipaddress.ip_interface(args[0])
            net = interface.network
            return (f'rede: IPv{net.version} | IP {interface.ip} | rede {net} | '
                    f'máscara {net.netmask} | endereços {net.num_addresses} | último {net.broadcast_address}')
        if cmd == 'base64':
            if len(args) != 2 or args[0] not in ('codificar', 'decodificar'):
                return 'Uso: base64 codificar|decodificar <texto UTF-8>'
            if args[0] == 'codificar':
                return base64.b64encode(args[1].encode('utf-8')).decode('ascii')
            return base64.b64decode(args[1], validate=True).decode('utf-8')
        if cmd == 'urlinfo':
            if len(args) != 1:
                return 'Uso: urlinfo <URL http ou https>'
            url = urlsplit(args[0])
            if url.scheme not in ('http', 'https') or not url.hostname or url.username is not None:
                return 'urlinfo: informe URL HTTP(S) válida sem credenciais.'
            port = url.port  # validates numeric range
            return (f'urlinfo: esquema={url.scheme} | host={url.hostname} | '
                    f'porta={port or (443 if url.scheme == "https" else 80)}\n'
                    f'caminho={url.path or "/"} | consulta={url.query or "(vazia)"} | fragmento={url.fragment or "(vazio)"}\nSem requisição de rede.')
        if cmd == 'jsoncheck':
            if len(args) != 1:
                return 'Uso: jsoncheck <arquivo UTF-8>'
            path = _path(args[0], cwd)
            if not path.is_file():
                raise ValueError('não é um arquivo regular ou não existe')
            with path.open('rb') as stream:
                data = stream.read(2 * 1024 * 1024 + 1)
            if len(data) > 2 * 1024 * 1024:
                return 'jsoncheck: arquivo excede 2 MiB.'
            parsed = json.loads(data.decode('utf-8'), parse_constant=lambda v: (_ for _ in ()).throw(ValueError(f'constante não JSON: {v}')))
            return f'jsoncheck: JSON válido | tipo raiz: {type(parsed).__name__} | {len(data)} bytes'
        if len(args) != 2:
            return 'Uso: comparar <arquivo1> <arquivo2> (SHA-256)'
        first, second = (_digest(_path(name, cwd)) for name in args)
        return 'comparar: SHA-256 ' + ('iguais' if hmac.compare_digest(first, second) else 'diferentes') + f'\n{first}  {args[0]}\n{second}  {args[1]}'
    except ssl.SSLCertVerificationError as exc:
        return f'{cmd}: certificado NÃO validado ({exc.verify_message}); conexão recusada.'
    except (OSError, ValueError, UnicodeError, binascii.Error, RecursionError) as exc:
        return f'{cmd}: {exc}'
