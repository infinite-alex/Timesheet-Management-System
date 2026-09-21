<#
.SYNOPSIS
  Sterge sarcinile programate create de install-autostart.ps1 (ruleaza ca administrator).
#>
$ErrorActionPreference = 'Stop'
foreach ($name in 'Timesheet-Server', 'Timesheet-Backup') {
    if (Get-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue) {
        Stop-ScheduledTask -TaskName $name -ErrorAction SilentlyContinue
        Unregister-ScheduledTask -TaskName $name -Confirm:$false
        Write-Host "Sters: $name"
    } else {
        Write-Host "Nu exista: $name"
    }
}
Write-Host 'Daca aplicatia mai ruleaza, opreste procesul java din Task Manager sau reporneste calculatorul.'
