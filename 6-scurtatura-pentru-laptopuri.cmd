@echo off
setlocal
title Pontaj - Pasul 6: scurtatura pentru laptopuri
cd /d "%~dp0"
if not exist "%~dp0scripts\create-shortcut.ps1" goto :missing

echo.
echo  PASUL 6 - Scurtatura Pontaj.url pentru laptopurile angajatilor
echo.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\create-shortcut.ps1"
set "RC=%ERRORLEVEL%"
goto :done

:missing
echo.
echo  Nu gasesc folderul scripts langa acest fisier.
echo  Dezarhiveaza TOT Timesheet-server.zip si porneste fisierul din folderul dezarhivat.
set "RC=1"

:done
echo.
if "%RC%"=="0" echo  Gata. Pontaj.url e pe Desktop - copiaz-o pe laptopuri. Dupa prima logare ca admin: 7-dupa-prima-logare-sterge-parola-admin.cmd
if not "%RC%"=="0" echo  *** Pasul NU s-a terminat cu bine. Citeste mesajul rosu de mai sus. ***
echo.
pause
exit /b %RC%
