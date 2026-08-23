#!/usr/bin/env bash
# Locate and invoke the pinned kotlin-lsp server (POSIX launcher).
#
# Sibling of scripts/install-kotlin-lsp.sh. Assumes the bootstrap script
# has already downloaded the archive pinned in scripts/kotlin-lsp-release.json.

set -euo pipefail

launcher_dir="$(cd "$(dirname "$0")" && pwd)"
repo_root="$(cd "$launcher_dir/.." && pwd)"
bin_dir="${launcher_dir}/kotlin-lsp"
manifest="${repo_root}/scripts/kotlin-lsp-release.json"

if [ ! -f "$manifest" ]; then
    echo "Manifest missing: $manifest" >&2
    exit 1
fi

if ! command -v jq >/dev/null 2>&1; then
    echo "jq is required (apt/dnf/brew install jq)." >&2
    exit 1
fi

version=$(jq -r '.version' "$manifest")
version_dir="${bin_dir}/${version}"
exe=""
for candidate in \
    "${version_dir}/bin/intellij-server" \
    "${version_dir}/bin/intellij-server.sh"; do
    if [ -x "$candidate" ]; then exe="$candidate"; break; fi
done

if [ -z "$exe" ]; then
    echo "Kotlin LSP $version not installed. Run scripts/install-kotlin-lsp.sh first." >&2
    exit 1
fi

# Resolve a JDK 25 runtime. Preference order:
#   1. $KOTLIN_LSP_JAVA_HOME / $JAVA25_HOME
#   2. .tools/jdk-25 inside the repo
#   3. Common system locations
java_home="${KOTLIN_LSP_JAVA_HOME:-${JAVA25_HOME:-}}"
if [ -z "$java_home" ]; then
    candidates=(
        "${repo_root}/.tools/jdk-25"
        /usr/lib/jvm/temurin-25-jdk
        /usr/lib/jvm/java-25-temurin
        /opt/homebrew/opt/openjdk@25
        /usr/local/opt/openjdk@25
    )
    for c in "${candidates[@]}"; do
        if [ -x "${c}/bin/java" ]; then java_home="$c"; break; fi
    done
fi
if [ -z "$java_home" ] || [ ! -x "${java_home}/bin/java" ]; then
    echo "JDK 25 not found. Set \$KOTLIN_LSP_JAVA_HOME to a JDK 25 home, vendor one to .tools/jdk-25/, or install Temurin 25 (https://adoptium.net/temurin/releases/?version=25)." >&2
    exit 1
fi

export JAVA_HOME="$java_home"
export PATH="${java_home}/bin:${PATH}"

exec "$exe" "$@"
