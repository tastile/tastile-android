#!/usr/bin/env pwsh
# Locate and invoke the pinned kotlin-lsp server (Windows launcher).
#
# Sibling of scripts/install-kotlin-lsp.ps1. Assumes that the bootstrap
# script has already downloaded the archive pinned in
# scripts/kotlin-lsp-release.json.

[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$ServerArgs
)

$ErrorActionPreference = 'Stop'

$LauncherDir = $PSScriptRoot
$RepoRoot = (Resolve-Path (Join-Path $LauncherDir '..')).Path
$BinDir = Join-Path $LauncherDir 'kotlin-lsp'
$ManifestPath = Join-Path $RepoRoot 'scripts\kotlin-lsp-release.json'

if (-not (Test-Path -LiteralPath $ManifestPath)) {
    throw "Manifest missing: $ManifestPath"
}
$version = (Get-Content -LiteralPath $ManifestPath -Raw | ConvertFrom-Json).version
$versionDir = Join-Path $BinDir $version
$exe = $null
$candidates = @(
    (Join-Path $versionDir 'bin\intellij-server.cmd'),
    (Join-Path $versionDir 'bin\intellij-server.bat'),
    (Join-Path $versionDir 'bin\intellij-server.ps1'),
    (Join-Path $versionDir 'bin\intellij-server.exe'),
    (Join-Path $versionDir 'bin\intellij-server')
)
foreach ($c in $candidates) {
    if (Test-Path -LiteralPath $c) { $exe = $c; break }
}
if (-not $exe) {
    throw "Kotlin LSP $version not installed. Run scripts\install-kotlin-lsp.ps1 first."
}

# Resolve a JDK 25 runtime. Preference order:
#   1. $KOTLIN_LSP_JAVA_HOME / $JAVA25_HOME
#   2. .tools/jdk-25 inside the repo
#   3. Standard Adoptium / system locations
$javaHome = $env:KOTLIN_LSP_JAVA_HOME
if (-not $javaHome) { $javaHome = $env:JAVA25_HOME }
if (-not $javaHome) {
    $candidates = @(
        (Join-Path $RepoRoot '.tools\jdk-25'),
        'C:\Program Files\Eclipse Adoptium\jdk-25',
        'C:\Program Files\Java\jdk-25'
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path -LiteralPath (Join-Path $c 'bin\java.exe'))) {
            $javaHome = $c
            break
        }
    }
}
if (-not $javaHome) {
    throw "JDK 25 not found. Set `$env:KOTLIN_LSP_JAVA_HOME to a JDK 25 home, vendor one to .tools\jdk-25\, or install Temurin 25 (https://adoptium.net/temurin/releases/?version=25)."
}

$javaExe = Join-Path $javaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $javaExe)) {
    throw "java.exe not found under $javaHome. Verify `$env:KOTLIN_LSP_JAVA_HOME points to a JDK 25 (not a JRE)."
}
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"

& $exe @ServerArgs
