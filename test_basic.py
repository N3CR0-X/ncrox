import os, tempfile, unittest
from unittest.mock import patch
from pathlib import Path
from satanachia import Shell

class BasicTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.old = Path.cwd()
        self.shell = Shell()
        self.shell.cwd = Path(self.tmp.name)
    def tearDown(self):
        os.chdir(self.old)
        self.tmp.cleanup()
    def test_make_and_tree(self):
        self.assertIn('Pasta criada', self.shell.run('make -p a/b'))
        self.assertTrue((Path(self.tmp.name) / 'a/b').is_dir())
        self.assertIn('a/', self.shell.run('tree'))
    def test_readmd_prints_raw_text(self):
        path = Path(self.tmp.name) / 'azure.md'
        path.write_text('# Azure\n\nÉ uma ferramenta.', encoding='utf-8')
        self.assertEqual(self.shell.run('readmd azure.md'), '# Azure\n\nÉ uma ferramenta.')
        self.assertIn('extensão .md', self.shell.run('readmd azure.txt'))
        self.assertIn('Uso: readmd', self.shell.run('readmd'))
    def test_docs_lists_extension(self):
        (Path(self.tmp.name) / 'archive.zip').write_bytes(b'zip')
        self.assertIn('archive.zip', self.shell.run('docs -zip'))
    def test_store_empty_and_lookup_case_insensitive(self):
        with patch.object(self.shell, '_store_files', return_value=[]):
            self.assertEqual(self.shell.run('satanachia -store'), 'loja vazia')
            self.assertEqual(self.shell.run('satanachia -pick Azure'), 'loja vazia')
    def test_tasklist_requires_confirmation(self):
        self.assertIn('(s/n)', self.shell.run('tasklist -sudozi'))
        with patch.object(self.shell, '_process_list', return_value='process list'):
            self.assertEqual(self.shell.run('n'), 'Operação cancelada.')
            self.shell.run('tasklist -sudozi')
            self.assertEqual(self.shell.run('s'), 'process list')
    def test_real_shell(self):
        out = self.shell.run('python3 -c "print(123)"')
        self.assertIn('123', out)
    def test_pick_help_without_network(self):
        self.assertIn('Uso: pick', self.shell.run('pick'))
    def test_net_alias(self):
        self.assertIn('interfaces locais', self.shell.run('net unknown'))

if __name__ == '__main__':
    unittest.main()
