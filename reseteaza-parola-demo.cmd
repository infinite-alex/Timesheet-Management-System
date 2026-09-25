@echo off
setlocal
title Pontaj - DEMO: parola noua pentru admin
cd /d "%~dp0"
if not exist "%~dp0scripts\run-demo.ps1" goto :missing

echo.
echo  DEMO - parola noua pentru contul admin din demo (http://localhost:8081)
echo  Parola de pe serverul normal NU se schimba.
echo  Daca demo-ul e deja pornit, inchide intai fereastra lui.
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\run-demo.ps1" -ResetAdminPassword
set "RC=%ERRORLEVEL%"
goto :done

:missing
echo.
echo  Nu gasesc folderul scripts langa acest fisier.
set "RC=1"

:done
echo.
if not "%RC%"=="0" echo  *** Demo-ul NU a pornit sau s-a oprit cu eroare. Citeste mesajul rosu de mai sus. ***
if "%RC%"=="0" echo  Demo-ul s-a oprit. Data viitoare porneste-l cu porneste-demo.cmd - parola noua ramane.
echo.
pause
exit /b %RC%
