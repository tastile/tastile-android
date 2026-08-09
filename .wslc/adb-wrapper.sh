#!/bin/bash
# adb wrapper for wslc dev containers. Routes the adb client through the
# docker-desktop WSL2 adb proxy (port 5555) which holds the actual USB
# device. wslc containers cannot see USB directly:
#   1. The kernel module matching WSL2 (6.18.35.2-microsoft) is not shipped.
#   2. wslc has no `--device` / `--privileged` flags.
#
# Host resolution order:
#   1. $ADB_PROXY_HOST env var (set via `wslc run -e`).
#   2. /etc/adb-proxy-host file (written by .wslc/wslc-adb-proxy.ps1).
#   3. Hardcoded fallback if neither is present (will fail until proxy is up).
#
# Once ADB_SERVER_SOCKET is set, adb clients transparently route through the
# remote server; the local adb server (if any) on this container's 5037 is
# unused. The real adb binary lives at $ANDROID_HOME/platform-tools/adb.real.

set -e

resolve_proxy_host() {
  if [ -n "${ADB_PROXY_HOST:-}" ]; then
    echo "$ADB_PROXY_HOST"
    return
  fi
  if [ -r /etc/adb-proxy-host ]; then
    tr -d '[:space:]' < /etc/adb-proxy-host
    return
  fi
  echo "172.22.156.22"  # last-known docker-desktop eth0; see .wslc/wslc-adb-proxy.ps1
}

HOST="$(resolve_proxy_host)"
export ADB_SERVER_SOCKET="tcp:${HOST}:5555"
exec "${ANDROID_HOME}/platform-tools/adb.real" "$@"
