<#
.SYNOPSIS
  Dupa ce ai schimbat codul (git pull / modificari): opreste serverul, reconstruieste jar-ul si il porneste din nou.
  Ruleaza-l ca administrator daca serverul ruleaza ca sarcina programata Timesheet-Server.
#>
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$task = Get-ScheduledTask -TaskName 'Timesheet-Server' -ErrorAction SilentlyContinue

if ($task) { Stop-ScheduledTask -TaskName 'Timesheet-Server' -ErrorAction SilentlyContinue }

# jar-ul ruland ar bloca rescrierea fisierului: opreste orice java pornit din acest proiect
Get-CimInstance Win32_Process -Filter "Name='java.exe'" |
    Where-Object { $_.CommandLine -like "*$root\target\demo-*" } |
    ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
Start-Sleep -Seconds 2

Push-Location $root
try {
    & (Join-Path $root 'mvnw.cmd') -q package -DskipTests
    if ($LASTEXITCODE -ne 0) { throw 'Build esuat; serverul NU a fost repornit.' }
} finally { Pop-Location }

if ($task) {
    Start-ScheduledTask -TaskName 'Timesheet-Server'
    Write-Host 'Serverul a fost reconstruit si repornit.' -ForegroundColor Green
} else {
    Write-Host "Build reusit. Sarcina Timesheet-Server nu exista; porneste manual: powershell -File scripts\run-server.ps1"
}
