@echo off
setlocal
title Pontaj - Pasul 1: baza de date
cd /d "%~dp0"
if not exist "%~dp0scripts\setup-database.ps1" goto :missing

echo.
echo  PASUL 1 - Pregatirea bazei de date PostgreSQL
echo  PostgreSQL trebuie sa fie deja instalat. Ai nevoie de parola din PAROLE.txt, sectiunea 1.
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\setup-database.ps1"
set "RC=%ERRORLEVEL%"
goto :done

:missing
echo.
echo  Nu gasesc folderul scripts langa acest fisier.
echo  Dezarhiveaza TOT Timesheet-server.zip si porneste fisierul din folderul dezarhivat.
set "RC=1"

:done
echo.
if "%RC%"=="0" echo  Pasul s-a terminat. Urmatorul: 2-seteaza-parola-admin.cmd
if not "%RC%"=="0" echo  *** Pasul NU s-a terminat cu bine. Citeste mesajul rosu de mai sus. ***
echo.
pause
exit /b %RC%
