@echo off
setlocal
title Pontaj - Pasul 5: test backup

fltmc >nul 2>&1
if errorlevel 1 goto :elevate

cd /d "%~dp0"
if not exist "%~dp0scripts\test-backup.ps1" goto :missing

echo.
echo  PASUL 5 - Fac un backup acum, ca test - dureaza cam un minut
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\test-backup.ps1"
set "RC=%ERRORLEVEL%"
goto :done

:elevate
if "%~1"=="--elevat" goto :noadmin
echo  Cer drepturi de administrator - apasa Yes in fereastra care apare.
powershell -NoProfile -Command "Start-Process -FilePath '%~f0' -ArgumentList '--elevat' -Verb RunAs"
exit /b

:noadmin
echo.
echo  Nu am primit drepturi de administrator. Click dreapta pe fisier - Run as administrator.
set "RC=1"
goto :done

:missing
echo.
echo  Nu gasesc folderul scripts langa acest fisier.
echo  Dezarhiveaza TOT Timesheet-server.zip si porneste fisierul din folderul dezarhivat.
set "RC=1"

:done
echo.
if "%RC%"=="0" echo  Pasul s-a terminat. Urmatorul: 6-scurtatura-pentru-laptopuri.cmd
if not "%RC%"=="0" echo  *** Pasul NU s-a terminat cu bine. Citeste mesajul de mai sus. ***
echo.
pause
exit /b %RC%
