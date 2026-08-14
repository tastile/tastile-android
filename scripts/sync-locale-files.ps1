# Sync existing locale files (es, ja, ko, zh-rCN) with the master values/.
# For each existing locale:
#   - Copy the master file as a starting point.
#   - For each <string> in master that is already present in the locale, keep the locale's translation.
#   - Add missing keys with the master value as fallback so lint is happy.
# This preserves existing translations while adding missing keys.

param(
    [string]$MasterDir = "C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\res\values",
    [string[]]$Locales = @("values-de", "values-fr", "values-pt-rBR", "values-es", "values-ja", "values-ko", "values-zh-rCN")
)

# Resources to sync
$resourceFiles = @(
    "app_common.xml",
    "app_language.xml",
    "app_shell.xml",
    "features_calendar.xml",
    "features_dashboard.xml",
    "features_memo.xml",
    "features_projects.xml",
    "features_quickcreate.xml",
    "features_schedule.xml",
    "features_tasks.xml",
    "features_tiles.xml",
    "features_timeline.xml",
    "plurals.xml",
    "system_account.xml",
    "system_auth.xml",
    "system_execution.xml",
    "system_notifications.xml",
    "system_preferences.xml",
    "system_prompt.xml",
    "system_settings.xml"
)

# Regex to extract <string name="..." ...>...</string> entries
$stringPattern = '<string\s+name="(?<name>[^"]+)"(?<attrs>(?:\s+[^>]*?)?)>(?<value>.*?)</string>'

function Get-StringMap {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        return @{}
    }
    $content = Get-Content $Path -Raw
    $map = @{}
    [regex]::Matches($content, $stringPattern, [System.Text.RegularExpressions.RegexOptions]::Singleline) | ForEach-Object {
        $name = $_.Groups["name"].Value
        $attrs = $_.Groups["attrs"].Value
        $value = $_.Groups["value"].Value
        $map[$name] = @{ Value = $value; Attributes = $attrs }
    }
    return $map
}

function Get-PluralMap {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        return @{}
    }
    $content = Get-Content $Path -Raw
    $map = @{}
    $pattern = '<plurals\s+name="(?<name>[^"]+)">(?<body>.*?)</plurals>'
    [regex]::Matches($content, $pattern, [System.Text.RegularExpressions.RegexOptions]::Singleline) | ForEach-Object {
        $name = $_.Groups["name"].Value
        $body = $_.Groups["body"].Value
        $map[$name] = $body
    }
    return $map
}

foreach ($locale in $Locales) {
    $localeDir = "C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\res\$locale"
    if (-not (Test-Path $localeDir)) {
        New-Item -ItemType Directory -Path $localeDir | Out-Null
    }

    foreach ($file in $resourceFiles) {
        $masterPath = Join-Path $MasterDir $file
        $localePath = Join-Path $localeDir $file
        if (-not (Test-Path $masterPath)) {
            continue
        }

        if ($file -eq "plurals.xml") {
            # Sync plurals entries
            $masterMap = Get-PluralMap -Path $masterPath
            $localeMap = Get-PluralMap -Path $localePath

            # Build the output XML
            $output = "<resources>`n"
            foreach ($key in $masterMap.Keys) {
                if ($localeMap.ContainsKey($key)) {
                    $body = $localeMap[$key]
                } else {
                    $body = $masterMap[$key]
                }
                $output += "    <plurals name=`"$key`">$body</plurals>`n"
            }
            $output += "</resources>`n"
            Set-Content -Path $localePath -Value $output -Encoding UTF8
        }
        else {
            # Sync string entries
            $masterMap = Get-StringMap -Path $masterPath
            $localeMap = Get-StringMap -Path $localePath

            # Build the output XML preserving XML header
            $masterContent = Get-Content $masterPath -Raw
            $xmlDeclaration = ""
            if ($masterContent -match '^<\?xml[^?]+\?>') {
                $xmlDeclaration = $Matches[0] + "`n"
            }

            # Detect XML comment block before <resources>
            $commentBlock = ""
            if ($masterContent -match '(?s)(<!--.*?-->)\s*<resources>') {
                $commentBlock = $Matches[1] + "`n"
            }

            # Build output: start with xmlDeclaration + commentBlock + opening <resources>
            $output = $xmlDeclaration + $commentBlock + "<resources>`n"

            foreach ($key in $masterMap.Keys) {
                if ($localeMap.ContainsKey($key)) {
                    $attrs = $localeMap[$key].Attributes
                    $value = $localeMap[$key].Value
                } else {
                    $attrs = $masterMap[$key].Attributes
                    $value = $masterMap[$key].Value
                }
                # Build attr string. Preserve translatable attribute from master if locale didn't override.
                $attrStr = ""
                if ($attrs -match 'translatable="false"') {
                    $attrStr = ' translatable="false"'
                }
                $output += "    <string name=`"$key`"$attrStr>$value</string>`n"
            }
            $output += "</resources>`n"
            Set-Content -Path $localePath -Value $output -Encoding UTF8
        }
    }
    Write-Host "Synced locale files for $locale"
}
