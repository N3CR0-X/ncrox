#!/usr/bin/env bash
# Build GNU Bash for Android ABIs supported by the Satanachia APK.
set -Eeuo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
APP="$ROOT/app"
NDK="${ANDROID_NDK_HOME:-${ANDROID_NDK:-}}"
[[ -n "$NDK" && -d "$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin" ]] || { echo 'Defina ANDROID_NDK_HOME para um Android NDK Linux.' >&2; exit 1; }
HOST_TAG=linux-x86_64
TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/$HOST_TAG/bin"
WORK="${RUNNER_TEMP:-${TMPDIR:-/tmp}}/ncrox-bash-build"
mkdir -p "$WORK" "$APP/src/main/jniLibs"
cd "$WORK"
GNU_BASH_VER=5.3
TARBALL="bash-${GNU_BASH_VER}.tar.gz"
curl --fail --location --silent --show-error "https://ftp.gnu.org/gnu/bash/$TARBALL" -o "$TARBALL"
echo '0d5cd86965f869a26cf64f4b71be7b96f90a3ba8b3d74e27e8e9d9d5550f31ba  bash-5.3.tar.gz' | sha256sum --check --status || { echo 'Checksum do GNU Bash não confere.' >&2; exit 1; }
mkdir -p patches
for n in $(seq -w 1 20); do
  patchfile="bash53-0${n}"
  curl --fail --location --silent --show-error "https://ftp.gnu.org/gnu/bash/bash-5.3-patches/$patchfile" -o "patches/$patchfile"
done
cat > patches.sha256 <<'SUMS'
1f608434364af86b9b45c8b0ea3fb3b165fb830d27697e6cdfc7ac17dee3287f  patches/bash53-001
e385548a00130765ec7938a56fbdca52447ab41fabc95a25f19ade527e282001  patches/bash53-002
f245d9c7dc3f5a20d84b53d249334747940936f09dc97e1dcb89fc3ab37d60ed  patches/bash53-003
9591d245045529f32f0812f94180b9d9ce9023f5a765c039b852e5dfc99747d0  patches/bash53-004
cca1ef52dbbf433bc98e33269b64b2c814028efe2538be1e2c9a377da90bc99d  patches/bash53-005
29119addefed8eff91ae37fd51822c31780ee30d4a28376e96002706c995ff10  patches/bash53-006
c0976bbfffa1453c7cfdd62058f206a318568ff2d690f5d4fa048793fa3eb299  patches/bash53-007
097cd723cbfb8907674ac32214063a3fd85282657ec5b4e544d2c0f719653fb4  patches/bash53-008
eee30fe78a4b0cb2fe20e010e00308899cfc613e0774ebb3c8557a1552f24f8c  patches/bash53-009
cf76f1cce2ea300c18bff9f002d21f280cc931acd17c28518110b93fe6e72569  patches/bash53-010
0298df8f5ea2a31d3be43ed7d269c5b3c7c342dd5b570bea7f64d66dcbbe7531  patches/bash53-011
d71379b39bebaedaf123414414e77fb458a0a43b9ad3116594c6df7ca6754573  patches/bash53-012
042f9cda967e24bf4211944697441e93d06ff42b4b998629a98a1b249279f200  patches/bash53-013
bd4360b401d38507e358783dcad8536a99c6789f0d3a5bd0cfb8c4a34144696c  patches/bash53-014
55b79ceee2fc27f6767eed697e939a7eb2fe2a28c01556bd75f18d581014f46e  patches/bash53-015
9ea29b266b7d24cb34d0ff3f1c4631e4d527bfe2d1ef15d17cdb924bf31ef767  patches/bash53-016
443b927b45c1558ca72052410f8b8f6e5152b617ed707061a2781d4375b0d1c3  patches/bash53-017
ae715d76c50341d7d7095e9a8d2eeed1ca9546152c2ac7289206f90cf30ac697  patches/bash53-018
a25c581e4d0057dea3833918438a930e2e86ee4c6dc17fe15267b7f04cbc4e3d  patches/bash53-019
df217ed3a9122aa2286d9b67bbe348661b6a9db262b580c29150dae55d532896  patches/bash53-020
SUMS
sha256sum --check patches.sha256 --status || { echo 'Checksum de patch GNU não confere.' >&2; exit 1; }

while IFS='|' read -r abi triple; do
  [[ -n "$abi" ]] || continue
  echo "Compilando GNU Bash $GNU_BASH_VER para $abi"
  rm -rf source
  mkdir source
  tar -xzf "$TARBALL" -C source
  src="$WORK/source/bash-$GNU_BASH_VER"
  for p in patches/bash53-*; do (cd "$src" && patch -s -p0 < "$WORK/$p"); done
  case "$abi" in
    arm64-v8a) target=aarch64-linux-android30; host=aarch64-linux-android ;;
    armeabi-v7a) target=armv7a-linux-androideabi30; host=arm-linux-androideabi ;;
    x86_64) target=x86_64-linux-android30; host=x86_64-linux-android ;;
    x86) target=i686-linux-android30; host=i686-linux-android ;;
    *) echo "ABI não suportada: $abi" >&2; exit 1 ;;
  esac
  (
    cd "$src"
    export CC="$TOOLCHAIN/$target-clang"
    export CC_FOR_BUILD='gcc -std=gnu17'
    export AR="$TOOLCHAIN/llvm-ar" RANLIB="$TOOLCHAIN/llvm-ranlib" STRIP="$TOOLCHAIN/llvm-strip"
    export CFLAGS='-O2 -fPIE' LDFLAGS='-pie'
    ./configure --host="$host" --without-bash-malloc --disable-nls --disable-rpath
    printf '\n#undef HAVE_GETPWENT\n' >> config.h
    make -j2
    "$STRIP" bash
    install -Dm755 bash "$APP/src/main/jniLibs/$abi/libbash.so"
  )
done <<'ABIS'
arm64-v8a|aarch64-linux-android
armeabi-v7a|arm-linux-androideabi
x86_64|x86_64-linux-android
x86|i686-linux-android
ABIS

echo 'GNU Bash 5.3 pronto em android/app/src/main/jniLibs/. O APK extrai a variante da ABI do aparelho.'
