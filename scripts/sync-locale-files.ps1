# Sync locale files with master, preserving translations and escaping XML special chars.
Add-Type -AssemblyName System.Xml

$MasterDir = "C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\res\values"

$ResourceFiles = @(
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

$Locales = @("values-de", "values-fr", "values-pt-rBR", "values-es", "values-ja", "values-ko", "values-zh-rCN")

function Escape-Xml {
    param([string]$Text)
    if ($null -eq $Text) { return "" }
    return $Text.Replace("&", "&amp;").Replace("<", "&lt;").Replace(">", "&gt;")
}

function Read-StringMap {
    param([string]$Path)
    $result = [ordered]@{}
    if (-not (Test-Path $Path)) { return $result }
    try {
        $xml = New-Object System.Xml.XmlDocument
        $xml.Load($Path)
        foreach ($node in $xml.SelectNodes("/resources/string")) {
            $name = $node.GetAttribute("name")
            $attrs = @{}
            foreach ($attr in $node.Attributes) {
                if ($attr.Name -ne "name") {
                    $attrs[$attr.Name] = $attr.Value
                }
            }
            $result[$name] = @{ Value = $node.InnerText; Attrs = $attrs }
        }
        foreach ($node in $xml.SelectNodes("/resources/plurals")) {
            $name = $node.GetAttribute("name")
            $body = $node.InnerXml
            $result[$name] = @{ Value = $body; Attrs = @{}; IsPlurals = $true }
        }
    } catch {
        Write-Host "Error reading $Path : $_"
    }
    return $result
}

function Build-Xml {
    param(
        [System.Collections.Specialized.OrderedDictionary]$MasterMap,
        [hashtable]$LocaleMap
    )
    $sb = New-Object System.Text.StringBuilder
    [void]$sb.AppendLine('<?xml version="1.0" encoding="utf-8"?>')
    [void]$sb.AppendLine('<resources>')
    foreach ($key in $MasterMap.Keys) {
        $masterEntry = $MasterMap[$key]
        $useEntry = $masterEntry
        if ($LocaleMap.ContainsKey($key)) {
            $useEntry = $LocaleMap[$key]
        }
        $attrParts = @()
        if ($masterEntry.Attrs.ContainsKey("translatable") -and $masterEntry.Attrs["translatable"] -eq "false") {
            $attrParts += 'translatable="false"'
        }
        $attrStr = ""
        if ($attrParts.Count -gt 0) {
            $attrStr = " " + ($attrParts -join " ")
        }
        if ($useEntry.IsPlurals) {
            # Don't escape the plural body - it contains <item> elements
            [void]$sb.AppendLine("    <plurals name=`"$key`"$attrStr>$($useEntry.Value)</plurals>")
        } else {
            $val = Escape-Xml -Text $useEntry.Value
            [void]$sb.AppendLine("    <string name=`"$key`"$attrStr>$val</string>")
        }
    }
    [void]$sb.AppendLine('</resources>')
    return $sb.ToString()
}

foreach ($locale in $Locales) {
    $localeDir = "C:\Users\rebui\Desktop\tastile\tastile-android\app\src\main\res\$locale"
    if (-not (Test-Path $localeDir)) {
        New-Item -ItemType Directory -Path $localeDir | Out-Null
    }
    foreach ($file in $ResourceFiles) {
        $masterPath = Join-Path $MasterDir $file
        $localePath = Join-Path $localeDir $file
        if (-not (Test-Path $masterPath)) { continue }

        $masterMap = Read-StringMap -Path $masterPath
        $localeMap = Read-StringMap -Path $localePath
        $output = Build-Xml -MasterMap $masterMap -LocaleMap $localeMap
        Set-Content -Path $localePath -Value $output -Encoding UTF8
    }
    Write-Host "Synced locale: $locale"
}
