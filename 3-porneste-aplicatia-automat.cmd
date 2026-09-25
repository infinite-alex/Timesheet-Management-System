@echo off
setlocal
title Pontaj - Pasul 3: pornire automata si backup

fltmc >nul 2>&1
if errorlevel 1 goto :elevate

cd /d "%~dp0"
if not exist "%~dp0scripts\install-autostart.ps1" goto :missing

echo.
echo  PASUL 3 - Pornirea automata a aplicatiei si backup-ul zilnic la 22:00
echo.
echo  Unde pun COPIA zilnica de backup? Trebuie sa fie pe ALT disc decat C:
echo  Exemplu: D:\Backup\timesheet
echo  Apasa doar Enter daca serverul nu are alt disc - backup-ul ramane doar pe C:
echo.
set "COPYTO="
set /p "COPYTO=Folder pentru copie: "

if not defined COPYTO goto :nocopy
set "COPYTO=%COPYTO:"=%"
if "%COPYTO:~0,2%"=="\\" goto :network
if not exist "%COPYTO:~0,3%" goto :nodrive
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-autostart.ps1" -BackupCopyTo "%COPYTO%"
set "RC=%ERRORLEVEL%"
goto :done

:nocopy
echo.
echo  Fara copie pe alt disc. Poti rula din nou acest pas mai tarziu, cand ai un disc.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\install-autostart.ps1"
set "RC=%ERRORLEVEL%"
goto :done

:network
echo.
echo  Un folder de retea \\... nu merge pentru backup-ul automat.
echo  Alege un disc al serverului, de exemplu D:\Backup\timesheet, si ruleaza din nou.
set "RC=1"
goto :done

:nodrive
echo.
echo  Discul %COPYTO:~0,3% nu exista pe acest server. Verifica litera in File Explorer si ruleaza din nou.
set "RC=1"
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
if "%RC%"=="0" echo  Pasul s-a terminat. Urmatorul: 4-deschide-firewall.cmd
if not "%RC%"=="0" echo  *** Pasul NU s-a terminat cu bine. Citeste mesajul rosu de mai sus. ***
echo.
pause
exit /b %RC%
