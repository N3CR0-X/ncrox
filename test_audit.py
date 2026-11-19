"""Offline tests: loopback sockets, local files and mocked OS resolver."""
import hashlib
import shutil
import socket
import ssl
import subprocess
import tempfile
import threading
import unittest
from pathlib import Path
from unittest.mock import patch

from satanachia import Shell


class AuditTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.shell = Shell()
        self.shell.cwd = self.root

    def tearDown(self):
        self.tmp.cleanup()

    def test_help_exposes_builtins(self):
        for cmd in ('vigia', 'oraculo', 'bastiao', 'selo', 'rede', 'base64', 'urlinfo', 'jsoncheck', 'comparar'):
            self.assertIn(cmd, self.shell.run('help'))

    def test_vigia_loopback_open_and_refused(self):
        with socket.socket() as listener, socket.socket() as closed:
            listener.bind(('127.0.0.1', 0)); listener.listen(1)
            closed.bind(('127.0.0.1', 0))  # Bound but not listening: deterministic local refusal.
            p1, p2 = listener.getsockname()[1], closed.getsockname()[1]
            result = self.shell.run(f'vigia 127.0.0.1 {p1},{p2},{p1}')
            self.assertIn(f'{p1}/tcp: aberta', result)
            self.assertEqual(result.count(f'{p1}/tcp:'), 1)
            self.assertIn(f'{p2}/tcp: recusada', result)
            conn, _ = listener.accept(); conn.close()

    def test_vigia_limits_and_single_address(self):
        with patch('ncrox_audit.socket.getaddrinfo') as dns, patch('ncrox_audit.socket.create_connection') as conn:
            for value in ('1-10', '0', '65536', ',80', ','.join(['1'] * 33)):
                self.assertIn('vigia:', self.shell.run('vigia localhost ' + value))
            self.assertIn('vigia:', self.shell.run('vigia a,b 80'))
            dns.assert_not_called(); conn.assert_not_called()
            dns.return_value = [(socket.AF_INET, socket.SOCK_STREAM, 6, '', ('127.0.0.1', 0)), (socket.AF_INET, socket.SOCK_STREAM, 6, '', ('127.0.0.2', 0))]
            conn.side_effect = [socket.timeout(), ConnectionRefusedError()]
            result = self.shell.run('vigia localhost 22,80')
            self.assertIn('apenas o primeiro', result)
            self.assertIn('timeout', result)
            self.assertIn('recusada', result)
            self.assertEqual([x.args[0] for x in conn.call_args_list], [('127.0.0.1', 22), ('127.0.0.1', 80)])
            self.assertTrue(all(x.kwargs['timeout'] == 1 for x in conn.call_args_list))
            self.assertEqual(dns.call_count, 1)

    def test_oraculo_mock_and_localhost(self):
        with patch('ncrox_audit.socket.getaddrinfo') as dns:
            dns.return_value = [(socket.AF_INET, socket.SOCK_STREAM, 6, '', ('127.0.0.1', 0)), (socket.AF_INET, socket.SOCK_STREAM, 6, '', ('127.0.0.1', 0)), (socket.AF_INET6, socket.SOCK_STREAM, 6, '', ('::1', 0, 0, 0))]
            result = self.shell.run('oraculo localhost')
            self.assertEqual(result.count('IPv4: 127.0.0.1'), 1)
            self.assertIn('IPv6: ::1', result)
            dns.side_effect = socket.gaierror('DNS indisponível')
            self.assertIn('DNS indisponível', self.shell.run('oraculo localhost'))
        self.assertIn('127.0.0.1', self.shell.run('oraculo 127.0.0.1'))

    def test_selo_and_comparar(self):
        data = b'abcdef' * 300000
        (self.root / 'a a').write_bytes(data)
        (self.root / 'b').write_bytes(data)
        digest = hashlib.sha256(data).hexdigest()
        self.assertIn(digest, self.shell.run('selo "a a"'))
        self.assertIn('CORRESPONDE', self.shell.run(f'selo "a a" {digest.upper()}'))
        self.assertIn('NÃO CORRESPONDE', self.shell.run('selo "a a" ' + '0' * 64))
        self.assertIn('64 caracteres', self.shell.run('selo b abc'))
        self.assertIn('não é um arquivo', self.shell.run('selo .'))
        self.assertIn('iguais', self.shell.run('comparar "a a" b'))
        (self.root / 'b').write_bytes(b'other')
        self.assertIn('diferentes', self.shell.run('comparar "a a" b'))

    def test_offline_utilities_and_errors(self):
        self.assertIn('rede 192.168.1.0/24', self.shell.run('rede 192.168.1.10/24'))
        self.assertIn('IPv6', self.shell.run('rede 2001:db8::1/64'))
        self.assertIn('rede:', self.shell.run('rede invalido'))
        self.assertEqual(self.shell.run('base64 codificar "olá mundo"'), 'b2zDoSBtdW5kbw==')
        self.assertEqual(self.shell.run('base64 decodificar b2zDoQ=='), 'olá')
        self.assertIn('base64:', self.shell.run('base64 decodificar !?'))
        self.assertIn('host=example.org', self.shell.run('urlinfo https://example.org/a?b=c'))
        self.assertIn('sem credenciais', self.shell.run('urlinfo https://user:pass@example.org'))
        self.assertIn('urlinfo:', self.shell.run('urlinfo https://example.org:99999'))
        (self.root / 'good.json').write_text('{"a":1}', encoding='utf-8')
        (self.root / 'bad.json').write_text('{x}', encoding='utf-8')
        (self.root / 'nan.json').write_text('NaN', encoding='utf-8')
        self.assertIn('JSON válido', self.shell.run('jsoncheck good.json'))
        self.assertIn('jsoncheck:', self.shell.run('jsoncheck bad.json'))
        self.assertIn('constante não JSON', self.shell.run('jsoncheck nan.json'))
        (self.root / 'large.json').write_bytes(b' ' * (2 * 1024 * 1024 + 1))
        self.assertIn('excede 2 MiB', self.shell.run('jsoncheck large.json'))

    @unittest.skipUnless(shutil.which('openssl'), 'teste TLS local requer openssl apenas para gerar certificado')
    def test_tls_local_validation_and_bad_cert(self):
        cert, key = self.root / 'cert.pem', self.root / 'key.pem'
        subprocess.run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-keyout', str(key), '-out', str(cert), '-days', '1', '-subj', '/CN=localhost', '-addext', 'subjectAltName=DNS:localhost,IP:127.0.0.1'], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        server_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
        server_context.load_cert_chain(cert, key)

        def local_server():
            listener = socket.socket()
            listener.bind(('127.0.0.1', 0)); listener.listen(1); listener.settimeout(5)
            port = listener.getsockname()[1]
            def serve():
                try:
                    conn, _ = listener.accept()
                    with conn:
                        try:
                            with server_context.wrap_socket(conn, server_side=True):
                                pass
                        except ssl.SSLError:
                            pass  # Client rejects untrusted/incorrect certificate.
                finally:
                    listener.close()
            thread = threading.Thread(target=serve, daemon=True); thread.start()
            return port, thread

        # Real default trust rejects our untrusted self-signed fixture.
        port, thread = local_server()
        result = self.shell.run(f'bastiao 127.0.0.1 {port}')
        thread.join(5)
        self.assertIn('certificado NÃO validado', result)
        self.assertFalse(thread.is_alive())
        # Explicitly trusted only inside this test; still validates matching IP.
        context = ssl.create_default_context(cafile=str(cert))
        port, thread = local_server()
        with patch('ncrox_audit.ssl.create_default_context', return_value=context):
            result = self.shell.run(f'bastiao 127.0.0.1 {port}')
        thread.join(5)
        self.assertIn('certificado validado', result)
        self.assertIn('TLS:', result)
        self.assertIn('IP Address:127.0.0.1', result)
        self.assertFalse(thread.is_alive())
        # Same trusted cert, but hostname does not match. Route connection to loopback.
        port, thread = local_server()
        real_connect = socket.create_connection
        def connect_local(_address, timeout):
            return real_connect(('127.0.0.1', port), timeout=timeout)
        with patch('ncrox_audit.ssl.create_default_context', return_value=context), patch('ncrox_audit.socket.create_connection', side_effect=connect_local):
            result = self.shell.run(f'bastiao wrong.test {port}')
        thread.join(5)
        self.assertIn('certificado NÃO validado', result)
        self.assertFalse(thread.is_alive())

    def test_bastiao_invalid_and_failures(self):
        with patch('ncrox_audit.socket.create_connection') as conn:
            for arg in ('a,b', '0', '65536'):
                self.assertIn('bastiao:', self.shell.run('bastiao localhost ' + arg))
            conn.assert_not_called()
            conn.side_effect = socket.timeout('tempo esgotado')
            self.assertIn('tempo esgotado', self.shell.run('bastiao localhost 443'))


if __name__ == '__main__':
    unittest.main()
