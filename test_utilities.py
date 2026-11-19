import hashlib
import io
import json
import os
import shutil
import socket
import subprocess
import uuid
import tempfile
import unittest
import urllib.error
from pathlib import Path
from unittest.mock import patch

from satanachia import Shell
from ncrox_tools import github, _NoRedirect


class UtilityTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.shell = Shell()
        self.shell.cwd = self.root

    def tearDown(self):
        self.tmp.cleanup()

    def test_hash_binary_and_missing(self):
        data = b'\x00abc' * 1000000
        (self.root / 'blob').write_bytes(data)
        self.assertIn(hashlib.sha256(data).hexdigest(), self.shell.run('hash blob'))
        self.assertIn('hash:', self.shell.run('hash absent'))
        self.assertIn('Uso:', self.shell.run('hash'))

    def test_disk_sysinfo_and_which(self):
        self.assertIn('livre=', self.shell.run('disk .'))
        self.assertIn('Sistema:', self.shell.run('sysinfo'))
        self.assertIn('não encontrado', self.shell.run('which surely_missing_12345'))
        self.assertIn('python', self.shell.run('where python'))

    def test_fileinfo_uuid_and_tcp(self):
        (self.root / 'file').write_bytes(b'abc')
        self.assertIn('3 bytes', self.shell.run('fileinfo file'))
        self.assertEqual(uuid.UUID(self.shell.run('uuid')).version, 4)
        self.assertIn('Uso:', self.shell.run('netcheck localhost 65536'))
        with socket.socket() as listener:
            listener.bind(('127.0.0.1', 0))
            listener.listen(1)
            port = listener.getsockname()[1]
            self.assertIn('TCP conectado', self.shell.run(f'netcheck 127.0.0.1 {port}'))
            connection, _ = listener.accept()
            connection.close()

    def test_findtext_utf8_literal(self):
        (self.root / 'arquivo com espaco').write_text('olá mundo\nsem\nolá + mundo\n', encoding='utf8')
        self.assertEqual(self.shell.run('findtext "olá +" "arquivo com espaco"'), '3: olá + mundo')
        self.assertEqual(self.shell.run('findtext nada "arquivo com espaco"'), 'Nenhuma ocorrência.')
        (self.root / 'bad').write_bytes(b'\xff')
        self.assertIn('findtext:', self.shell.run('findtext x bad'))

    def test_relative_tree_after_cd(self):
        (self.root / 'a' / 'b').mkdir(parents=True)
        (self.root / 'a' / 'b' / 'file').touch()
        self.shell.run('cd a')
        self.assertIn('file', self.shell.run('tree b'))

    @unittest.skipIf(os.name == 'nt', 'POSIX shell test')
    def test_pipe_redirection_real_ls_and_exit(self):
        (self.root / 'file').touch()
        self.assertIn('file', self.shell.run('ls'))
        self.assertEqual(self.shell.run('printf abc | tr a-z A-Z'), 'ABC')
        self.shell.run('printf x > output')
        self.assertEqual((self.root / 'output').read_text(), 'x')
        self.assertIn('Código de saída: 7', self.shell.run('printf bad; exit 7'))
        self.assertEqual(self.shell.run('printf "a|b"'), 'a|b')

    @unittest.skipIf(os.name == 'nt', 'POSIX shell test')
    def test_termux_prefix_shell_and_path_preserved(self):
        prefix = self.root / 'prefix'
        (prefix / 'bin').mkdir(parents=True)
        (prefix / 'bin' / 'sh').symlink_to(shutil.which('sh'))
        command = prefix / 'bin' / 'ncrox_only_test'
        command.write_text('#!/bin/sh\nprintf preserved', encoding='utf8')
        command.chmod(0o755)
        with patch.dict(os.environ, {'PREFIX': str(prefix), 'PATH': str(prefix / 'bin') + os.pathsep + os.environ['PATH']}):
            self.assertEqual(self.shell.run('ncrox_only_test'), 'preserved')
            with patch('satanachia.subprocess.run', wraps=subprocess.run) as runner:
                self.shell.run('printf ok')
                self.assertEqual(runner.call_args.args[0][0], str(prefix / 'bin' / 'sh'))
                self.assertEqual(runner.call_args.args[0][1], '-c')

    def test_pick_arguments_and_injection(self):
        with patch.object(self.shell, '_exec', return_value='done') as exe:
            self.assertEqual(self.shell.run('pick user/repo dest'), 'done')
            exe.assert_called_once_with(['git', 'clone', '--', 'https://github.com/user/repo.git', 'dest'])
        self.assertIn('opções', self.shell.run('pick --upload-pack=evil'))
        self.assertIn('use usuario/repo', self.shell.run('pick ext::evil'))


class FakeResponse(io.BytesIO):
    pass


class GithubTests(unittest.TestCase):
    def api(self, data, args, env=None):
        response = FakeResponse(json.dumps(data).encode())
        with patch.dict(os.environ, env or {}, clear=True), patch('ncrox_tools.urllib.request.build_opener') as build:
            build.return_value.open.return_value = response
            result = github(args)
            request = build.return_value.open.call_args.args[0]
            return result, request, build

    def test_repo_auth_and_no_redirect_handler(self):
        result, req, build = self.api({'full_name':'owner/repo','description':'test','html_url':'https://github.com/owner/repo','default_branch':'main','stargazers_count':12,'private':False}, ['repo','owner/repo'], {'GH_TOKEN':'secret'})
        self.assertIn('owner/repo', result)
        self.assertNotIn('secret', result)
        self.assertEqual(req.full_url, 'https://api.github.com/repos/owner/repo')
        self.assertEqual(req.get_header('Authorization'), 'Bearer secret')
        self.assertIsInstance(build.call_args.args[0], _NoRedirect)
        self.assertIsNone(_NoRedirect().redirect_request(req, None, 302, '', {}, 'https://evil.example'))

    def test_issues_includes_pr_label_and_page_limit(self):
        result, req, _ = self.api([{'number':1,'title':'bug','html_url':'url'}, {'number':2,'title':'change','pull_request':{},'html_url':'url'}], ['issues', 'a/b'])
        self.assertIn('#1 issue', result)
        self.assertIn('#2 PR', result)
        self.assertIn('per_page=20', req.full_url)
        self.assertIsNone(req.get_header('Authorization'))

    def test_user_release_and_empty(self):
        self.assertIn('alice', self.api({'login':'alice','public_repos':2}, ['user', 'alice'])[0])
        self.assertIn('v1', self.api([{'tag_name':'v1'}], ['releases', 'a/b'])[0])
        self.assertEqual(self.api([], ['issues', 'a/b'])[0], 'Nenhum resultado.')

    def test_invalid_urls_rejected_before_network(self):
        with patch('ncrox_tools.urllib.request.build_opener') as build:
            for arg in ['../../evil', 'a/b?x=1', 'a/b/c', 'a/..']:
                self.assertIn('github:', github(['repo', arg]))
            build.assert_not_called()
        self.assertIn('inválido', github(['user', 'evil.com']))

    def test_errors_do_not_expose_token_or_server_body(self):
        error = urllib.error.HTTPError('https://api.github.com', 403, 'secret', {}, None)
        with patch.dict(os.environ, {'GH_TOKEN':'secret'}, clear=True), patch('ncrox_tools.urllib.request.build_opener') as build:
            build.return_value.open.side_effect = error
            result = github(['repo', 'a/b'])
            self.assertIn('403', result)
            self.assertNotIn('secret', result)

    def test_oversize_and_bad_json(self):
        response = FakeResponse(b'a' * (2 * 1024 * 1024 + 1))
        with patch.dict(os.environ, {}, clear=True), patch('ncrox_tools.urllib.request.build_opener') as build:
            build.return_value.open.return_value = response
            self.assertIn('2 MiB', github(['repo', 'a/b']))
            build.return_value.open.return_value = FakeResponse(b'no json')
            self.assertIn('inválida', github(['repo', 'a/b']))


if __name__ == '__main__':
    unittest.main()
