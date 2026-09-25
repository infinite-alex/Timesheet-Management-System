@echo off
setlocal
title Pontaj - Pasul 7: sterge ADMIN_PASSWORD din Windows
cd /d "%~dp0"
if not exist "%~dp0scripts\set-admin-password.ps1" goto :missing

echo.
echo  PASUL 7 - Doar DUPA ce ai intrat o data in aplicatie ca admin.
echo  Sterge parola de admin din variabilele Windows; contul admin din aplicatie ramane neschimbat.
echo.
choice /C DN /M "Ai intrat deja in aplicatie ca admin"
if errorlevel 2 goto :later

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\set-admin-password.ps1" -Remove
set "RC=%ERRORLEVEL%"
goto :done

:later
echo.
echo  Intra intai pe http://localhost:8080 ca admin, apoi porneste din nou acest fisier.
set "RC=0"
goto :done

:missing
echo.
echo  Nu gasesc folderul scripts langa acest fisier.
echo  Dezarhiveaza TOT Timesheet-server.zip si porneste fisierul din folderul dezarhivat.
set "RC=1"

:done
echo.
pause
exit /b %RC%
