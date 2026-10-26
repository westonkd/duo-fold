#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

for name in FOLIO_RELEASE_STORE_FILE FOLIO_RELEASE_STORE_PASSWORD FOLIO_RELEASE_KEY_ALIAS FOLIO_RELEASE_KEY_PASSWORD; do
    if [[ -z "${!name:-}" ]]; then
        echo "Set $name first. See docs/releases/play-1.0.0.md." >&2
        exit 1
    fi
done

if [[ -n "$(git status --porcelain)" ]]; then
    echo "The working tree has changes. Build a Play bundle from a clean commit." >&2
    exit 1
fi

if command -v mise >/dev/null; then eval "$(mise env -s bash)"; fi

./gradlew --console=plain :app:testDebugUnitTest :app:lintRelease :app:bundleRelease

bundle=app/build/outputs/bundle/release/app-release.aab
manifest=$(unzip -p "$bundle" base/manifest/AndroidManifest.xml | strings)

version=$(sed -n 's/^val folioVersion = "\(.*\)"/\1/p' app/build.gradle.kts)
signer=$(keytool -printcert -jarfile "$bundle" 2>/dev/null | sed -n 's/^.*SHA256: //p' | head -1)

echo
echo "Bundle:   $bundle ($(du -h "$bundle" | cut -f1))"
echo "Package:  $(grep -o 'com\.westonkd\.folio' <<< "$manifest" | head -1)"
echo "Version:  $version"
echo "Signer:   ${signer:-unsigned}"
echo "Commit:   $(git rev-parse --short HEAD)"

if [[ -z "$signer" ]]; then
    echo "The bundle isn't signed." >&2
    exit 1
fi
if [[ "$version" == *-* ]]; then
    echo "Warning: $version is a pre-release name. Play shows it to testers as is." >&2
fi
if grep -q 'android\.permission\.INTERNET' <<< "$manifest"; then
    echo "The bundle asks for INTERNET. The Data safety answers in docs/releases/play-1.0.0.md assume it doesn't." >&2
    exit 1
fi
if unzip -p "$bundle" base/assets/CHANGELOG.md | grep -q '^## Before Google Play'; then
    echo "The bundled CHANGELOG.md still carries the history before Google Play." >&2
    exit 1
fi
