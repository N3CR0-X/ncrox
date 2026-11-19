#!/bin/sh
# Testes locais independentes do Gradle. Requer JDK (javac, java, keytool).
set -eu
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$HERE/../.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT HUP INT TERM
keytool -genkeypair -alias local -keyalg RSA -keysize 2048 -validity 2 -dname 'CN=localhost' -ext 'SAN=dns:localhost' -storetype PKCS12 -keystore "$TMP/trusted.p12" -storepass testpass -keypass testpass -noprompt >/dev/null 2>&1
keytool -exportcert -alias local -keystore "$TMP/trusted.p12" -storepass testpass -file "$TMP/local.cer" >/dev/null 2>&1
keytool -importcert -alias local -file "$TMP/local.cer" -keystore "$TMP/trust.p12" -storetype PKCS12 -storepass testpass -noprompt >/dev/null 2>&1
keytool -genkeypair -alias other -keyalg RSA -keysize 2048 -validity 2 -dname 'CN=localhost' -ext 'SAN=dns:localhost' -storetype PKCS12 -keystore "$TMP/untrusted.p12" -storepass testpass -keypass testpass -noprompt >/dev/null 2>&1
mkdir -p "$TMP/classes"
javac --release 8 -encoding UTF-8 -d "$TMP/classes" "$ROOT/android/app/src/main/java/com/ncrox/satanachia/FerramentasSeguras.java"
javac --release 11 -encoding UTF-8 -cp "$TMP/classes" -d "$TMP/classes" "$HERE/com/ncrox/satanachia/FerramentasSegurasTest.java"
java -cp "$TMP/classes" com.ncrox.satanachia.FerramentasSegurasTest "$TMP/trusted.p12" "$TMP/untrusted.p12" "$TMP/trust.p12"
