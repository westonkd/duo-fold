#!/usr/bin/env bash
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

site=https://twofoldlauncher.app
mode=deploy
allow_dirty=0

for arg in "$@"; do
    case "$arg" in
        --dry-run) mode=dry-run ;;
        --check) mode=check ;;
        --allow-dirty) allow_dirty=1 ;;
        *) echo "Usage: bash tools/deploy-website.sh [--dry-run | --check] [--allow-dirty]" >&2; exit 2 ;;
    esac
done

public_resolve() {
    local host=${site#https://} ip=""
    if command -v dig >/dev/null; then
        ip=$(dig +short @1.1.1.1 "$host" A | grep -E '^[0-9.]+$' | head -1 || true)
    fi
    [[ -n "$ip" ]] && echo "--resolve $host:443:$ip"
}

check_live() {
    local failed=0 path want got
    local -a resolve
    read -ra resolve <<< "$(public_resolve)"
    while read -r path want; do
        got=$(curl -s -o /dev/null -w '%{http_code}' --max-time 15 "${resolve[@]}" "$site$path" || true)
        if [[ "$got" == "$want" ]]; then
            printf 'ok      %s %s\n' "$got" "$site$path"
        else
            printf 'FAILED  %s %s (wanted %s)\n' "$got" "$site$path" "$want"
            failed=1
        fi
    done <<'EOF'
/ 200
/privacy 200
/permissions 200
/sitemap.xml 200
/robots.txt 200
/og.png 200
/no-such-page 404
EOF
    return "$failed"
}

if [[ "$mode" == check ]]; then
    check_live
    exit
fi

if [[ "$allow_dirty" == 0 && "$mode" == deploy ]]; then
    if [[ -n "$(git status --porcelain -- website PRIVACY.md PERMISSIONS.md)" ]]; then
        echo "website/, PRIVACY.md or PERMISSIONS.md has uncommitted changes. Commit them, or pass --allow-dirty." >&2
        exit 1
    fi
    git fetch -q origin main
    if [[ "$(git rev-parse HEAD)" != "$(git rev-parse origin/main)" ]]; then
        echo "HEAD is not origin/main. The live site is deployed from main; check it out and pull, or pass --allow-dirty." >&2
        exit 1
    fi
fi

cd website
if command -v mise >/dev/null; then eval "$(mise env -s bash)"; fi
[[ -d node_modules ]] || npm ci
npm run build

if [[ "$mode" == dry-run ]]; then
    npx wrangler deploy --dry-run
    exit
fi

npx wrangler deploy
echo
echo "Checking $site (a new custom domain can take a minute or two to answer)"
for attempt in 1 2 3 4 5 6; do
    if check_live; then exit 0; fi
    [[ "$attempt" == 6 ]] && exit 1
    echo "Retrying in 20 s"
    sleep 20
done
