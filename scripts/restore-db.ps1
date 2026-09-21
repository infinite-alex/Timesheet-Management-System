<#
.SYNOPSIS
  Restaureaza un backup creat de backup-db.ps1 intr-o baza de date NOUA (nu suprascrie timesheetdb).

.DESCRIPTION
  Creeaza baza -TargetDatabase si incarca backup-ul in ea, ca sa poti verifica datele.
  Refuza sa scrie peste o baza existenta. Ca sa inlocuiesti baza reala, opreste aplicatia,
  redenumeste-o din psql (ALTER DATABASE ... RENAME TO ...) si redenumeste baza restaurata in timesheetdb.

.EXAMPLE
  powershell -File scripts\restore-db.ps1 -BackupFile C:\Users\alexu\Backups\timesheet\timesheetdb-2026-09-21_22-00-00.dump
#>
param(
    [Parameter(Mandatory = $true)][string]$BackupFile,
    [string]$TargetDatabase = 'timesheetdb_restore',
    [string]$DbUser = 'postgres',
    [string]$DbHost = 'localhost',
    [int]$Port = 5432,
    [string]$PgBin = ''
)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path $BackupFile)) { throw "Nu exista fisierul $BackupFile" }
if ($TargetDatabase -notmatch '^[A-Za-z0-9_]+$') { throw 'Numele bazei poate contine doar litere, cifre si underscore.' }

if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User') }
if (-not $env:DB_PASSWORD) { throw 'DB_PASSWORD nu este setat.' }

$bin = $PgBin
if (-not $bin) {
    $cmd = Get-Command pg_restore.exe -ErrorAction SilentlyContinue
    if ($cmd) { $bin = Split-Path $cmd.Source }
}
if (-not $bin) {
    $bin = Get-ChildItem 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Name -replace '\D', '') } -Descending |
        ForEach-Object { Join-Path $_.FullName 'bin' } |
        Where-Object { Test-Path (Join-Path $_ 'pg_restore.exe') } |
        Select-Object -First 1
}
if (-not $bin) { throw 'Nu am gasit pg_restore.exe. Da calea folderului bin prin -PgBin.' }

$env:PGPASSWORD = $env:DB_PASSWORD
try {
    $exists = & (Join-Path $bin 'psql.exe') -h $DbHost -p $Port -U $DbUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$TargetDatabase'"
    if ($exists -match '1') { throw "Baza $TargetDatabase exista deja. Alege alt nume sau sterge-o mai intai." }

    & (Join-Path $bin 'createdb.exe') -h $DbHost -p $Port -U $DbUser $TargetDatabase
    if ($LASTEXITCODE -ne 0) { throw 'createdb a esuat.' }

    & (Join-Path $bin 'pg_restore.exe') -h $DbHost -p $Port -U $DbUser -d $TargetDatabase --no-owner $BackupFile
    if ($LASTEXITCODE -ne 0) { throw 'pg_restore a raportat erori.' }

    Write-Host "Backup restaurat in baza '$TargetDatabase'." -ForegroundColor Green
} finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}
