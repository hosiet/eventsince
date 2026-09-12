# Shared by scripts/build-release.sh and scripts/build-bundle.sh. Source it, then call
# load_release_signing: it locates the keystore and the Android SDK, reads the keystore
# password from the desktop keyring (Secret Service API via secret-tool) so it never has
# to be written to disk, and exports the EVENTSINCE_* variables that app/build.gradle.kts
# turns into the release signing config.
#
# Store the password once with:
#
#   secret-tool store --label="EventSince release keystore" app eventsince purpose keystore
#
# Optional environment overrides:
#   EVENTSINCE_STORE_FILE    path to the keystore (default: ~/.android/eventsince-release.jks)
#   EVENTSINCE_KEY_ALIAS     key alias (default: eventsince)
#   EVENTSINCE_KEY_PASSWORD  key password if it differs from the keystore password
#   ANDROID_HOME             Android SDK root (default: sdk.dir from local.properties)
#
# After the call these variables are set for the caller:
#   store_file, key_alias, store_password, key_password, sdk_dir, build_tools, apksigner,
#   keystore_fingerprint (lower-case hex SHA-256 of the signing certificate, no colons)

load_release_signing() {
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

    # Locate the Android SDK and the newest build-tools.
    sdk_dir=${ANDROID_HOME:-}
    if [[ -z "$sdk_dir" && -f local.properties ]]; then
        sdk_dir=$(sed -n 's/^sdk\.dir=//p' local.properties)
    fi
    apksigner=$(ls -d "$sdk_dir"/build-tools/*/apksigner 2>/dev/null | sort -V | tail -n 1 || true)
    if [[ -z "$apksigner" ]]; then
        echo "error: apksigner not found under $sdk_dir/build-tools" >&2
        exit 1
    fi
    build_tools=$(dirname "$apksigner")

    keystore_fingerprint=$(STORE_PASSWORD="$store_password" keytool -list -v -keystore "$store_file" -alias "$key_alias" -storepass:env STORE_PASSWORD 2>/dev/null \
        | sed -n 's/^[[:space:]]*SHA256: //p' | tr -d ':' | tr 'A-F' 'a-f' | head -n 1)
    if [[ -z "$keystore_fingerprint" ]]; then
        echo "error: could not read the certificate fingerprint from $store_file (wrong password or alias?)" >&2
        exit 1
    fi

    export EVENTSINCE_STORE_FILE="$store_file"
    export EVENTSINCE_STORE_PASSWORD="$store_password"
    export EVENTSINCE_KEY_ALIAS="$key_alias"
    export EVENTSINCE_KEY_PASSWORD="$key_password"
}

# Reads versionName from app/build.gradle.kts (the bundle's manifest is protobuf-encoded,
# so it cannot be read with aapt).
release_version_name() {
    sed -n 's/^[[:space:]]*versionName = "\([^"]*\)".*/\1/p' app/build.gradle.kts | head -n 1
}

# Rewrites $dist_dir/SHA256SUMS to cover every APK and bundle currently in $dist_dir, so
# the APK and bundle scripts can run in either order without losing each other's lines.
write_checksums() {
    (
        cd "$dist_dir"
        files=()
        for f in *.apk *.aab; do [[ -f "$f" ]] && files+=("$f"); done
        sha256sum "${files[@]}" | tee SHA256SUMS
    )
}
