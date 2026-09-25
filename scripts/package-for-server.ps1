<#
.SYNOPSIS
  Face un zip cu aplicatia, gata de copiat pe serverul firmei (stick USB).

.DESCRIPTION
  Copiaza fisierele proiectului urmarite de git (plus cele noi, neignorate) intr-un folder temporar,
  construieste acolo jar-ul (cu teste) si pune in zip sursele, scripturile si jar-ul.
  NU include datele clientilor (05.2026\, *.xlsx, *.zip), jurnalele sau notele interne - sunt in .gitignore.
  Build-ul se face in copie, deci nu atinge jar-ul folosit de serverul pornit pe acest calculator.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\package-for-server.ps1
  powershell -ExecutionPolicy Bypass -File scripts\package-for-server.ps1 -OutFile E:\Timesheet-server.zip
  powershell -ExecutionPolicy Bypass -File scripts\package-for-server.ps1 -SkipTests
#>
param(
    [string]$OutFile = (Join-Path ([Environment]::GetFolderPath('Desktop')) 'Timesheet-server.zip'),
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$staging = Join-Path $env:TEMP "timesheet-package-$(Get-Date -Format 'yyyyMMddHHmmss')"

Push-Location $root
try {
    # fisierele urmarite + cele noi neignorate; .gitignore tine afara datele clientilor
    $files = @(git ls-files) + @(git ls-files --others --exclude-standard) |
        Where-Object { $_ -and (Test-Path $_ -PathType Leaf) } | Sort-Object -Unique
    if ($LASTEXITCODE -ne 0 -or -not $files) { throw 'git ls-files a esuat; ruleaza din folderul proiectului, cu git instalat.' }

    foreach ($f in $files) {
        $dest = Join-Path $staging $f
        New-Item -ItemType Directory -Force -Path (Split-Path $dest -Parent) | Out-Null
        Copy-Item $f $dest
    }
} finally {
    Pop-Location
}

try {
    Push-Location $staging
    try {
        Write-Host 'Construiesc jar-ul intr-o copie a proiectului...'
        $mvnArgs = @('-q', 'package')
        if ($SkipTests) { $mvnArgs += '-DskipTests' }
        & (Join-Path $staging 'mvnw.cmd') @mvnArgs
        if ($LASTEXITCODE -ne 0) { throw 'Build esuat (sau au picat teste); nu am facut zip-ul.' }
    } finally {
        Pop-Location
    }

    $target = Join-Path $staging 'target'
    $jar = Get-ChildItem $target -Filter 'demo-*.jar' | Where-Object { $_.Name -notmatch 'original' } | Select-Object -First 1
    if (-not $jar) { throw 'Build-ul nu a produs un jar.' }

    # in zip ramane din target\ doar jar-ul (fara clase compilate si rapoarte de teste)
    $jarCopy = Join-Path $env:TEMP $jar.Name
    Move-Item $jar.FullName $jarCopy -Force
    Remove-Item $target -Recurse -Force
    New-Item -ItemType Directory -Path $target | Out-Null
    Move-Item $jarCopy (Join-Path $target $jar.Name)

    if (Test-Path $OutFile) { Remove-Item $OutFile -Force }
    # Compress-Archive din PowerShell 5.1 scrie caile cu "\"; unele programe de dezarhivare nu mai creeaza
    # atunci folderul scripts\. Scriem noi intrarile, cu "/" (formatul standard zip).
    Add-Type -AssemblyName System.IO.Compression, System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::Open($OutFile, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($file in Get-ChildItem $staging -Recurse -File) {
            $entry = $file.FullName.Substring($staging.Length + 1).Replace('\', '/')
            [void][IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $file.FullName, $entry, [IO.Compression.CompressionLevel]::Optimal)
        }
    } finally {
        $zip.Dispose()
    }
    $size = [math]::Round((Get-Item $OutFile).Length / 1MB, 1)
    Write-Host "Gata: $OutFile ($size MB, $($files.Count) fisiere + $($jar.Name))" -ForegroundColor Green
} finally {
    if (Test-Path $staging) { Remove-Item $staging -Recurse -Force }
}
