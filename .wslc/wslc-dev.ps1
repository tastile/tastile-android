<#
.SYNOPSIS
    Start tastile-android development container with ADB support.

.DESCRIPTION
    Starts an Android development container with ADB for app installation and debugging.
    Supports USB passthrough via usbipd and ADB over TCP/IP.

.EXAMPLE
    .wslc/wslc-dev.ps1
    .wslc/wslc-dev.ps1 -Rebuild
    .wslc/wslc-dev.ps1 -DeviceIp 192.168.1.100
#>
[CmdletBinding()]
param(
    [switch]$Rebuild,
    [switch]$WhatIf,
    [string]$DeviceIp
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir

Write-Host "=== tastile-android Development Environment ===" -ForegroundColor Cyan

# ── Configuration ───────────────────────────────────────────────────
$container = "tastile-android-dev"
$adbPort = 5037

# ── Helper: stop & remove container if exists ───────────────────────
function Remove-Container($name) {
    $existing = & wslc container list 2>&1 | Select-String $name
    if ($existing) {
        & wslc stop $name 2>&1 | Out-Null
        & wslc remove $name 2>&1 | Out-Null
    }
}

# ── Helper: attach USB device to WSL2 ───────────────────────────────
function Attach-UsbDevice {
    Write-Host "  Checking for USB devices..." -ForegroundColor Yellow
    
    # Check if usbipd is available
    $usbipdAvailable = $null -ne (Get-Command usbipd -ErrorAction SilentlyContinue)
    if (-not $usbipdAvailable) {
        Write-Host "  usbipd not installed. Install with: winget install usbipd" -ForegroundColor DarkGray
        return $false
    }
    
    # List USB devices
    $devices = & usbipd list 2>&1 | Select-String -Pattern "Android|Google|Samsung|OnePlus|Pixel" -CaseSensitive:$false
    
    if ($devices) {
        Write-Host "  Found Android device(s):" -ForegroundColor Green
        $devices | ForEach-Object { Write-Host "    $_" -ForegroundColor DarkGray }
        
        # Try to attach first unattached device
        $unattached = & usbipd list 2>&1 | Select-String -Pattern "Not attached" -Context 0,0
        if ($unattached) {
            $busid = ($unattached -split '\s+')[0]
            Write-Host "  Attaching device on bus $busid..." -ForegroundColor Yellow
            & usbipd attach --wsl --busid $busid 2>&1
            if ($LASTEXITCODE -eq 0) {
                Write-Host "  ✓ Device attached to WSL2" -ForegroundColor Green
                return $true
            } else {
                Write-Host "  ⚠ Failed to attach device" -ForegroundColor Yellow
            }
        }
    } else {
        Write-Host "  No Android USB devices found" -ForegroundColor DarkGray
    }
    return $false
}

# ── Step 1: Build image if needed ───────────────────────────────────
Write-Host "`n[1/3] Building image..." -ForegroundColor Yellow

$buildScript = Join-Path $ScriptDir "wslc-build.ps1"
if ($Rebuild) {
    & $buildScript -NoCache
} else {
    $imageExists = & wslc images 2>&1 | Select-String "tastile-android-dev"
    if (-not $imageExists) {
        & $buildScript
    } else {
        Write-Host "  ✓ Image already built (use -Rebuild to rebuild)" -ForegroundColor Green
    }
}

# ── Step 2: Setup ADB connection ────────────────────────────────────
Write-Host "`n[2/3] Setting up ADB..." -ForegroundColor Yellow

if ($DeviceIp) {
    Write-Host "  Connecting to device at ${DeviceIp}:5555..." -ForegroundColor Yellow
    & adb connect "${DeviceIp}:5555" 2>&1
    if ($LASTEXITCODE -eq 0) {
        Write-Host "  ✓ Connected to device" -ForegroundColor Green
    } else {
        Write-Host "  ⚠ Could not connect. Ensure ADB TCP/IP is enabled on device." -ForegroundColor Yellow
    }
} else {
    # Try USB passthrough
    $usbAttached = Attach-UsbDevice
    
    if (-not $usbAttached) {
        Write-Host "  No USB device attached. Options:" -ForegroundColor Yellow
        Write-Host "    1. Connect device via USB and run this script again" -ForegroundColor DarkGray
        Write-Host "    2. Use -DeviceIp <ip> for wireless ADB" -ForegroundColor DarkGray
        Write-Host "    3. Manually: adb connect <device-ip>:5555" -ForegroundColor DarkGray
    }
}

# ── Step 3: Start container ─────────────────────────────────────────
Write-Host "`n[3/3] Starting container..." -ForegroundColor Yellow

Remove-Container $container

$runArgs = @(
    "run", "-d",
    "--name", $container,
    "-v", "${RepoRoot}:/workspace",
    "-p", "${adbPort}:5037",
    "tastile-android-dev:latest"
)

if ($WhatIf) {
    Write-Host "  [DRY RUN] wslc $($runArgs -join ' ')" -ForegroundColor DarkGray
    Write-Host "`n=== Dry Run Complete ===" -ForegroundColor Cyan
    return
}

& wslc $runArgs
if ($LASTEXITCODE) { throw "Failed to start container" }

Write-Host "  ✓ Container started" -ForegroundColor Green

# Wait for ADB server
Start-Sleep -Seconds 3

# Check ADB status inside container
Write-Host "  Checking ADB status..." -ForegroundColor Yellow
$adbStatus = & wslc exec $container adb devices 2>&1
Write-Host "  $adbStatus" -ForegroundColor DarkGray

Write-Host "`n=== Development Environment Ready ===" -ForegroundColor Green
Write-Host "Container: $container"
Write-Host "ADB Port: $adbPort"
Write-Host ""
Write-Host "Useful commands:"
Write-Host "  wslc exec $container adb devices              # List devices"
Write-Host "  wslc exec $container adb install app.apk      # Install APK"
Write-Host "  wslc exec $container adb push local remote    # Push file"
Write-Host "  wslc exec $container bash                     # Enter container"
Write-Host "  wslc exec $container ./gradlew assembleDebug  # Build debug APK"
Write-Host "  wslc stop $container                           # Stop container"
Write-Host ""
Write-Host "For wireless ADB:"
Write-Host "  wslc exec $container adb connect <device-ip>:5555"
