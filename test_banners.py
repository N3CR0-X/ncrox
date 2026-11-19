import io
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import ncrox_banners
from satanachia import Shell, cli


class BannerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.patcher = patch.object(ncrox_banners, 'BANNER_DIR', self.root)
        self.patcher.start()
        self.addCleanup(self.patcher.stop)
        self.art = '\x1b[38;2;255;42;0m  ╭─á▀  \x1b[0m\n\x1b[38;2;1;2;3m └─終\x1b[0m\n'
        for name in ncrox_banners.COMMANDS:
            (self.root / (name + '.txt')).write_text(self.art, encoding='utf-8')

    def test_plain_all_commands_and_help(self):
        shell = Shell()
        for name in ncrox_banners.COMMANDS:
            result = shell.run(name + ' --help')
            self.assertTrue(result.startswith('  ╭─á▀  \n └─終\n'), repr(result))
            self.assertNotIn('\x1b', result)
        self.assertFalse(shell.run('help').startswith('  ╭'))

    def test_tty_keeps_truecolor_and_resets(self):
        result = Shell(color_banners=True).run('vigia --help')
        self.assertTrue(result.startswith(self.art))
        self.assertIn('\x1b[0m\n', result[len(self.art):])

    def test_no_banner_invalid_utf8_missing_and_cwd(self):
        (self.root / 'vigia.txt').write_bytes(b'\xff')
        (self.root / 'selo.txt').unlink()
        self.assertFalse(Shell().run('vigia --help').startswith('  ╭'))
        self.assertFalse(Shell().run('selo --help').startswith('  ╭'))
        with tempfile.TemporaryDirectory() as cwd, patch('pathlib.Path.cwd', return_value=Path(cwd)):
            self.assertTrue(Shell().run('ORACULO --help').startswith('  ╭'))

    def test_shell_operators_not_intercepted(self):
        shell = Shell()
        self.assertEqual(shell.run("printf 'vigia' | tr a-z A-Z"), 'VIGIA')
        self.assertEqual(shell.run('printf "selo | teste"'), 'selo | teste')

    def test_other_terminal_controls_are_removed(self):
        (self.root / 'selo.txt').write_text('\x1b[2J\x1b]0;title\x07olé\x1b[38;2;2;3;4mX', encoding='utf8')
        self.assertEqual(ncrox_banners.banner('selo'), 'oléX')
        colored = ncrox_banners.banner('selo', True)
        self.assertNotIn('\x1b[2J', colored)
        self.assertNotIn('title', colored)
        self.assertIn('\x1b[38;2;2;3;4m', colored)

    def test_cli_tty_and_redirected(self):
        class Stream(io.StringIO):
            def __init__(self, tty): super().__init__(); self.tty = tty
            def isatty(self): return self.tty
        for tty in (False, True):
            stream = Stream(tty)
            with patch('sys.stdout', stream), patch('builtins.input', side_effect=['vigia --help', EOFError]):
                cli()
            self.assertEqual('\x1b[38;2;255;42;0m' in stream.getvalue(), tty)
            self.assertIn('╭─á▀', stream.getvalue())


if __name__ == '__main__': unittest.main()
