<#
.SYNOPSIS
  Deschide portul aplicatiei (8080) in firewall pentru retelele Domain si Private si afiseaza linkul pentru angajati.

.DESCRIPTION
  Ruleaza-l ca administrator. Se poate rula de mai multe ori: nu dubleaza regula.
  Nu schimba tipul retelei; daca reteaua e "Public", regula nu se aplica si scriptul iti spune asta.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\open-firewall.ps1
#>
param([int]$Port = 8080)

$ErrorActionPreference = 'Stop'
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$identity).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host 'PROBLEMA  Ruleaza ca administrator.' -ForegroundColor Red
    exit 1
}

$name = "Timesheet $Port"
if (Get-NetFirewallRule -DisplayName $name -ErrorAction SilentlyContinue) {
    Write-Host "OK  Regula de firewall '$name' exista deja." -ForegroundColor Green
} else {
    New-NetFirewallRule -DisplayName $name -Direction Inbound -Protocol TCP -LocalPort $Port -Action Allow -Profile Domain, Private | Out-Null
    Write-Host "OK  Regula de firewall '$name' creata (retele Domain si Private)." -ForegroundColor Green
}

$public = $false
foreach ($net in Get-NetConnectionProfile) {
    $category = $net.NetworkCategory
    if ($category -eq 'Public') {
        $public = $true
        Write-Host "ATENTIE  Reteaua '$($net.Name)' ($($net.InterfaceAlias)) este Public: regula NU se aplica pe ea." -ForegroundColor Yellow
    } else {
        Write-Host "OK  Reteaua '$($net.Name)' ($($net.InterfaceAlias)) este $category." -ForegroundColor Green
    }
}
if ($public) {
    Write-Host '    Roaga adminul firmei sa treaca reteaua biroului pe Private, apoi ruleaza din nou.' -ForegroundColor Yellow
}

$ip = Get-NetIPConfiguration |
    Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } |
    ForEach-Object { $_.IPv4Address.IPAddress } |
    Select-Object -First 1
Write-Host ''
if ($ip) {
    Write-Host "Linkul pentru angajati: http://${ip}:$Port" -ForegroundColor Cyan
    Write-Host 'Deschide-l de pe un laptop din birou: trebuie sa apara pagina de login.'
} else {
    Write-Host 'Nu am gasit adresa IP a serverului (ruleaza ipconfig).' -ForegroundColor Yellow
}
if ($public) { exit 2 }
