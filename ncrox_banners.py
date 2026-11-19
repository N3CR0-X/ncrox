"""UTF-8 artwork for audit builtins. Missing artwork is silently ignored."""
import re
import sys
from pathlib import Path

COMMANDS = frozenset(('vigia', 'oraculo', 'bastiao', 'selo'))
ANSI = re.compile(r'\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)|\x1b\[[0-?]*[ -/]*[@-~]|\x1b[@-_]')
SGR = re.compile(r'\x1b\[[0-9;:]*m')
BANNER_DIR = Path(getattr(sys, '_MEIPASS', Path(__file__).resolve().parent)) / 'banners'


def banner(command, color=False):
    if command not in COMMANDS:
        return ''
    try:
        art = (BANNER_DIR / (command + '.txt')).read_text(encoding='utf-8')
    except (OSError, UnicodeError):
        return ''
    if not art:
        return ''
    if not color:
        return ANSI.sub('', art)
    # Preserve only SGR sequences (including truecolor), not cursor/OSC controls.
    parts, codes = SGR.split(art), SGR.findall(art)
    art = ''.join(ANSI.sub('', p) + (codes[i] if i < len(codes) else '')
                  for i, p in enumerate(parts))
    return art + '\x1b[0m'
