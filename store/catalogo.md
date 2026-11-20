# Catálogo de ferramentas da Satanachia Store

A Store contém **100 ZIPs individuais**. Cada ZIP contém `install.sh` e `LEIA-ME.md`; baixar/extrair não executa nem instala o programa. Execute o script somente depois de ler as instruções e aprovar. Os pacotes vêm dos repositórios configurados no sistema ou do índice pip usado pelo Python.

## Ambientes

- **Termux:** usa `pkg` e nomes de pacotes verificados contra as receitas oficiais do repositório Termux.
- **Debian/Parrot:** usa `apt`; alguns pacotes podem não existir em todos os releases/repositórios.
- **Arch:** usa `pacman -Syu`; isso pode atualizar o sistema junto. O pacote pode não estar disponível em todos os repositórios/arquiteturas.
- Bibliotecas Python via pip instalam em `~/.venvs/ncrox-store-python`, não no Python global.
- Esses scripts rodam no terminal do sistema onde estão executados; instalar algo em Termux não disponibiliza automaticamente no APK Satanachia. O APK não pode bootstrapar Termux ou instalar imagens completas de Debian/Parrot/Arch.

## Como usar no Satanachia

```text
satanachia -store
satanachia -pick <nome-do-pacote>
satanachia -st <nome-do-pacote>
```

Abra a pasta extraída no Termux/Debian/Parrot/Arch, confira `LEIA-ME.md` e, se confiar, rode `bash install.sh`. O formato atual usa senha convencional `<nome-do-pacote>ncrox`; não considere isso uma verificação de autenticidade. Confira hashes por canal confiável antes de executar pacotes.

## Pacotes

### Python e ciência

- `python` — [ZIP](./python.zip)
- `python-pip` — [ZIP](./python-pip.zip)
- `python-cryptography` — [ZIP](./python-cryptography.zip)
- `python-numpy` — [ZIP](./python-numpy.zip)
- `python-pandas` — [ZIP](./python-pandas.zip)
- `python-scipy` — [ZIP](./python-scipy.zip)
- `python-lxml` — [ZIP](./python-lxml.zip)
- `python-pillow` — [ZIP](./python-pillow.zip)
- `beautifulsoup4` — [ZIP](./beautifulsoup4.zip)
- `requests` — [ZIP](./requests.zip)
- `rich` — [ZIP](./rich.zip)
- `flask` — [ZIP](./flask.zip)
- `django` — [ZIP](./django.zip)
- `fastapi` — [ZIP](./fastapi.zip)
- `uvicorn` — [ZIP](./uvicorn.zip)
- `pytest` — [ZIP](./pytest.zip)
- `black` — [ZIP](./black.zip)
- `ruff` — [ZIP](./ruff.zip)
- `mypy` — [ZIP](./mypy.zip)
- `ipython` — [ZIP](./ipython.zip)
- `jupyterlab` — [ZIP](./jupyterlab.zip)
- `matplotlib` — [ZIP](./matplotlib.zip)

### Git, linguagens e compiladores

- `git` — [ZIP](./git.zip)
- `gh` — [ZIP](./gh.zip)
- `git-lfs` — [ZIP](./git-lfs.zip)
- `tig` — [ZIP](./tig.zip)
- `lazygit` — [ZIP](./lazygit.zip)
- `golang` — [ZIP](./golang.zip)
- `nodejs` — [ZIP](./nodejs.zip)
- `ruby` — [ZIP](./ruby.zip)
- `perl` — [ZIP](./perl.zip)
- `php` — [ZIP](./php.zip)
- `rust` — [ZIP](./rust.zip)
- `make` — [ZIP](./make.zip)
- `cmake` — [ZIP](./cmake.zip)
- `ninja` — [ZIP](./ninja.zip)
- `autoconf` — [ZIP](./autoconf.zip)
- `automake` — [ZIP](./automake.zip)
- `libtool` — [ZIP](./libtool.zip)
- `binutils` — [ZIP](./binutils.zip)
- `pkg-config` — [ZIP](./pkg-config.zip)
- `gdb` — [ZIP](./gdb.zip)
- `rust-analyzer` — [ZIP](./rust-analyzer.zip)
- `openjdk-17` — [ZIP](./openjdk-17.zip)
- `openjdk-21` — [ZIP](./openjdk-21.zip)
- `maven` — [ZIP](./maven.zip)
- `gradle` — [ZIP](./gradle.zip)
- `kotlin` — [ZIP](./kotlin.zip)

### Editores e shell

- `nano` — [ZIP](./nano.zip)
- `vim` — [ZIP](./vim.zip)
- `neovim` — [ZIP](./neovim.zip)
- `micro` — [ZIP](./micro.zip)
- `helix` — [ZIP](./helix.zip)
- `zsh` — [ZIP](./zsh.zip)
- `fish` — [ZIP](./fish.zip)
- `bash` — [ZIP](./bash.zip)
- `tmux` — [ZIP](./tmux.zip)
- `screen` — [ZIP](./screen.zip)
- `starship` — [ZIP](./starship.zip)
- `direnv` — [ZIP](./direnv.zip)
- `shellcheck` — [ZIP](./shellcheck.zip)
- `shfmt` — [ZIP](./shfmt.zip)

### CLI e arquivos

- `tree` — [ZIP](./tree.zip)
- `jq` — [ZIP](./jq.zip)
- `yq` — [ZIP](./yq.zip)
- `ripgrep` — [ZIP](./ripgrep.zip)
- `fd` — [ZIP](./fd.zip)
- `fzf` — [ZIP](./fzf.zip)
- `bat` — [ZIP](./bat.zip)
- `eza` — [ZIP](./eza.zip)
- `zoxide` — [ZIP](./zoxide.zip)
- `tealdeer` — [ZIP](./tealdeer.zip)
- `less` — [ZIP](./less.zip)
- `htop` — [ZIP](./htop.zip)
- `procs` — [ZIP](./procs.zip)
- `coreutils` — [ZIP](./coreutils.zip)
- `findutils` — [ZIP](./findutils.zip)
- `grep` — [ZIP](./grep.zip)
- `sed` — [ZIP](./sed.zip)
- `gawk` — [ZIP](./gawk.zip)
- `diffutils` — [ZIP](./diffutils.zip)
- `patch` — [ZIP](./patch.zip)
- `dos2unix` — [ZIP](./dos2unix.zip)
- `parallel` — [ZIP](./parallel.zip)
- `moreutils` — [ZIP](./moreutils.zip)
- `procps` — [ZIP](./procps.zip)
- `util-linux` — [ZIP](./util-linux.zip)
- `ncdu` — [ZIP](./ncdu.zip)
- `dust` — [ZIP](./dust.zip)
- `which` — [ZIP](./which.zip)

### Rede geral e sincronização

- `openssh` — [ZIP](./openssh.zip)
- `wget` — [ZIP](./wget.zip)
- `rsync` — [ZIP](./rsync.zip)
- `rclone` — [ZIP](./rclone.zip)
- `aria2` — [ZIP](./aria2.zip)

### Arquivos comprimidos e conteúdo

- `unzip` — [ZIP](./unzip.zip)
- `zip` — [ZIP](./zip.zip)
- `tar` — [ZIP](./tar.zip)
- `gzip` — [ZIP](./gzip.zip)
- `7zip` — [ZIP](./7zip.zip)

Pacotes publicados:
`git`, `gh`, `python`, `python-pip`, `python-cryptography`, `python-numpy`, `python-pandas`, `python-scipy`, `python-lxml`, `python-pillow`, `nano`, `vim`, `neovim`, `micro`, `helix`, `git-lfs`, `tig`, `lazygit`, `golang`, `nodejs`, `ruby`, `perl`, `php`, `rust`, `make`, `cmake`, `ninja`, `autoconf`, `automake`, `libtool`, `binutils`, `pkg-config`, `gdb`, `rust-analyzer`, `openjdk-17`, `openjdk-21`, `maven`, `gradle`, `kotlin`, `htop`, `procs`, `tree`, `jq`, `yq`, `ripgrep`, `fd`, `fzf`, `bat`, `eza`, `zoxide`, `tealdeer`, `less`, `tmux`, `screen`, `zsh`, `fish`, `bash`, `starship`, `direnv`, `shellcheck`, `shfmt`, `openssh`, `wget`, `rsync`, `rclone`, `aria2`, `unzip`, `zip`, `tar`, `gzip`, `7zip`, `which`, `coreutils`, `findutils`, `grep`, `sed`, `gawk`, `diffutils`, `patch`, `dos2unix`, `parallel`, `moreutils`, `procps`, `util-linux`, `ncdu`, `dust`, `beautifulsoup4`, `requests`, `rich`, `flask`, `django`, `fastapi`, `uvicorn`, `pytest`, `black`, `ruff`, `mypy`, `ipython`, `jupyterlab`, `matplotlib`
