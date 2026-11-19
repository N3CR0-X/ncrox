#!/bin/sh
# Testes Java offline, sem Android SDK. Não validam layout nem spans na UI.
set -eu
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$HERE/../.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
SRC="$ROOT/android/app/src/main/java/com/ncrox/satanachia"
javac --release 8 -encoding UTF-8 -d "$TMP" "$SRC/FerramentasSeguras.java" "$SRC/BannerArt.java" "$HERE/com/ncrox/satanachia/BannerArtTest.java"
java -cp "$TMP" com.ncrox.satanachia.BannerArtTest
