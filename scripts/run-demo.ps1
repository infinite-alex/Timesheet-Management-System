<#
.SYNOPSIS
  Porneste o copie DEMO a aplicatiei, pe portul 8081, cu baza ei de date (timesheetdb_demo).

.DESCRIPTION
  Demo-ul nu atinge serverul normal: alt port, alta baza de date. Poti incerca orice in el.
  La prima pornire creeaza baza timesheetdb_demo si iti cere parola contului admin din demo.
  Cu -ResetAdminPassword iti cere o parola noua pentru admin si o pune in locul celei vechi.
  ADMIN_PASSWORD si ADMIN_RESET_PASSWORD sunt folosite doar in aceasta fereastra, nu se salveaza in Windows.
  Aplicatia ruleaza cat timp fereastra e deschisa; o opresti cu Ctrl+C sau inchizand fereastra.

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts\run-demo.ps1
  powershell -ExecutionPolicy Bypass -File scripts\run-demo.ps1 -ResetAdminPassword
#>
param(
    [switch]$ResetAdminPassword,
    [int]$Port          = 8081,
    [string]$Database   = 'timesheetdb_demo',
    [string]$DbUser     = 'postgres',
    [string]$DbHost     = 'localhost',
    [int]$DbPort        = 5432,
    [int]$StartupSeconds = 180
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$minLength = 12

function Step([string]$text) { Write-Host ''; Write-Host "==> $text" -ForegroundColor Cyan }
function Ok([string]$text) { Write-Host "    OK  $text" -ForegroundColor Green }
function Fail([string]$text, [string]$hint) {
    Write-Host "    PROBLEMA  $text" -ForegroundColor Red
    if ($hint) { Write-Host "    Ce faci:  $hint" -ForegroundColor Yellow }
    Write-Host ''
    exit 1
}

function Read-Secret([string]$prompt) {
    $secure = Read-Host $prompt -AsSecureString
    [Runtime.InteropServices.Marshal]::PtrToStringBSTR([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

function Read-NewAdminPassword {
    Write-Host '    Scrie parola noua pentru contul admin din DEMO (nu se vede cand o scrii), apoi Enter.'
    Write-Host "    Minimum $minLength caractere. Nu schimba parola de pe serverul normal."
    $first = Read-Secret '    Parola admin demo'
    if ($first.Length -lt $minLength) { Fail "Parola are $($first.Length) caractere; trebuie cel putin $minLength." 'Porneste din nou fisierul.' }
    $second = Read-Secret '    Inca o data, pentru verificare'
    if ($first -ne $second) { Fail 'Cele doua parole nu coincid.' 'Porneste din nou fisierul.' }
    $first
}

function Test-Port([int]$p) {
    $client = New-Object System.Net.Sockets.TcpClient
    try { $client.Connect('127.0.0.1', $p); return $true } catch { return $false } finally { $client.Close() }
}

function Find-Jar {
    Get-ChildItem (Join-Path $root 'target') -Filter 'demo-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'original' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
}

if ($Database -notmatch '^[A-Za-z0-9_]+$') { Fail 'Numele bazei poate contine doar litere, cifre si underscore.' '' }

Step "Verific ca portul $Port e liber"
if (Test-Port $Port) {
    Fail "Ceva ruleaza deja pe portul $Port - probabil demo-ul e deja pornit." "Deschide http://localhost:$Port sau inchide fereastra demo-ului si porneste din nou."
}
Ok 'Liber.'

Step 'Parola bazei de date (DB_PASSWORD)'
foreach ($scope in 'User', 'Machine') {
    if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = [Environment]::GetEnvironmentVariable('DB_PASSWORD', $scope) }
}
if (-not $env:DB_PASSWORD) { Fail 'DB_PASSWORD nu este setat.' 'Ruleaza intai 1-pregateste-baza-de-date.cmd.' }
Ok 'Gasita.'

# setarile de admin ale serverului normal nu se aplica demo-ului
Remove-Item Env:\ADMIN_PASSWORD, Env:\ADMIN_RESET_PASSWORD -ErrorAction SilentlyContinue

Step "Baza de date $Database"
$pgBin = ''
$cmd = Get-Command psql.exe -ErrorAction SilentlyContinue
if ($cmd) { $pgBin = Split-Path $cmd.Source }
if (-not $pgBin) {
    $pgBin = Get-ChildItem 'C:\Program Files\PostgreSQL' -Directory -ErrorAction SilentlyContinue |
        Sort-Object { [int]($_.Name -replace '\D', '') } -Descending |
        ForEach-Object { Join-Path $_.FullName 'bin' } |
        Where-Object { Test-Path (Join-Path $_ 'psql.exe') } |
        Select-Object -First 1
}
if (-not $pgBin) { Fail 'Nu am gasit PostgreSQL in C:\Program Files\PostgreSQL.' 'Instaleaza PostgreSQL cu "Command Line Tools" bifat.' }

$env:PGPASSWORD = $env:DB_PASSWORD
$env:PGCONNECT_TIMEOUT = '10'
$newDatabase = $false
# mesajele de eroare ale psql le interpretam noi (in PowerShell 5.1 ar opri scriptul cu o exceptie)
$ErrorActionPreference = 'Continue'
try {
    $exists = & (Join-Path $pgBin 'psql.exe') -h $DbHost -p $DbPort -U $DbUser -d postgres -w -tAc "SELECT 1 FROM pg_database WHERE datname = '$Database'" 2>&1
    if ($LASTEXITCODE -ne 0) {
        Fail "Nu ma pot conecta la PostgreSQL: $((($exists | ForEach-Object { "$_" }) -join ' ').Trim())" 'Verifica in services.msc ca serviciul postgresql ruleaza si ca DB_PASSWORD e corect.'
    }
    if (($exists | Out-String).Trim() -eq '1') {
        Ok 'Exista deja.'
    } else {
        & (Join-Path $pgBin 'createdb.exe') -h $DbHost -p $DbPort -U $DbUser -w $Database
        if ($LASTEXITCODE -ne 0) { Fail "Nu am putut crea baza $Database." 'Citeste mesajul de mai sus.' }
        $newDatabase = $true
        Ok 'Creata acum (demo nou, fara date).'
    }
} finally {
    Remove-Item Env:\PGPASSWORD -ErrorAction SilentlyContinue
    $ErrorActionPreference = 'Stop'
}

if ($newDatabase -or $ResetAdminPassword) {
    if ($newDatabase) { Step 'Parola contului admin din demo (demo nou)' } else { Step 'Resetez parola contului admin din demo' }
    $env:ADMIN_PASSWORD = Read-NewAdminPassword
    if ($ResetAdminPassword) { $env:ADMIN_RESET_PASSWORD = 'true' }
    Ok 'Folosita doar pentru pornirea asta; nu se salveaza in Windows.'
}

Step 'Aplicatia'
$jar = Find-Jar
if (-not $jar) {
    Write-Host '    Nu exista jar-ul; il construiesc (dureaza cateva minute prima data).'
    Push-Location $root
    try { & (Join-Path $root 'mvnw.cmd') -q package -DskipTests } finally { Pop-Location }
    $jar = Find-Jar
    if (-not $jar) { Fail 'Build-ul nu a produs un jar.' 'Citeste mesajele de mai sus.' }
}
Ok $jar.Name

$java = 'java'
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
}

Step "Pornesc demo-ul pe http://localhost:$Port (asteapta, dureaza cam 30 de secunde)"
$process = Start-Process -FilePath $java -WorkingDirectory $root -NoNewWindow -PassThru `
    -ArgumentList @('-jar', "`"$($jar.FullName)`"", "--server.port=$Port",
        "--spring.datasource.url=jdbc:postgresql://${DbHost}:$DbPort/$Database")
$null = $process.Handle  # fara asta, Windows PowerShell 5.1 nu mai afla codul de iesire

$deadline = (Get-Date).AddSeconds($StartupSeconds)
while (-not (Test-Port $Port) -and -not $process.HasExited -and (Get-Date) -lt $deadline) { Start-Sleep -Seconds 2 }
if ($process.HasExited) { Fail "Aplicatia s-a oprit (cod $($process.ExitCode))." 'Citeste mesajele de mai sus.' }
if (-not (Test-Port $Port)) {
    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    Fail "Aplicatia nu raspunde pe portul $Port dupa $StartupSeconds secunde." 'Citeste mesajele de mai sus.'
}

Write-Host ''
Write-Host "GATA. Demo-ul merge: http://localhost:$Port" -ForegroundColor Green
if ($env:ADMIN_PASSWORD) {
    Write-Host '      Intra cu utilizatorul admin si parola scrisa acum.' -ForegroundColor Green
}
Write-Host '      Lasa fereastra deschisa cat folosesti demo-ul; Ctrl+C sau inchiderea ferestrei il opreste.' -ForegroundColor Green
Write-Host ''
Remove-Item Env:\ADMIN_PASSWORD, Env:\ADMIN_RESET_PASSWORD -ErrorAction SilentlyContinue
try { Start-Process "http://localhost:$Port" } catch { }
$process.WaitForExit()
