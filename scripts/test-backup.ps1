<#
.SYNOPSIS
  Porneste acum sarcina programata Timesheet-Backup si arata rezultatul din backup.log.

.DESCRIPTION
  Ruleaza-l ca administrator, dupa install-autostart.ps1. Asteapta pana termina backup-ul (max. 5 minute)
  si afiseaza ultimele randuri din jurnal: trebuie sa apara "OK" si, daca ai ales o copie pe alt disc, "Copiat si in".

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\test-backup.ps1
#>
$ErrorActionPreference = 'Stop'
$taskName = 'Timesheet-Backup'

$task = Get-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
if (-not $task) {
    Write-Host "PROBLEMA  Sarcina $taskName nu exista. Ruleaza intai pasul de pornire automata (install-autostart)." -ForegroundColor Red
    exit 1
}

# folderul de backup e in argumentele sarcinii (-BackupDir "...")
$arguments = $task.Actions[0].Arguments
$match = [regex]::Match($arguments, '-BackupDir "([^"]+)"')
$backupDir = if ($match.Success) { $match.Groups[1].Value } else { Join-Path $env:USERPROFILE 'Backups\timesheet' }
$log = Join-Path $backupDir 'backup.log'
$before = if (Test-Path $log) { @(Get-Content $log).Count } else { 0 }

Write-Host "Pornesc $taskName ..."
Start-ScheduledTask -TaskName $taskName
Start-Sleep -Seconds 3
$deadline = (Get-Date).AddMinutes(5)
while ((Get-ScheduledTask -TaskName $taskName).State -eq 'Running' -and (Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 2
}

if (-not (Test-Path $log)) {
    Write-Host "PROBLEMA  Nu exista jurnalul $log. Backup-ul nu a pornit." -ForegroundColor Red
    exit 1
}
$new = @(Get-Content $log | Select-Object -Skip $before)
if (-not $new) {
    Write-Host 'PROBLEMA  Backup-ul nu a scris nimic in jurnal. Vezi Task Scheduler -> Timesheet-Backup -> Last Run Result.' -ForegroundColor Red
    exit 1
}
Write-Host ''
Write-Host "Jurnal ($log):"
foreach ($line in $new) {
    $color = if ($line -match 'EROARE') { 'Red' } else { 'Green' }
    Write-Host "  $line" -ForegroundColor $color
}
Write-Host ''
if ($new -match 'EROARE') {
    Write-Host 'Backup-ul a esuat - citeste mesajul EROARE de mai sus.' -ForegroundColor Red
    exit 1
}
if ($arguments -match '-CopyTo' -and -not ($new -match 'Copiat si in')) {
    Write-Host 'ATENTIE  Nu apare copia pe al doilea disc.' -ForegroundColor Yellow
    exit 1
}
Write-Host 'Backup-ul merge.' -ForegroundColor Green
