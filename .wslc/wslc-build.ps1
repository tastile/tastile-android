<#
.SYNOPSIS
    Build tastile-android WSLC container with versions auto-extracted from build.gradle.kts.

.DESCRIPTION
    Reads app/build.gradle.kts to extract compileSdk and ndkVersion, then passes them
    as --build-arg to wslc build. If the extracted SDK version is not available,
    falls back to the latest stable version automatically.

.EXAMPLE
    .wslc/wslc-build.ps1
    .wslc/wslc-build.ps1 -NoCache
    .wslc/wslc-build.ps1 -WhatIf
#>
[CmdletBinding()]
param(
    [switch]$NoCache,
    [switch]$WhatIf
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir

# ── Extract versions from app/build.gradle.kts ──────────────────────
$gradleFile = Join-Path $RepoRoot "app/build.gradle.kts"
if (-not (Test-Path $gradleFile)) {
    throw "build.gradle.kts not found: $gradleFile"
}

$compileSdk = (Select-String -Path $gradleFile -Pattern 'compileSdk\s*=\s*(\d+)' |
    Select-Object -First 1).Matches.Groups[1].Value

$ndkVersion = (Select-String -Path $gradleFile -Pattern 'ndkVersion\s*=\s*"(.+?)"' |
    Select-Object -First 1).Matches.Groups[1].Value

if (-not $compileSdk) {
    throw "Could not extract compileSdk from $gradleFile"
}
if (-not $ndkVersion) {
    throw "Could not extract ndkVersion from $gradleFile"
}

Write-Host "=== tastile-android WSLC Build ===" -ForegroundColor Cyan
Write-Host "compileSdk (from build.gradle.kts): $compileSdk"
Write-Host "ndkVersion  (from build.gradle.kts): $ndkVersion"

# ── Build with fallback ─────────────────────────────────────────────
$containerfile = Join-Path $ScriptDir "Containerfile"
$tagName = "tastile-android-dev:latest"
$fallbackSdk = "36"

function Invoke-Build {
    param([string]$SdkVersion)
    $buildArgs = @(
        "build",
        "-f", $containerfile,
        "-t", $tagName,
        "--build-arg", "ANDROID_COMPILE_SDK=$SdkVersion",
        "--build-arg", "ANDROID_NDK_VERSION=$ndkVersion"
    )
    if ($NoCache) { $buildArgs += "--no-cache" }
    $buildArgs += $RepoRoot
    Write-Host "Command: wslc $($buildArgs -join ' ')" -ForegroundColor DarkGray
    if ($WhatIf) {
        Write-Host "[DRY RUN] Would execute above command" -ForegroundColor Yellow
        return $true
    }
    # Capture output and check for success indicators
    $output = & wslc $buildArgs 2>&1 | ForEach-Object { 
        Write-Host $_
        $_  # Pass through for inspection
    }
    # Check if the image was created by looking for the naming line
    $success = $output | Select-String -Pattern "naming to.*$tagName" -Quiet
    return $success
}

Write-Host "Trying SDK $compileSdk..." -ForegroundColor Yellow
$success = Invoke-Build -SdkVersion $compileSdk

if (-not $success -and $compileSdk -ne $fallbackSdk) {
    Write-Host "⚠ SDK $compileSdk not available, falling back to SDK $fallbackSdk" -ForegroundColor Yellow
    $success = Invoke-Build -SdkVersion $fallbackSdk
}

if (-not $success) {
    throw "Build failed"
}

Write-Host "✓ $tagName built successfully" -ForegroundColor Green
