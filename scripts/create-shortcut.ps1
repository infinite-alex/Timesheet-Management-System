<#
.SYNOPSIS
  Creeaza scurtatura "Pontaj.url" care deschide aplicatia in browser.

.DESCRIPTION
  Ruleaza-l pe SERVER: detecteaza adresa IP a serverului si scrie Pontaj.url (implicit pe Desktop).
  Fisierul rezultat se copiaza apoi pe desktopul fiecarui laptop (stick USB, folder partajat).
  Nu se instaleaza nimic pe laptopuri; scurtatura doar deschide browserul la adresa serverului.

  Daca serverul nu are IP fix, scurtatura nu mai merge dupa ce IP-ul se schimba.
  Cere adminului o rezervare de IP in router, sau da -ServerUrl cu numele serverului.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\create-shortcut.ps1
  powershell -ExecutionPolicy Bypass -File scripts\create-shortcut.ps1 -ServerUrl "http://SERVER-FIRMA:8080"
  powershell -ExecutionPolicy Bypass -File scripts\create-shortcut.ps1 -OutDir "\\SERVER-FIRMA\Comun"
#>
param(
    [string]$ServerUrl = '',
    [int]$Port         = 8080,
    [string]$OutDir    = [Environment]::GetFolderPath('Desktop'),
    [string]$Name      = 'Pontaj'
)

$ErrorActionPreference = 'Stop'

if (-not $ServerUrl) {
    # Adresa placii de retea care iese spre router (are default gateway), nu cea de loopback sau de VPN.
    $ip = Get-NetIPConfiguration |
        Where-Object { $_.IPv4DefaultGateway -and $_.NetAdapter.Status -eq 'Up' } |
        ForEach-Object { $_.IPv4Address.IPAddress } |
        Select-Object -First 1
    if (-not $ip) { throw 'Nu am gasit adresa IP a serverului. Da adresa prin -ServerUrl.' }
    $ServerUrl = "http://${ip}:$Port"
}
$ServerUrl = $ServerUrl.TrimEnd('/')

New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
$file = Join-Path $OutDir "$Name.url"
$content = @(
    '[InternetShortcut]'
    "URL=$ServerUrl/"
)
Set-Content -Path $file -Value $content -Encoding ASCII

Write-Host "Scurtatura creata: $file" -ForegroundColor Green
Write-Host "Deschide: $ServerUrl/"
Write-Host 'Copiaz-o pe desktopul fiecarui laptop. Verifica intai ca linkul merge de pe un laptop din birou.'
