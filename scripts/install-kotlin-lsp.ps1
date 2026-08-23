#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Install the Kotlin language server (kotlin-lsp) into .tools/.

.DESCRIPTION
    Downloads the standalone Kotlin Language Server archive pinned in
    scripts/kotlin-lsp-release.json, verifies its SHA-256, and extracts it
    under .tools/kotlin-lsp/<version>/.

    The corresponding launcher scripts (kotlin-lsp.ps1 / kotlin-lsp.sh) are
    static files that read the same manifest and invoke the extracted server
    under a JDK 25 runtime. They expect this script to have run at least once.

    Repositories: .tools/ is gitignored; this script does not commit anything.

.PARAMETER Force
    Re-download and re-extract even if the pinned version is already present.

.PARAMETER ManifestPath
    Override the pinned manifest path (defaults to scripts/kotlin-lsp-release.json).

.EXAMPLE
    .\scripts\install-kotlin-lsp.ps1
    # Idempotent install (no-op if already at pinned version)

    .\scripts\install-kotlin-lsp.ps1 -Force
    # Reinstall from scratch
#>

[CmdletBinding()]
param(
    [switch]$Force,
    [string]$ManifestPath
)

$ErrorActionPreference = 'Stop'

function Write-Section {
    param([string]$Message)
    Write-Host ''
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Resolve-Os {
    if ($IsWindows -or ([Environment]::OSVersion.Platform -eq [System.PlatformID]::Win32NT)) { return 'windows' }
    if ($IsMacOS)   { return 'macos' }
    if ($IsLinux)   { return 'linux' }
    throw "Unsupported OS: $PSVersionTable.OS"
}

function Resolve-Platform {
    $arch = [System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture
    switch ($arch) {
        'X64'   { return 'x64' }
        'Arm64' { return 'arm64' }
        default { throw "Unsupported architecture: $arch" }
    }
}

if (-not $ManifestPath) {
    $ManifestPath = Join-Path $PSScriptRoot 'kotlin-lsp-release.json'
}
if (-not (Test-Path -LiteralPath $ManifestPath)) {
    throw "Manifest not found: $ManifestPath"
}

$manifest = Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json
$version = $manifest.version
$os = Resolve-Os
$arch = Resolve-Platform
$platformKey = "$os-$arch"
$archive = $manifest.standaloneArchives."$platformKey"
if (-not $archive) {
    throw "No pinned archive for $platformKey. Update $ManifestPath with a 'standaloneArchives' entry."
}

Write-Section "kotlin-lsp $version ($platformKey)"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$installRoot = Join-Path $repoRoot ".tools\kotlin-lsp\$version"
$serverExe = Join-Path $installRoot 'bin\intellij-server'

if ((-not $Force) -and (Test-Path -LiteralPath $serverExe)) {
    Write-Host "Already installed at $installRoot" -ForegroundColor Green
    Write-Host "Use -Force to reinstall." -ForegroundColor DarkGray
    return
}

Write-Section "Downloading archive"
$expectedExt = switch ($os) { 'windows' { 'win.zip' } 'macos' { 'sit' } 'linux' { 'tar.gz' } }
$archivePath = Join-Path $env:TEMP "kotlin-server-$version.$expectedExt"
Invoke-WebRequest -Uri $archive.url -OutFile $archivePath -UseBasicParsing

Write-Section "Verifying SHA-256"
$actual = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash.ToLower()
$expected = $archive.sha256.ToLower()
if ($actual -ne $expected) {
    Remove-Item -LiteralPath $archivePath -Force
    throw "SHA-256 mismatch: got $actual, expected $expected"
}
Write-Host "SHA-256 OK" -ForegroundColor Green

Write-Section "Extracting"
if (Test-Path -LiteralPath $installRoot) {
    Remove-Item -LiteralPath $installRoot -Recurse -Force
}
New-Item -ItemType Directory -Path $installRoot -Force | Out-Null
switch ($os) {
    'windows' {
        Expand-Archive -LiteralPath $archivePath -DestinationPath $installRoot
    }
    'linux' {
        & tar -xzf $archivePath -C $installRoot
    }
    'macos' {
        # Standalone macOS distribution is a .sit (StuffIt) archive.
        # StuffIt is not shipped with macOS by default; require the user to install it
        # or use brew extraction. Document in ADR; intentionally not auto-extracting.
        throw "macOS standalone archive is .sit; install StuffIt Expander or use brew to extract. Manual step required for now."
    }
}
Remove-Item -LiteralPath $archivePath -Force
if (-not (Test-Path -LiteralPath $serverExe)) {
    throw "Server binary not found at $serverExe after extraction; check archive layout."
}
Write-Host "Installed: $serverExe" -ForegroundColor Green

Write-Section "Done"
Write-Host "Launcher (PowerShell): $(Join-Path $repoRoot '.tools\kotlin-lsp\bin\kotlin-lsp.ps1')"
Write-Host "Run that launcher with --help to verify the server starts." -ForegroundColor DarkGray
