<#
.SYNOPSIS
  Pregateste baza de date PostgreSQL pentru aplicatie, dupa ce PostgreSQL a fost instalat.

.DESCRIPTION
  Ruleaza-l o singura data pe server, dupa instalarea PostgreSQL:
      cd C:\Timesheet
      powershell -ExecutionPolicy Bypass -File scripts\setup-database.ps1

  Pasii, fiecare verificat si explicat pe ecran:
    1. gaseste PostgreSQL (folderul bin) si verifica daca serviciul ruleaza;
    2. ia parola bazei de date din DB_PASSWORD sau o cere (nu se vede cand o scrii);
    3. se conecteaza ca postgres - daca parola e gresita, spune asta si se opreste;
    4. creeaza baza timesheetdb, doar daca nu exista (nu sterge si nu modifica nimic existent);
    5. se conecteaza la baza noua, ca test;
    6. optional, salveaza parola in DB_PASSWORD (setx), daca nu e deja salvata.
  Tabelele le creeaza aplicatia singura la prima pornire.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\setup-database.ps1
  powershell -ExecutionPolicy Bypass -File scripts\setup-database.ps1 -Database timesheetdb_proba   # repetitie, fara sa atinga baza reala
#>
param(
    [string]$Database = 'timesheetdb',
    [string]$DbUser   = 'postgres',
    [string]$DbHost   = 'localhost',
    [int]$Port        = 5432,
    [string]$PgBin    = ''
)

$ErrorActionPreference = 'Stop'

function Step([string]$text) { Write-Host ''; Write-Host "==> $text" -ForegroundColor Cyan }
function Ok([string]$text) { Write-Host "    OK  $text" -ForegroundColor Green }
function Fail([string]$text, [string]$hint) {
    Write-Host "    PROBLEMA  $text" -ForegroundColor Red
    if ($hint) { Write-Host "    Ce faci:  $hint" -ForegroundColor Yellow }
    Write-Host ''
    exit 1
}

if ($Database -notmatch '^[A-Za-z0-9_]+$') { Fail 'Numele bazei poate contine doar litere, cifre si underscore.' '' }

Step 'Caut PostgreSQL'
if (-not $PgBin) {
    $cmd = Get-Command psql.exe -ErrorAction SilentlyContinue
    if ($cmd) { $PgBin = Split-Path $cmd.Source }
}
if (-not $PgBin) {
    $PgBin = Get-ChildItem 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Name -replace '\D', '') } -Descending |
        ForEach-Object { Join-Path $_.FullName 'bin' } |
        Where-Object { Test-Path (Join-Path $_ 'psql.exe') } |
        Select-Object -First 1
}
if (-not $PgBin) {
    Fail 'Nu am gasit PostgreSQL in C:\Program Files\PostgreSQL.' 'Instaleaza PostgreSQL din kituri\ (cu "Command Line Tools" bifat), apoi ruleaza din nou scriptul.'
}
$psql = Join-Path $PgBin 'psql.exe'
$createdb = Join-Path $PgBin 'createdb.exe'
Ok "Gasit: $PgBin"

$services = @(Get-Service -Name 'postgresql*' -ErrorAction SilentlyContinue)
if ($services.Count -eq 0) {
    Write-Host '    Atentie: nu vad un serviciu Windows postgresql*. Incerc totusi conexiunea.' -ForegroundColor Yellow
} elseif (-not ($services | Where-Object Status -eq 'Running')) {
    Fail "Serviciul $($services[0].Name) este oprit." "Porneste-l: Win+R -> services.msc -> $($services[0].Name) -> Start. Apoi ruleaza din nou scriptul."
} else {
    Ok "Serviciul ruleaza: $(($services | Where-Object Status -eq 'Running' | Select-Object -First 1).Name)"
}

Step 'Parola bazei de date (utilizatorul postgres)'
$password = $env:DB_PASSWORD
$source = 'variabila DB_PASSWORD'
if (-not $password) {
    foreach ($scope in 'User', 'Machine') {
        $password = [Environment]::GetEnvironmentVariable('DB_PASSWORD', $scope)
        if ($password) { break }
    }
}
if (-not $password) {
    Write-Host '    Scrie parola din PAROLE.txt, sectiunea 1 (nu se vede cand o scrii), apoi Enter:'
    $secure = Read-Host '    Parola' -AsSecureString
    $password = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
    $source = 'scrisa acum'
    if (-not $password) { Fail 'Nu ai scris nicio parola.' 'Ruleaza din nou scriptul si lipeste parola.' }
}
Ok "Folosesc parola ($source)"

$env:PGPASSWORD = $password
$env:PGCONNECT_TIMEOUT = '10'
# mesajele de eroare ale psql le interpretam noi (in PowerShell 5.1 ar opri scriptul cu o exceptie)
$ErrorActionPreference = 'Continue'
try {
    Step "Ma conectez la PostgreSQL ($DbHost`:$Port) ca $DbUser"
    $version = & $psql -h $DbHost -p $Port -U $DbUser -d postgres -w -tAc 'SHOW server_version' 2>&1
    if ($LASTEXITCODE -ne 0) {
        $message = ((($version | ForEach-Object { "$_" }) -join ' ') -replace '\s+', ' ').Trim()
        if ($message -match 'password authentication failed|autentificare') {
            Fail 'Parola nu este cea pusa la instalarea PostgreSQL.' 'Verifica parola din PAROLE.txt (sectiunea 1). Daca DB_PASSWORD e gresit, sterge-l: [Environment]::SetEnvironmentVariable(''DB_PASSWORD'', $null, ''User'') si ruleaza din nou.'
        }
        if ($message -match 'Connection refused|could not connect|timeout') {
            Fail "PostgreSQL nu raspunde pe portul $Port." 'Verifica in services.msc ca serviciul postgresql ruleaza. Daca la instalare ai ales alt port decat 5432, spune-mi.'
        }
        Fail "Conexiunea a esuat: $message" ''
    }
    Ok "Conectat. Versiune PostgreSQL: $(($version | Out-String).Trim())"

    Step "Baza de date $Database"
    $exists = & $psql -h $DbHost -p $Port -U $DbUser -d postgres -w -tAc "SELECT 1 FROM pg_database WHERE datname = '$Database'"
    if (($exists | Out-String).Trim() -eq '1') {
        Ok "Exista deja - nu o modific."
    } else {
        & $createdb -h $DbHost -p $Port -U $DbUser -w $Database
        if ($LASTEXITCODE -ne 0) { Fail "Nu am putut crea baza $Database." 'Citeste mesajul de mai sus; de obicei e o problema de drepturi.' }
        Ok "Creata."
    }

    Step 'Test: ma conectez la baza noua'
    $check = & $psql -h $DbHost -p $Port -U $DbUser -d $Database -w -tAc 'SELECT current_database()'
    if ($LASTEXITCODE -ne 0 -or ($check | Out-String).Trim() -ne $Database) { Fail "Nu ma pot conecta la $Database." '' }
    Ok "Merge: $Database raspunde."
} finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
}

Step 'Variabila DB_PASSWORD (aplicatia si backup-ul o folosesc)'
$saved = [Environment]::GetEnvironmentVariable('DB_PASSWORD', 'User')
if ($saved -eq $password) {
    Ok 'Este deja salvata pe acest cont Windows.'
} else {
    $answer = Read-Host '    O salvez acum pe contul Windows curent? (D/N)'
    if ($answer -match '^[DdYy]') {
        [Environment]::SetEnvironmentVariable('DB_PASSWORD', $password, 'User')
        Ok "Salvata pentru contul $env:USERNAME. Ferestrele PowerShell deschise de acum o vad."
    } else {
        Write-Host '    Nu am salvat-o. Fa pasul B4 din ghid (setx DB_PASSWORD ...) inainte de install-autostart.' -ForegroundColor Yellow
    }
}

Write-Host ''
Write-Host "GATA. Baza de date $Database e pregatita. Urmatorul pas: ADMIN_PASSWORD si install-autostart (ghid, B4-B5)." -ForegroundColor Green
Write-Host ''
