#!/usr/bin/env bash
# Builds signed release APKs: one per ABI (arm64-v8a, x86_64) plus a universal one, for
# GitHub releases and sideloading. For Google Play use scripts/build-bundle.sh instead.
#
# The keystore password is read from the desktop keyring; see scripts/lib/release-signing.sh
# for the setup and the environment overrides.
set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$repo_root"
# shellcheck source=lib/release-signing.sh
source "$repo_root/scripts/lib/release-signing.sh"
load_release_signing

echo "==> Building signed release APKs"
./gradlew assembleRelease -PabiSplits=true --no-daemon --no-configuration-cache "$@"

apk_dir=app/build/outputs/apk/release
apks=("$apk_dir"/app-*-release.apk)
if [[ ! -f "${apks[0]}" ]]; then
    echo "error: no signed APKs under $apk_dir; was the signing config picked up?" >&2
    exit 1
fi

dist_dir=app/build/outputs/release-dist
mkdir -p "$dist_dir"
rm -f "$dist_dir"/*.apk

echo "==> Verifying signatures"
for apk in "${apks[@]}"; do
    name=$(basename "$apk")
    apk_fingerprint=$("$apksigner" verify --print-certs "$apk" 2>/dev/null | sed -n 's/^Signer #1 certificate SHA-256 digest: //p')
    if [[ "$apk_fingerprint" != "$keystore_fingerprint" ]]; then
        echo "error: $name certificate does not match the keystore ($apk_fingerprint vs $keystore_fingerprint)" >&2
        exit 1
    fi
    schemes=$("$apksigner" verify --verbose "$apk" 2>/dev/null | sed -n 's/^Verified using \(v[0-9.]*\) scheme.*: true$/\1/p' | paste -sd, -)
    # app-<abi>-release.apk -> eventsince-<version>-<abi>.apk
    abi=${name#app-}; abi=${abi%-release.apk}
    version=$("$build_tools/aapt" dump badging "$apk" 2>/dev/null | sed -n "s/.*versionName='\([^']*\)'.*/\1/p" | head -n 1)
    dist_apk="$dist_dir/eventsince-${version:-unknown}-${abi}.apk"
    cp "$apk" "$dist_apk"
    echo "$name: certificate OK, signed with $schemes -> $dist_apk"
done
echo "Certificate matches $store_file (alias $key_alias)"

echo "==> Signed APKs in $dist_dir"
write_checksums
