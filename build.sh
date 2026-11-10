#!/data/data/com.termux/files/usr/bin/bash
# Build Mystic Realm APK tanpa Gradle: aapt2 + javac + d8 + zipalign + apksigner.
set -e

ROOT="$(cd "$(dirname "$0")" && pwd)"
SDK_JAR="${ANDROID_JAR:-$ROOT/sdk/android.jar}"
KEYSTORE="${KEYSTORE:-/storage/emulated/0/apk/parental apk/debug.keystore}"
KS_PASS="${KS_PASS:-}"
KS_ALIAS="${KS_ALIAS:-monitor}"
OUT="${OUT:-$ROOT/MysticRealm.apk}"

if [ ! -f "$SDK_JAR" ]; then
  echo "[!] android.jar tidak ditemukan di $SDK_JAR"
  exit 1
fi
for tool in aapt2 javac d8 zipalign apksigner zip keytool; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    echo "[!] Tool '$tool' tidak ada. Jalankan: pkg install aapt2 openjdk-21 d8 apksigner zip"
    exit 1
  fi
done
if [ -z "$KS_PASS" ]; then
  echo "[!] KS_PASS belum diset. Contoh: KS_PASS=... bash build.sh"
  exit 1
fi
if [ ! -f "$KEYSTORE" ]; then
  echo "[*] Membuat keystore debug..."
  keytool -genkeypair -keystore "$KEYSTORE" -alias "$KS_ALIAS" \
    -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -dname "CN=Mystic Realm, O=Local" -keyalg RSA -keysize 2048 -validity 10000 >/dev/null 2>&1
fi

cd "$ROOT"
rm -rf build
mkdir -p build/gen build/classes build/dex

echo "[*] Compile resources..."
aapt2 compile --dir res -o build/res.zip
AAPT_ARGS=(-o build/base.apk -I "$SDK_JAR" --manifest AndroidManifest.xml -R build/res.zip --java build/gen --auto-add-overlay)
if [ -d assets ]; then
  AAPT_ARGS+=(-A assets)
fi
aapt2 link "${AAPT_ARGS[@]}"

echo "[*] Compile Java..."
javac --release 8 -encoding UTF-8 -Xlint:-options -cp "$SDK_JAR" -d build/classes \
  $(find src build/gen -name '*.java')

echo "[*] Dex..."
find build/classes -name '*.class' > build/classes.txt
d8 --lib "$SDK_JAR" --output build/dex @build/classes.txt

cp build/base.apk build/unsigned.apk
( cd build/dex && zip -q ../unsigned.apk classes.dex )
zipalign -f 4 build/unsigned.apk build/aligned.apk

echo "[*] Sign..."
apksigner sign --ks "$KEYSTORE" --ks-pass "pass:$KS_PASS" --key-pass "pass:$KS_PASS" \
  --out build/app.apk build/aligned.apk
apksigner verify build/app.apk

cp build/app.apk "$OUT"
cp build/app.apk /storage/emulated/0/apk/GAME_RPG/MysticRealm.apk
echo "[OK] $OUT"
ls -la "$OUT" /storage/emulated/0/apk/GAME_RPG/MysticRealm.apk
