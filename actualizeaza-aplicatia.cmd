@echo off
setlocal
title Pontaj - Actualizare aplicatie

fltmc >nul 2>&1
if errorlevel 1 goto :elevate

cd /d "%~dp0"
if not exist "%~dp0scripts\update-server.ps1" goto :missing

echo.
echo  ACTUALIZARE - opreste aplicatia, o reconstruieste din codul din acest folder si o reporneste.
echo  Prima data dureaza cateva minute si are nevoie de internet.
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\update-server.ps1"
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
set "RC=1"

:done
echo.
if "%RC%"=="0" echo  Actualizarea s-a terminat.
if not "%RC%"=="0" echo  *** Actualizarea NU s-a terminat cu bine. Citeste mesajul de mai sus. ***
echo.
pause
exit /b %RC%
