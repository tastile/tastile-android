#!/usr/bin/env bash
# Install the Kotlin language server (kotlin-lsp) into .tools/ on POSIX hosts
# (WSL / wslc / macOS / native Linux).
#
# Mirrors scripts/install-kotlin-lsp.ps1. Reads the version + URL + SHA-256
# pinned in scripts/kotlin-lsp-release.json and extracts the archive under
# .tools/kotlin-lsp/<version>/. Does not commit anything (.tools/ is gitignored).

set -euo pipefail

force=0
manifest=""
while [ $# -gt 0 ]; do
    case "$1" in
        --force) force=1 ;;
        --manifest) manifest="$2"; shift 2 ;;
        -h|--help)
            sed -n '2,/^$/p' "$0" | sed 's/^# \?//'
            exit 0
            ;;
        *) echo "Unknown arg: $1" >&2; exit 2 ;;
    esac
done

script_dir="$(cd "$(dirname "$0")" && pwd)"
repo_root="$(cd "$script_dir/.." && pwd)"
manifest="${manifest:-${repo_root}/scripts/kotlin-lsp-release.json}"

if [ ! -f "$manifest" ]; then
    echo "Manifest not found: $manifest" >&2
    exit 1
fi

if command -v jq >/dev/null 2>&1; then
    version=$(jq -r '.version' "$manifest")
    os=$(uname -s | tr '[:upper:]' '[:lower:]')
    case "$os" in
        linux*)  os_key="linux"  ;;
        darwin*) os_key="macos"  ;;
        *)       echo "Unsupported OS: $os" >&2; exit 1 ;;
    esac
    arch=$(uname -m)
    case "$arch" in
        x86_64|amd64) arch_key="x64" ;;
        aarch64|arm64) arch_key="arm64" ;;
        *) echo "Unsupported architecture: $arch" >&2; exit 1 ;;
    esac
    platform_key="${os_key}-${arch_key}"
    url=$(jq -r ".standaloneArchives[\"$platform_key\"].url" "$manifest")
    sha=$(jq -r ".standaloneArchives[\"$platform_key\"].sha256" "$manifest")
else
    echo "jq is required for install-kotlin-lsp.sh (macOS: brew install jq, Linux apt/dnf: jq)." >&2
    exit 1
fi

if [ -z "$url" ] || [ "$url" = "null" ]; then
    echo "No pinned archive for platform '$platform_key'." >&2
    echo "Update $manifest with the matching standaloneArchives entry." >&2
    exit 1
fi

echo
echo "==> kotlin-lsp $version ($platform_key)"

install_root="${repo_root}/.tools/kotlin-lsp/${version}"
server_exe="${install_root}/bin/intellij-server"

if [ "$force" != "1" ] && [ -x "$server_exe" ]; then
    echo "Already installed at $install_root"
    exit 0
fi

echo
echo "==> Downloading archive"
case "$platform_key" in
    *-x64|*arm64)
        case "$platform_key" in
            *windows*) ext="win.zip" ;;
            *macos*)   ext="sit" ;;
            *)         ext="tar.gz" ;;
        esac
        ;;
esac
work="$(mktemp -d)/kotlin-server-${version}.${ext}"
curl -fsSL "$url" -o "$work"

echo
echo "==> Verifying SHA-256"
actual=$(sha256sum "$work" | awk '{print $1}')
expected=$(echo "$sha" | tr '[:upper:]' '[:lower:]')
if [ "$actual" != "$expected" ]; then
    rm -f "$work"
    echo "SHA-256 mismatch: got $actual, expected $expected" >&2
    exit 1
fi
echo "SHA-256 OK"

echo
echo "==> Extracting"
rm -rf "$install_root"
mkdir -p "$install_root"
case "$ext" in
    tar.gz)
        tar -xzf "$work" -C "$install_root"
        ;;
    win.zip|zip)
        if command -v unzip >/dev/null 2>&1; then
            unzip -q "$work" -d "$install_root"
        else
            echo "unzip not available; install it (apt/dnf/brew install unzip)" >&2
            exit 1
        fi
        ;;
    sit)
        # .sit is a StuffIt archive; macOS-only. Hand off to the system 'open'
        # with StuffIt Expander installed, or document manual extraction.
        echo "macOS .sit extraction requires StuffIt Expander (https://www.stuffit.com)." >&2
        echo "After extraction, ensure ${install_root}/bin/intellij-server exists and is executable." >&2
        exit 1
        ;;
esac
chmod +x "$server_exe" || true
rm -f "$work"

if [ ! -x "$server_exe" ]; then
    echo "Server binary not found at $server_exe after extraction; check archive layout." >&2
    exit 1
fi

echo
echo "==> Installed"
echo "Server binary: $server_exe"
echo "Launcher:      ${repo_root}/scripts/kotlin-lsp-launcher.sh"
