<#
.SYNOPSIS
  Backup pentru baza de date PostgreSQL a aplicatiei (pg_dump, format custom, comprimat).

.DESCRIPTION
  - Citeste parola din variabila de mediu DB_PASSWORD (si din cea a utilizatorului, daca sesiunea nu o are).
  - Scrie un fisier timesheetdb-AAAA-LL-ZZ_OO-MM-SS.dump in -BackupDir.
  - Verifica fisierul cu pg_restore --list; un backup care nu se poate citi este sters si scriptul esueaza.
  - Sterge backup-urile mai vechi de -KeepDays zile, dar pastreaza intotdeauna cele mai recente 7.
  - Optional copiaza backup-ul si in -CopyTo (ex. un hard extern sau un alt disc).
  - Scrie un jurnal in backup.log din -BackupDir.

.EXAMPLE
  powershell -File scripts\backup-db.ps1
  powershell -File scripts\backup-db.ps1 -CopyTo "E:\Backup\timesheet"
#>
param(
    [string]$BackupDir = (Join-Path $env:USERPROFILE 'Backups\timesheet'),
    [string]$Database  = 'timesheetdb',
    [string]$DbUser    = 'postgres',
    [string]$DbHost    = 'localhost',
    [int]$Port         = 5432,
    [int]$KeepDays     = 30,
    [string]$CopyTo    = '',
    [string]$PgBin     = ''
)

$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null
$log = Join-Path $BackupDir 'backup.log'

function Write-Log([string]$message) {
    $line = '{0:yyyy-MM-dd HH:mm:ss}  {1}' -f (Get-Date), $message
    Add-Content -Path $log -Value $line -Encoding UTF8
    Write-Host $line
}

function Find-PgBin {
    if ($PgBin) { return $PgBin }
    $cmd = Get-Command pg_dump.exe -ErrorAction SilentlyContinue
    if ($cmd) { return Split-Path $cmd.Source }
    $found = Get-ChildItem 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Name -replace '\D', '') } -Descending |
        ForEach-Object { Join-Path $_.FullName 'bin' } |
        Where-Object { Test-Path (Join-Path $_ 'pg_dump.exe') } |
        Select-Object -First 1
    if ($found) { return $found }
    throw 'Nu am gasit pg_dump.exe. Da calea folderului bin din PostgreSQL prin -PgBin.'
}

try {
    if (-not $env:DB_PASSWORD) {
        $env:DB_PASSWORD = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User')
    }
    if (-not $env:DB_PASSWORD) { throw 'DB_PASSWORD nu este setat.' }

    $bin = Find-PgBin
    $stamp = Get-Date -Format 'yyyy-MM-dd_HH-mm-ss'
    $file = Join-Path $BackupDir "$Database-$stamp.dump"

    $env:PGPASSWORD = $env:DB_PASSWORD
    try {
        & (Join-Path $bin 'pg_dump.exe') -h $DbHost -p $Port -U $DbUser -Fc --no-password -f $file $Database
        if ($LASTEXITCODE -ne 0) { throw "pg_dump a esuat (cod $LASTEXITCODE)." }

        & (Join-Path $bin 'pg_restore.exe') --list $file | Out-Null
        if ($LASTEXITCODE -ne 0) { throw 'Fisierul de backup nu poate fi citit de pg_restore.' }
    } catch {
        if (Test-Path $file) { Remove-Item $file -Force }
        throw
    } finally {
        Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
    }

    $size = [math]::Round((Get-Item $file).Length / 1KB)
    Write-Log "OK  $file ($size KB)"

    if ($CopyTo) {
        New-Item -ItemType Directory -Force -Path $CopyTo | Out-Null
        Copy-Item $file $CopyTo -Force
        Write-Log "Copiat si in $CopyTo"
    }

    $cutoff = (Get-Date).AddDays(-$KeepDays)
    $old = Get-ChildItem $BackupDir -Filter "$Database-*.dump" |
        Sort-Object LastWriteTime -Descending |
        Select-Object -Skip 7 |
        Where-Object { $_.LastWriteTime -lt $cutoff }
    foreach ($f in $old) {
        Remove-Item $f.FullName -Force
        Write-Log "Sters backup vechi: $($f.Name)"
    }
} catch {
    Write-Log "EROARE  $($_.Exception.Message)"
    exit 1
}
