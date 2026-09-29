[CmdletBinding()]
param(
    [ValidateSet('dev', 'staging', 'prod')]
    [string]$Environment = 'dev',

    [ValidateSet('verify', 'testDebugUnitTest', 'assembleDebug', 'assembleRelease', 'bundleRelease')]
    [string]$Task = 'verify'
)

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$workspaceRoot = Split-Path -Parent $repositoryRoot
$configurationPath = Join-Path $repositoryRoot '.infisical.json'
$configuration = Get-Content -LiteralPath $configurationPath -Raw | ConvertFrom-Json
$projectId = [string]$configuration.projects.$Environment.projectId
$domain = [string]$configuration.domain
if ($projectId -notmatch '^[0-9a-fA-F-]{36}$' -or $domain -notmatch '^https://[^/]+/?$') {
    throw "Invalid Infisical project configuration for $Environment."
}
if (-not (Get-Command infisical -ErrorAction SilentlyContinue)) {
    throw 'Infisical CLI was not found. Install it and authenticate with infisical login.'
}

$temporaryRoot = Join-Path $workspaceRoot '.tmp'
$exitCode = 1
$null = New-Item -ItemType Directory -Path $temporaryRoot -Force
$temporaryDirectory = Join-Path $temporaryRoot "infisical-gradle-$([guid]::NewGuid().ToString('N'))"
$null = New-Item -ItemType Directory -Path $temporaryDirectory
$cliConfiguration = @{
    workspaceId = $projectId
    domain = $domain.TrimEnd('/')
    defaultEnvironment = ''
    gitBranchToEnvironmentMapping = $null
} | ConvertTo-Json
[System.IO.File]::WriteAllText(
    (Join-Path $temporaryDirectory '.infisical.json'),
    $cliConfiguration,
    [System.Text.UTF8Encoding]::new($false)
)

try {
    Push-Location -LiteralPath $repositoryRoot
    try {
        & infisical "--domain=$domain" run "--project-config-dir=$temporaryDirectory" `
            "--env=$Environment" --path=/tastile/android -- .\gradlew.bat $Task
        $exitCode = $LASTEXITCODE
    } finally {
        Pop-Location
    }
} finally {
    if (Test-Path -LiteralPath $temporaryDirectory) {
        Remove-Item -LiteralPath $temporaryDirectory -Recurse -Force
    }
}
if ($exitCode -ne 0) {
    exit $exitCode
}
