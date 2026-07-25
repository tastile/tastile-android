#!/bin/bash
set -e

# ── Configuration ───────────────────────────────────────────────────
ADB_PORT="${TASTILE_ADB_PORT:-5037}"

echo "[dev] Android development container ready."
echo "[dev] Available tools:"
echo "  - adb (Android Debug Bridge)"
echo "  - gradle (via wrapper)"
echo "  - sdkmanager"
echo "  - javac/java"

# ── Start ADB server ────────────────────────────────────────────────
echo "[dev] Starting ADB server on port ${ADB_PORT}..."
adb start-server -P "${ADB_PORT}" 2>&1 || true

# ── Check for devices ───────────────────────────────────────────────
echo "[dev] Checking for connected devices..."
adb devices 2>&1 || true

echo ""
echo "[dev] Ready. Use 'adb' commands inside the container."
echo "[dev] To connect to a device over network: adb connect <device-ip>:5555"
echo ""

# ── Execute command or keep container running ────────────────────────
if [ $# -gt 0 ]; then
    exec "$@"
else
    # Keep container running in detached mode
    echo "[dev] Running in background mode. Use 'wslc exec' to interact."
    tail -f /dev/null
fi
