#!/usr/bin/env bash
# Builds the signed Android App Bundle (.aab) for Google Play.
#
# The bundle is signed with the same key as the APKs from scripts/build-release.sh. With
# Play App Signing that key acts as the upload key; if the same key is also registered as
# the app signing key ("use an existing key" when enrolling), the APKs Play serves carry
# the same certificate as the GitHub and F-Droid builds, so users can move between them.
#
# The keystore password is read from the desktop keyring; see scripts/lib/release-signing.sh
# for the setup and the environment overrides. If BUNDLETOOL points at a bundletool jar
# (https://github.com/google/bundletool/releases) the bundle is additionally validated with
# it; otherwise that step is skipped.
set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$repo_root"
# shellcheck source=lib/release-signing.sh
source "$repo_root/scripts/lib/release-signing.sh"
load_release_signing

echo "==> Building signed release bundle"
./gradlew bundleRelease --no-daemon --no-configuration-cache "$@"

bundle=app/build/outputs/bundle/release/app-release.aab
if [[ ! -f "$bundle" ]]; then
    echo "error: $bundle was not produced" >&2
    exit 1
fi

echo "==> Verifying the bundle signature"
# A bundle is a JAR-signed zip, so apksigner does not apply; keytool reads the signer
# certificate straight from the archive. Both tools exit non-zero and print warnings for a
# self-signed certificate, which is expected here, so only their output is checked. The
# output language is pinned because the messages are localised.
bundle_fingerprint=$(keytool -J-Duser.language=en -printcert -jarfile "$bundle" 2>/dev/null \
    | sed -n 's/^[[:space:]]*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f' | head -n 1 || true)
if [[ -z "$bundle_fingerprint" ]]; then
    echo "error: $bundle is not signed; was the signing config picked up?" >&2
    exit 1
fi
if [[ "$bundle_fingerprint" != "$keystore_fingerprint" ]]; then
    echo "error: bundle certificate does not match the keystore ($bundle_fingerprint vs $keystore_fingerprint)" >&2
    exit 1
fi
if ! jarsigner -J-Duser.language=en -verify "$bundle" 2>/dev/null | grep -q '^jar verified\.'; then
    echo "error: jarsigner could not verify the signature of $bundle" >&2
    exit 1
fi
echo "Certificate matches $store_file (alias $key_alias)"

if [[ -n "${BUNDLETOOL:-}" ]]; then
    echo "==> Validating with bundletool"
    java -jar "$BUNDLETOOL" validate --bundle="$bundle"
fi

version=$(release_version_name)
dist_dir=app/build/outputs/release-dist
mkdir -p "$dist_dir"
rm -f "$dist_dir"/*.aab "$dist_dir"/*-mapping.txt
dist_bundle="$dist_dir/eventsince-${version:-unknown}.aab"
cp "$bundle" "$dist_bundle"

# The R8 mapping is embedded in the bundle (BUNDLE-METADATA/com.android.tools.build.obfuscation),
# so Play deobfuscates crash reports on its own; a copy is kept next to the bundle for
# retrace of stack traces gathered elsewhere.
mapping=app/build/outputs/mapping/release/mapping.txt
if [[ -f "$mapping" ]]; then
    cp "$mapping" "$dist_dir/eventsince-${version:-unknown}-mapping.txt"
fi

echo "==> Signed bundle in $dist_dir"
write_checksums
