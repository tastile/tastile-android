param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$Command
)

$ErrorActionPreference = "Stop"

$imageName = "tastile-android-dev"
$containerFile = Join-Path $PSScriptRoot "../.wslc/Containerfile"
$repositoryRoot = Resolve-Path (Join-Path $PSScriptRoot "..")

if (-not $Command) {
    $Command = @("bash")
}

wslc build `
    --tag $imageName `
    --file $containerFile `
    (Split-Path $containerFile)

if ($LASTEXITCODE -ne 0) {
    exit $LASTEXITCODE
}

$commandText = $Command -join " "

wslc run `
    --rm `
    --interactive `
    --tty `
    --volume "${repositoryRoot}:/workspace" `
    $imageName `
    bash -lc "cd /workspace && $commandText"

exit $LASTEXITCODE