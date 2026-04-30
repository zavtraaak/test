#!/usr/bin/env bash
# Reproducible build of "NoTube Spotify" — an apktool-patched Spotify APK.
#
# Removes visual interstitial / display-ad activities (best effort), repacks
# and signs with the project keystore.
#
# Audio ads delivered inside the audio stream are NOT removed by this patch
# (Spotify-side ad insertion cannot be blocked client-side without bypassing
# attestation, which was DMCA'd and is out of scope for this build).
#
# Usage:
#   ./spotify/build.sh /path/to/spotify-orig.apk
#
# Outputs:
#   build/spotify-patched.apk

set -euo pipefail

APK_IN=${1:-spotify-orig.apk}
WORK=$(mktemp -d)
OUT=build/spotify-patched.apk

if [ ! -f "$APK_IN" ]; then
  echo "Input APK not found: $APK_IN" >&2
  echo "Download Spotify ${VERSION:-9.1.24.1739} from Aptoide:" >&2
  echo "  curl -sLo spotify-orig.apk https://pool.apk.aptoide.com/sirkesyone-applikes/com-spotify-music-138424205-73673310-5e0f2c75b1dc460c083d4c9cbc1846b0.apk" >&2
  exit 2
fi

if [ -z "${ANDROID_HOME:-}" ]; then
  echo "ANDROID_HOME not set" >&2; exit 2
fi
BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)

if [ ! -f apktool.jar ]; then
  echo "Downloading apktool 2.10.0..."
  curl -sLo apktool.jar https://github.com/iBotPeaches/Apktool/releases/download/v2.10.0/apktool_2.10.0.jar
fi

mkdir -p build

echo "[1/4] Decompiling $APK_IN"
java -jar apktool.jar d -f -o "$WORK/decoded" "$APK_IN" >/dev/null

echo "[2/4] Applying ad-stub patches"
SPOTIFY_PATCH_ROOT="$WORK/decoded" python3 spotify/patch_smali.py

echo "[3/4] Repacking + zipalign"
java -jar apktool.jar b -f -o "$WORK/unsigned.apk" "$WORK/decoded" >/dev/null
"$BT/zipalign" -f -p 4 "$WORK/unsigned.apk" "$WORK/aligned.apk"

echo "[4/4] Signing with notube-release.jks"
"$BT/apksigner" sign \
  --ks notube-release.jks \
  --ks-pass pass:notube123 \
  --ks-key-alias notube \
  --key-pass pass:notube123 \
  --out "$OUT" \
  "$WORK/aligned.apk"

"$BT/apksigner" verify --print-certs "$OUT" | head -5
"$BT/aapt" dump badging "$OUT" | head -3

rm -rf "$WORK"
echo "Built: $OUT"
