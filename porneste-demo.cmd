@echo off
setlocal
title Pontaj - DEMO (port 8081)
cd /d "%~dp0"
if not exist "%~dp0scripts\run-demo.ps1" goto :missing

echo.
echo  DEMO - o copie separata a aplicatiei, pe http://localhost:8081
echo  Are baza ei de date (timesheetdb_demo); serverul normal nu e atins.
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-demo.ps1"
set "RC=%ERRORLEVEL%"
goto :done

:missing
echo.
echo  Nu gasesc folderul scripts langa acest fisier.
set "RC=1"

:done
echo.
if not "%RC%"=="0" echo  *** Demo-ul NU a pornit sau s-a oprit cu eroare. Citeste mesajul rosu de mai sus. ***
if "%RC%"=="0" echo  Demo-ul s-a oprit.
echo.
pause
exit /b %RC%
