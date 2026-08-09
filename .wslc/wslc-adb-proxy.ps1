<#
.SYNOPSIS
    Bootstrap the wslc Android-device bridge: install adb on docker-desktop WSL2
    (gcompat), start an adb proxy bound to all interfaces on port 5555, and
    write the proxy host IP into the wslc container so the adb wrapper can
    reach it via ADB_SERVER_SOCKET.

.DESCRIPTION
    wslc containers cannot see USB devices directly. This script wires up a
    bridge:

        ┌─────────────────────────────┐
        │  docker-desktop WSL2 distro  │  ← sees /dev/bus/usb after
        │   adb -a -P 5555 server      │     `usbipd attach --wsl --busid X`
        │   (gcompat for adb on musl)  │
        └──────────────┬───────────────┘
                       │ tcp 5555 (docker-desktop eth0 IP)
                       ▼
        ┌─────────────────────────────┐
        │  wslc dev container         │  ← ADB_SERVER_SOCKET points here
        │   /opt/android-sdk/.../adb  │     (real adb is adb.real, wrapper
        │   → adb-wrapper.sh          │      takes its place on PATH)
        └─────────────────────────────┘

    Run this AFTER `usbipd attach --wsl --busid <X>` (see wslc-dev.ps1).

.PARAMETER ContainerName
    Name of the wslc container to configure. Default: tastile-android-dev.

.EXAMPLE
    .wslc/wslc-adb-proxy.ps1
    .wslc/wslc-adb-proxy.ps1 -ContainerName my-dev-container
#>
[CmdletBinding()]
param(
    [string]$ContainerName = "tastile-android-dev",
    [int]$Port = 5555,
    [switch]$SkipInstall  # Skip the apk add / platform-tools download step
)

$ErrorActionPreference = "Stop"

function Write-Step($msg) { Write-Host "`n[proxy] $msg" -ForegroundColor Cyan }
function Write-Ok($msg)   { Write-Host "  ✓ $msg" -ForegroundColor Green }
function Write-Warn($msg) { Write-Host "  ⚠ $msg" -ForegroundColor Yellow }

# `wsl.exe` itself writes UTF-16 LE to stdout; commands run inside a
# distribution (Alpine docker-desktop) output UTF-8. Windows PowerShell 5.1
# reads piped external-process output as the console input encoding (CP932
# on Japanese locale), mangling every other byte into a space. Use
# ProcessStartInfo with the appropriate StandardOutputEncoding.
#
#   -Encoding Unicode : wsl.exe top-level commands (`--list --verbose`, etc.)
#   -Encoding UTF8    : commands executed inside a distro via `--distribution`
function Invoke-Wsl {
    [CmdletBinding()]
    param(
        [ValidateSet('Unicode','UTF8')]
        [string]$Encoding = 'Unicode',
        [Parameter(Position=0, ValueFromRemainingArguments=$true)]
        [string[]]$Args
    )
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = 'wsl.exe'
    # Quote any arg containing whitespace or shell metacharacters so that
    # CommandLineToArgvW keeps it as one token (otherwise `sh -c "ip ... |
    # awk ..."` arrives at the shell as six separate words).
    $quoted = $Args | ForEach-Object {
        if ($_ -match '\s|[<>|&;]') { '"' + ($_ -replace '"', '\"') + '"' }
        else { $_ }
    }
    $psi.Arguments = ($quoted -join ' ')
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.UseShellExecute = $false
    $enc = [System.Text.Encoding]::Unicode
    if ($Encoding -eq 'UTF8') { $enc = [System.Text.Encoding]::UTF8 }
    $psi.StandardOutputEncoding = $enc
    $psi.StandardErrorEncoding = $enc
    $p = [System.Diagnostics.Process]::Start($psi)
    $stdout = $p.StandardOutput.ReadToEnd()
    $stderr = $p.StandardError.ReadToEnd()
    $p.WaitForExit()
    $global:LASTEXITCODE = $p.ExitCode
    if ($stderr) { Write-Warning "wsl stderr: $stderr" }
    return $stdout
}

# ── 1. Probe docker-desktop WSL2 distro ─────────────────────────────
Write-Step "Locating docker-desktop WSL2 distro..."
$ddState = Invoke-Wsl --list --verbose | Select-String "docker-desktop"
if (-not $ddState) {
    throw "docker-desktop WSL2 distro not found. Install Docker Desktop."
}
Write-Ok "docker-desktop present"

# ── 2. Install adb + gcompat on docker-desktop (Alpine musl) ────────
if (-not $SkipInstall) {
    Write-Step "Installing gcompat + platform-tools on docker-desktop..."
    & wsl --distribution docker-desktop -- sh -c @'
set -e
if ! command -v apk >/dev/null 2>&1; then
  echo "no apk on docker-desktop" >&2; exit 1
fi
apk update -q
apk add -q gcompat libgcc wget unzip
mkdir -p /opt
if [ ! -x /opt/platform-tools/adb ]; then
  cd /tmp
  wget -q https://dl.google.com/android/repository/platform-tools_r37.0.1-linux.zip -O pt.zip
  unzip -q pt.zip -d /opt
  rm pt.zip
fi
echo INSTALLED
'@
    if ($LASTEXITCODE -ne 0) { throw "Failed to install adb on docker-desktop" }
    Write-Ok "adb ready on docker-desktop"
}

# ── 3. Discover docker-desktop eth0 IP ──────────────────────────────
Write-Step "Resolving docker-desktop eth0 IP..."
# docker-desktop's BusyBox `awk` script quoting gets mangled when the script
# passes through PowerShell + wsl.exe's nested arg parsing. Use `tr -s ' '`
# to collapse whitespace then `cut` to grab the address column.
$proxyHost = (Invoke-Wsl -Encoding UTF8 --distribution docker-desktop -- sh -c "ip -o addr show eth0 | head -1 | tr -s ' ' | cut -d' ' -f4 | cut -d/ -f1").Trim()
if (-not $proxyHost) {
    throw "Could not determine docker-desktop eth0 IP"
}
Write-Ok "docker-desktop eth0 = $proxyHost"

# ── 4. Kill any existing adb on docker-desktop, then start proxy ────
Write-Step "Starting adb -a -P $Port server on docker-desktop..."
# Kill any prior proxy, then launch detached. Backgrounding via `nohup ... &`
# inside `wsl ... sh -c` dies when wsl.exe's process exits (wsl launches sh in
# a transient namespace). Start-Process creates a real Win32 process that
# outlives this script, holding the wsl bridge open for as long as the device
# is attached.
Invoke-Wsl -Encoding UTF8 --distribution docker-desktop -- sh -c "pkill -f 'adb.*nodaemon server' 2>/dev/null; sleep 1; true"
$adbLog = Join-Path $env:TEMP 'adb-proxy.out.log'
$adbErr = Join-Path $env:TEMP 'adb-proxy.err.log'
if (Test-Path $adbLog) { Remove-Item $adbLog -Force }
if (Test-Path $adbErr) { Remove-Item $adbErr -Force }
Start-Process -FilePath 'wsl.exe' `
    -ArgumentList @('--distribution','docker-desktop','--','/opt/platform-tools/adb','-a','-P',"$Port",'nodaemon','server') `
    -RedirectStandardOutput $adbLog `
    -RedirectStandardError $adbErr `
    -WindowStyle Hidden | Out-Null
# Give adb a moment to bind the port, then verify it's up on the docker-desktop host.
$bound = $false
for ($i = 0; $i -lt 10; $i++) {
    Start-Sleep -Seconds 1
    $probe = Invoke-Wsl -Encoding UTF8 --distribution docker-desktop -- sh -c "netstat -ln 2>/dev/null | grep -E ':$Port\b'"
    if ($probe -match ":$Port\b") { $bound = $true; break }
}
if (-not $bound) { throw "adb proxy did not bind :$Port on docker-desktop (see $adbErr)" }

# ── 5. Verify proxy is reachable from wslc container ────────────────
Write-Step "Probing container's reachability to ${proxyHost}:$Port..."
$containerExists = (& wslc container list 2>&1) | Select-String $ContainerName
if ($containerExists) {
    & wslc exec $ContainerName bash -c "timeout 3 bash -c 'echo > /dev/tcp/$proxyHost/$Port' && echo REACHABLE || echo UNREACHABLE"
    if ($LASTEXITCODE -ne 0) {
        Write-Warn "Container cannot reach ${proxyHost}:$Port. Check WSL networking."
    }
}

# ── 6. Write proxy host into container for the wrapper ──────────────
if ($containerExists) {
    Write-Step "Writing /etc/adb-proxy-host = $proxyHost into $ContainerName..."
    & wslc exec $ContainerName bash -c "echo '$proxyHost' > /etc/adb-proxy-host && chmod 644 /etc/adb-proxy-host"
    if ($LASTEXITCODE -ne 0) {
        Write-Warn "Failed to write /etc/adb-proxy-host; wrapper will use env var fallback."
    }
}

Write-Host "`n=== adb proxy ready ===" -ForegroundColor Green
Write-Host "  Proxy host : ${proxyHost}:$Port"
Write-Host "  Container  : $ContainerName"
Write-Host ""
Write-Host "Verify with:"
Write-Host "  wslc exec $ContainerName adb devices"
Write-Host ""
Write-Host "Rebuild the image to bake the wrapper in:"
Write-Host "  .wslc/wslc-build.ps1 -NoCache"
