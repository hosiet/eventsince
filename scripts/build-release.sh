#!/usr/bin/env bash
# Builds a signed release APK.
#
# The keystore password is read from the desktop keyring (Secret Service API via
# secret-tool) so it never has to be written to disk. Store it once with:
#
#   secret-tool store --label="EventSince release keystore" app eventsince purpose keystore
#
# Optional environment overrides:
#   EVENTSINCE_STORE_FILE  path to the keystore (default: ~/.android/eventsince-release.jks)
#   EVENTSINCE_KEY_ALIAS   key alias (default: eventsince)
#   EVENTSINCE_KEY_PASSWORD  key password if it differs from the keystore password
#   ANDROID_HOME           Android SDK root (default: sdk.dir from local.properties)
set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$repo_root"

store_file=${EVENTSINCE_STORE_FILE:-$HOME/.android/eventsince-release.jks}
key_alias=${EVENTSINCE_KEY_ALIAS:-eventsince}

if [[ ! -f "$store_file" ]]; then
    echo "error: keystore not found: $store_file" >&2
    exit 1
fi

if ! command -v secret-tool >/dev/null; then
    echo "error: secret-tool (libsecret) is required to read the keystore password" >&2
    exit 1
fi

store_password=$(secret-tool lookup app eventsince purpose keystore || true)
if [[ -z "$store_password" ]]; then
    echo "error: no keystore password in the keyring; store it with:" >&2
    echo '  secret-tool store --label="EventSince release keystore" app eventsince purpose keystore' >&2
    exit 1
fi
key_password=${EVENTSINCE_KEY_PASSWORD:-$store_password}

# Locate the Android SDK and the newest apksigner.
sdk_dir=${ANDROID_HOME:-}
if [[ -z "$sdk_dir" && -f local.properties ]]; then
    sdk_dir=$(sed -n 's/^sdk\.dir=//p' local.properties)
fi
apksigner=$(ls -d "$sdk_dir"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -n 1 || true)
if [[ -z "$apksigner" ]]; then
    echo "error: apksigner not found under $sdk_dir/build-tools" >&2
    exit 1
fi

echo "==> Building signed release APK"
EVENTSINCE_STORE_FILE="$store_file" \
EVENTSINCE_STORE_PASSWORD="$store_password" \
EVENTSINCE_KEY_ALIAS="$key_alias" \
EVENTSINCE_KEY_PASSWORD="$key_password" \
./gradlew assembleRelease --no-daemon --no-configuration-cache "$@"

apk=app/build/outputs/apk/release/app-release.apk
if [[ ! -f "$apk" ]]; then
    echo "error: expected $apk after the build; was the signing config picked up?" >&2
    exit 1
fi

echo "==> Verifying signature"
"$apksigner" verify --verbose --print-certs "$apk" | grep -E "Verified using|Signer .* certificate SHA-256|Number of signers"

apk_fingerprint=$("$apksigner" verify --print-certs "$apk" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
keystore_fingerprint=$(STORE_PASSWORD="$store_password" keytool -list -v -keystore "$store_file" -alias "$key_alias" -storepass:env STORE_PASSWORD 2>/dev/null \
    | sed -n 's/^[[:space:]]*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f' | head -n 1)
if [[ "$apk_fingerprint" != "$keystore_fingerprint" ]]; then
    echo "error: APK certificate does not match the keystore ($apk_fingerprint vs $keystore_fingerprint)" >&2
    exit 1
fi
echo "Certificate matches $store_file (alias $key_alias)"

# Copy to a versioned file name for uploading to a release.
build_tools=$(dirname "$apksigner")
version=$("$build_tools/aapt" dump badging "$apk" 2>/dev/null | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -n 1)
dist_dir=app/build/outputs/release-dist
mkdir -p "$dist_dir"
dist_apk="$dist_dir/eventsince-${version:-unknown}.apk"
cp "$apk" "$dist_apk"
echo "==> Signed APK: $dist_apk"
