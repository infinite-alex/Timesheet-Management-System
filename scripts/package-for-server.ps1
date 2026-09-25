<#
.SYNOPSIS
  Face un zip cu aplicatia, gata de copiat pe serverul firmei (stick USB).

.DESCRIPTION
  Pune in zip doar fisierele proiectului urmarite de git (plus cele noi, neignorate) si jar-ul din target\.
  NU include datele clientilor (05.2026\, *.xlsx, *.zip), jurnalele sau notele interne - sunt in .gitignore.
  Nu construieste jar-ul (ar fi blocat de serverul pornit pe acest calculator); refuza daca jar-ul e mai vechi
  decat codul din src\. Atunci ruleaza intai scripts\update-server.ps1 (reconstruieste si reporneste).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\package-for-server.ps1
  powershell -ExecutionPolicy Bypass -File scripts\package-for-server.ps1 -OutFile E:\Timesheet-server.zip
#>
param(
    [string]$OutFile = (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Timesheet-server.zip')
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$staging = Join-Path $env:TEMP "timesheet-package-$(Get-Date -Format 'yyyyMMddHHmmss')"

Push-Location $root
try {
    $jar = Get-ChildItem (Join-Path $root 'target') -Filter 'demo-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'original' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if (-not $jar) { throw 'Nu exista jar-ul in target\. Ruleaza intai: mvnw.cmd package' }

    $newest = Get-ChildItem (Join-Path $root 'src\main'), (Join-Path $root 'pom.xml') -Recurse -File |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($newest.LastWriteTime -gt $jar.LastWriteTime) {
        throw "Jar-ul e mai vechi decat $($newest.Name). Ruleaza intai scripts\update-server.ps1 (ca administrator)."
    }

    # fisierele urmarite + cele noi neignorate; .gitignore tine afara datele clientilor
    $files = @(git ls-files) + @(git ls-files --others --exclude-standard) |
        Where-Object { $_ -and (Test-Path $_ -PathType Leaf) } | Sort-Object -Unique
    if ($LASTEXITCODE -ne 0 -or -not $files) { throw 'git ls-files a esuat; ruleaza din folderul proiectului, cu git instalat.' }

    foreach ($f in $files) {
        $dest = Join-Path $staging $f
        New-Item -ItemType Directory -Force -Path (Split-Path $dest -Parent) | Out-Null
        Copy-Item $f $dest
    }
    New-Item -ItemType Directory -Force -Path (Join-Path $staging 'target') | Out-Null
    Copy-Item $jar.FullName (Join-Path $staging 'target')

    if (Test-Path $OutFile) { Remove-Item $OutFile -Force }
    Compress-Archive -Path (Join-Path $staging '*') -DestinationPath $OutFile
    $size = [math]::Round((Get-Item $OutFile).Length / 1MB, 1)
    Write-Host "Gata: $OutFile ($size MB, $($files.Count) fisiere + $($jar.Name))" -ForegroundColor Green
} finally {
    Pop-Location
    if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
}
