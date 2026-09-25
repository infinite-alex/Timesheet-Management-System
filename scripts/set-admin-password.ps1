<#
.SYNOPSIS
  Salveaza (sau sterge) parola initiala a contului admin in variabila ADMIN_PASSWORD a contului Windows curent.

.DESCRIPTION
  Aplicatia foloseste ADMIN_PASSWORD o singura data: cand creeaza contul admin la prima pornire.
  Dupa prima logare in aplicatie, sterge variabila cu -Remove (parola nu mai trebuie sa stea in Windows).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\set-admin-password.ps1
  powershell -ExecutionPolicy Bypass -File scripts\set-admin-password.ps1 -Remove
#>
param([switch]$Remove)

$ErrorActionPreference = 'Stop'
$minLength = 12

if ($Remove) {
    [Environment]::SetEnvironmentVariable('ADMIN_PASSWORD', $null, 'User')
    Write-Host "OK  ADMIN_PASSWORD a fost sters de pe contul $env:USERNAME. Contul admin din aplicatie ramane cu parola lui." -ForegroundColor Green
    exit 0
}

function Read-Secret([string]$prompt) {
    $secure = Read-Host $prompt -AsSecureString
    [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

Write-Host 'Lipeste parola contului admin din PAROLE.txt, sectiunea 2 (Ctrl+V sau click dreapta; nu se vede nimic - e normal).'
$first = Read-Secret 'Parola admin'
if ($first.Length -lt $minLength) {
    Write-Host "PROBLEMA  Parola are $($first.Length) caractere; trebuie cel putin $minLength." -ForegroundColor Red
    exit 1
}
$second = Read-Secret 'Inca o data, pentru verificare'
if ($first -ne $second) {
    Write-Host 'PROBLEMA  Cele doua parole nu coincid. Ruleaza din nou.' -ForegroundColor Red
    exit 1
}

[Environment]::SetEnvironmentVariable('ADMIN_PASSWORD', $first, 'User')
Write-Host "OK  ADMIN_PASSWORD salvat pentru contul $env:USERNAME." -ForegroundColor Green
Write-Host '    La prima pornire, aplicatia creeaza contul admin cu aceasta parola.'
