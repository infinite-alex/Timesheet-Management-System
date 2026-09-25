<#
.SYNOPSIS
  Instaleaza pornirea automata a aplicatiei si backup-ul zilnic, ca sarcini programate Windows.

.DESCRIPTION
  Ruleaza-l O SINGURA DATA, dintr-un PowerShell deschis "ca administrator" (click dreapta > Run as administrator):
      cd C:\Users\alexu\Timesheet-Management-System
      powershell -ExecutionPolicy Bypass -File scripts\install-autostart.ps1

  Creeaza doua sarcini, rulate ca utilizatorul curent (fara parola stocata, fara fereastra):
    Timesheet-Server  - porneste aplicatia la pornirea calculatorului (chiar daca nimeni nu e logat) si o reporneste daca cade.
    Timesheet-Backup  - ruleaza scripts\backup-db.ps1 in fiecare zi la ora -BackupAt (si la prima ocazie, daca calculatorul era oprit).

  Sterge-le cu scripts\uninstall-autostart.ps1.
#>
param(
    [string]$BackupAt = '22:00',
    [string]$BackupDir = (Join-Path $env:USERPROFILE 'Backups\timesheet'),
    [string]$BackupCopyTo = '',
    [switch]$SkipServer,
    [switch]$SkipBackup
)

$ErrorActionPreference = 'Stop'
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$identity).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Ruleaza acest script dintr-un PowerShell deschis ca administrator.'
}

$scripts = $PSScriptRoot
$root = Split-Path $scripts -Parent
$user = $identity.Name

# parolele trebuie sa fie pe ACELASI cont Windows cu care se creeaza sarcinile
$dbPassword = @('User', 'Machine') | ForEach-Object { [Environment]::GetEnvironmentVariable('DB_PASSWORD', $_) } | Where-Object { $_ } | Select-Object -First 1
if (-not $dbPassword) {
    throw "DB_PASSWORD nu este setat pentru contul $user. Ruleaza intai pregatirea bazei de date (setup-database.ps1) logat cu acest cont. Daca ai aprobat fereastra de administrator cu ALT cont, logheaza-te pe server cu contul ales pentru aplicatie."
}
if (-not $SkipServer -and -not (@('User', 'Machine') | ForEach-Object { [Environment]::GetEnvironmentVariable('ADMIN_PASSWORD', $_) } | Where-Object { $_ })) {
    Write-Host "ATENTIE  ADMIN_PASSWORD nu este setat pentru $user. Daca e prima instalare, aplicatia genereaza o parola de admin aleatorie si o scrie o singura data in logs\console.log." -ForegroundColor Yellow
}
$principal = New-ScheduledTaskPrincipal -UserId $user -LogonType S4U -RunLevel Limited
$powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'

if (-not $SkipServer) {
    if (-not (Get-ChildItem (Join-Path $root 'target') -Filter 'demo-*.jar' -ErrorAction SilentlyContinue)) {
        Write-Host 'Construiesc aplicatia (prima data dureaza cateva minute)...'
        Push-Location $root
        try { & (Join-Path $root 'mvnw.cmd') -q package -DskipTests; if ($LASTEXITCODE -ne 0) { throw 'Build esuat.' } } finally { Pop-Location }
    }

    $action = New-ScheduledTaskAction -Execute $powershell -WorkingDirectory $root `
        -Argument "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$(Join-Path $scripts 'run-server.ps1')`""
    $trigger = New-ScheduledTaskTrigger -AtStartup
    $trigger.Delay = 'PT30S'
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
        -ExecutionTimeLimit ([TimeSpan]::Zero) -MultipleInstances IgnoreNew -StartWhenAvailable `
        -RestartCount 5 -RestartInterval (New-TimeSpan -Minutes 1)
    Register-ScheduledTask -TaskName 'Timesheet-Server' -Action $action -Trigger $trigger -Settings $settings `
        -Principal $principal -Description 'Porneste aplicatia Timesheet la pornirea calculatorului.' -Force | Out-Null
    Write-Host "Sarcina 'Timesheet-Server' creata." -ForegroundColor Green
}

if (-not $SkipBackup) {
    $arguments = "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$(Join-Path $scripts 'backup-db.ps1')`" -BackupDir `"$BackupDir`""
    if ($BackupCopyTo) { $arguments += " -CopyTo `"$BackupCopyTo`"" }
    $action = New-ScheduledTaskAction -Execute $powershell -Argument $arguments -WorkingDirectory $root
    $trigger = New-ScheduledTaskTrigger -Daily -At $BackupAt
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries `
        -StartWhenAvailable -ExecutionTimeLimit (New-TimeSpan -Minutes 30)
    Register-ScheduledTask -TaskName 'Timesheet-Backup' -Action $action -Trigger $trigger -Settings $settings `
        -Principal $principal -Description 'Backup zilnic al bazei de date Timesheet (pg_dump).' -Force | Out-Null
    Write-Host "Sarcina 'Timesheet-Backup' creata (zilnic la $BackupAt, in $BackupDir)." -ForegroundColor Green
}

if (-not $SkipServer) {
    Start-ScheduledTask -TaskName 'Timesheet-Server'
    Write-Host 'Pornesc aplicatia; verific pe http://localhost:8080/login ...'
    $ok = $false
    for ($i = 0; $i -lt 40 -and -not $ok; $i++) {
        Start-Sleep -Seconds 3
        try { $ok = (Invoke-WebRequest -Uri 'http://localhost:8080/login' -UseBasicParsing -TimeoutSec 3).StatusCode -eq 200 } catch { }
    }
    if ($ok) { Write-Host 'Aplicatia raspunde pe http://localhost:8080' -ForegroundColor Green }
    else { Write-Host "Aplicatia nu raspunde inca. Vezi $(Join-Path $root 'logs\run-server.log') si console-error.log." -ForegroundColor Yellow }
}
