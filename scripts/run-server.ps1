<#
.SYNOPSIS
  Porneste aplicatia Timesheet si o reporneste singura daca se opreste.

.DESCRIPTION
  Ruleaza fisierul target\demo-*.jar (il construieste daca lipseste). Asteapta PostgreSQL la pornirea
  calculatorului. Citeste DB_PASSWORD si ADMIN_PASSWORD din variabilele de mediu (proces, utilizator, masina).
  Jurnalele sunt in folderul logs\ din proiect (timesheet.log se roteste singur, console.log e rescris la fiecare pornire).
  Se foloseste atat manual (powershell -File scripts\run-server.ps1), cat si din sarcina programata "Timesheet-Server".
#>
param(
    [int]$Port = 8080,
    [int]$WaitForDatabaseSeconds = 180
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$logs = Join-Path $root 'logs'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$runLog = Join-Path $logs 'run-server.log'

function Write-Log([string]$message) {
    Add-Content -Path $runLog -Value ('{0:yyyy-MM-dd HH:mm:ss}  {1}' -f (Get-Date), $message) -Encoding UTF8
}

foreach ($name in 'DB_PASSWORD', 'ADMIN_PASSWORD') {
    if (-not [Environment]::GetEnvironmentVariable($name, 'Process')) {
        foreach ($scope in 'User', 'Machine') {
            $value = [Environment]::GetEnvironmentVariable($name, $scope)
            if ($value) { [Environment]::SetEnvironmentVariable($name, $value, 'Process'); break }
        }
    }
}
if (-not $env:DB_PASSWORD) {
    Write-Log 'EROARE  DB_PASSWORD nu este setat (nici la utilizator, nici la nivel de masina).'
    throw 'DB_PASSWORD nu este setat.'
}

function Test-Port([int]$port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try { $client.Connect('127.0.0.1', $port); return $true } catch { return $false } finally { $client.Close() }
}

function Find-Jar {
    Get-ChildItem (Join-Path $root 'target') -Filter 'demo-*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notmatch 'original' } |
        Sort-Object LastWriteTime -Descending | Select-Object -First 1
}

$java = 'java'
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
}

Write-Log "Pornesc (port $Port)."
$deadline = (Get-Date).AddSeconds($WaitForDatabaseSeconds)
while (-not (Test-Port 5432)) {
    if ((Get-Date) -gt $deadline) { Write-Log 'PostgreSQL nu raspunde pe portul 5432; continui oricum.'; break }
    Start-Sleep -Seconds 3
}

$jar = Find-Jar
if (-not $jar) {
    Write-Log 'Nu exista jar-ul; construiesc cu mvnw package.'
    Push-Location $root
    try { & (Join-Path $root 'mvnw.cmd') -q package -DskipTests } finally { Pop-Location }
    $jar = Find-Jar
    if (-not $jar) { Write-Log 'EROARE  Build-ul nu a produs un jar.'; throw 'Build esuat.' }
}

while ($true) {
    $jar = Find-Jar
    Write-Log "Rulez $($jar.Name)"
    $process = Start-Process -FilePath $java -WorkingDirectory $root -NoNewWindow -Wait -PassThru `
        -ArgumentList @('-jar', "`"$($jar.FullName)`"", "--server.port=$Port", "--logging.file.name=`"$(Join-Path $logs 'timesheet.log')`"") `
        -RedirectStandardOutput (Join-Path $logs 'console.log') `
        -RedirectStandardError (Join-Path $logs 'console-error.log')
    Write-Log "Aplicatia s-a oprit (cod $($process.ExitCode)). Repornesc peste 30 de secunde."
    Start-Sleep -Seconds 30
}
