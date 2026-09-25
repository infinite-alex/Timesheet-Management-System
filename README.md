# Timesheet-Management-System

## Rulare locala

Parolele NU sunt in cod si nici in `application.properties`; aplicatia le citeste
din variabile de mediu.

Inainte de a rula proiectul:

1. Seteaza `DB_PASSWORD` cu parola reala a bazei de date PostgreSQL
   (in Windows: `setx DB_PASSWORD "parola-ta"` intr-un terminal nou, apoi redeschide
   terminalul / editorul).
2. Seteaza `ADMIN_PASSWORD` cu parola initiala a contului `admin` (minimum 12 caractere,
   de preferat lunga si aleatorie): `setx ADMIN_PASSWORD "parola-admin"`.
   - Variabila se foloseste doar cand contul `admin` **nu exista inca** (baza de date noua).
   - Daca variabila lipseste si contul nu exista, aplicatia genereaza o parola aleatorie
     si o afiseaza o singura data in consola.
   - Dupa prima pornire parola se schimba din aplicatie (pagina "Contul meu"); o schimbare
     facuta acolo NU este suprascrisa la repornire.
   - Recuperare (ai uitat parola de admin): porneste o singura data cu
     `ADMIN_RESET_PASSWORD=true` si `ADMIN_PASSWORD` setat; parola adminului devine cea din
     `ADMIN_PASSWORD`. Apoi scoate `ADMIN_RESET_PASSWORD`.

Daca ai folosit vreodata o parola de baza de date care a aparut in istoricul git,
schimb-o in PostgreSQL: `ALTER USER postgres WITH PASSWORD 'parola-noua';`.

## Roluri si conturi

- Oricine se poate inregistra la `/register` (primeste rolul angajat).
- Angajatul isi poate adauga, edita si sterge doar propriile pontaje; adminul le poate gestiona pe toate.
- Adminul gestioneaza angajatii din panou: editare (nume, rol, stare, parola noua), dezactivare si
  stergere (permisa doar daca angajatul nu are pontaje; altfel se dezactiveaza, iar orele raman in rapoarte).
- Un cont dezactivat sau cu rol schimbat pierde accesul imediat, chiar daca are o sesiune deschisa.
- Adminul gestioneaza si clientii: redenumire si stergere (stergerea e permisa doar daca clientul nu are
  pontaje; redenumirea pastreaza pontajele legate de client).

## Protectie la parole gresite

- 5 parole gresite pentru acelasi username (fara diferenta intre litere mari si mici) blocheaza logarea 15 minute,
  chiar daca urmatoarea parola e corecta. Un login reusit reseteaza numaratoarea.
- 20 de logari gresite de la aceeasi adresa IP blocheaza logarea de la acea adresa 15 minute.
- Inregistrarea e limitata la 10 conturi noi pe adresa IP in 15 minute.
- Pragurile se schimba in `application.properties`: `app.security.max-login-failures`,
  `max-login-failures-per-ip`, `max-registrations-per-ip`, `lockout-minutes`.
- Starea e tinuta in memorie: repornirea aplicatiei deblocheaza toate conturile (asa deblochezi un cont
  blocat din greseala). Daca pui aplicatia in spatele unui proxy, seteaza `server.forward-headers-strategy=framework`,
  altfel toti utilizatorii apar cu adresa proxy-ului.

## Pornire automata si backup (Windows)

Scripturile sunt in `scripts\`. Instalarea se face o singura data, dintr-un PowerShell deschis **ca administrator**:

```
cd C:\Users\alexu\Timesheet-Management-System
powershell -ExecutionPolicy Bypass -File scripts\install-autostart.ps1
```

Creeaza doua sarcini programate (rulate ca utilizatorul tau, fara fereastra):

- `Timesheet-Server` - porneste aplicatia la pornirea calculatorului, chiar daca nimeni nu e logat, o reporneste
  daca se opreste si asteapta PostgreSQL. Aplicatia raspunde pe http://localhost:8080. Jurnale: `logs\`.
- `Timesheet-Backup` - `pg_dump` in fiecare zi la 22:00 (sau la prima ocazie, daca calculatorul era oprit), in
  `C:\Users\alexu\Backups\timesheet`, cu pastrare 30 de zile.

Optiuni: `-BackupAt 23:30`, `-BackupDir D:\backup`, `-BackupCopyTo E:\backup` (copie si pe alt disc), `-SkipServer`, `-SkipBackup`.
**Backup-ul de pe acelasi disc nu te apara de un disc defect: seteaza `-BackupCopyTo` spre un disc extern sau alt loc.**
Backup-ul contine numele clientilor si ale angajatilor; alege cu grija un loc in cloud.

`DB_PASSWORD` (si `ADMIN_PASSWORD`) trebuie sa fie setate cu `setx` (utilizator) sau `setx /M` (masina); scripturile le citesc de acolo.
Accesul de la alte calculatoare din birou cere in plus o regula de firewall pentru portul 8080 (nu o creeaza scriptul).

Dupa ce schimbi codul: `powershell -ExecutionPolicy Bypass -File scripts\update-server.ps1` (opreste serverul,
reconstruieste jar-ul si il porneste). Ca sa scoti sarcinile: `scripts\uninstall-autostart.ps1`.
Rulare manuala, fara sarcini: `powershell -File scripts\run-server.ps1`.

Backup manual: `powershell -File scripts\backup-db.ps1`.

## Demo (copie separata, pentru incercari)

- `porneste-demo.cmd` (dublu-clic) porneste o copie a aplicatiei pe http://localhost:8081, cu baza ei de date
  `timesheetdb_demo`. Serverul normal (8080, `timesheetdb`) nu e atins. La prima pornire creeaza baza si cere parola
  contului admin din demo. Demo-ul merge cat timp fereastra e deschisa.
- `reseteaza-parola-demo.cmd` cere o parola noua pentru adminul din demo si porneste demo-ul cu ea. Parola de pe
  serverul normal nu se schimba. Daca demo-ul e deja pornit, inchide intai fereastra lui.
- Parolele scrise acolo se folosesc doar in fereastra respectiva; nu se salveaza in Windows (`ADMIN_PASSWORD` si
  `ADMIN_RESET_PASSWORD` salvate cu `setx` sunt ignorate de demo).

Instalare pe serverul firmei: `scripts\package-for-server.ps1` face un zip (fara datele clientilor) de copiat pe server;
pe server, `scripts\create-shortcut.ps1` creeaza `Pontaj.url` cu adresa serverului, de copiat pe laptopurile angajatilor.

Pe server, fiecare pas are si un fisier `.cmd` in radacina proiectului (dublu-click, fara comenzi tastate; cele care
au nevoie de administrator cer singure drepturile): `1-pregateste-baza-de-date` (`setup-database.ps1`),
`2-seteaza-parola-admin` (`set-admin-password.ps1`), `3-porneste-aplicatia-automat` (`install-autostart.ps1`),
`4-deschide-firewall` (`open-firewall.ps1`), `5-testeaza-backup` (`test-backup.ps1`),
`6-scurtatura-pentru-laptopuri` (`create-shortcut.ps1`), `7-dupa-prima-logare-sterge-parola-admin`
(`set-admin-password.ps1 -Remove`) si `actualizeaza-aplicatia` (`update-server.ps1`).

Restaurare (intr-o baza noua, ca sa verifici datele, nu suprascrie `timesheetdb`):

```
powershell -File scripts\restore-db.ps1 -BackupFile C:\Users\alexu\Backups\timesheet\timesheetdb-2026-09-21_22-00-00.dump
```

Ca sa inlocuiesti baza reala cu cea restaurata: opreste aplicatia, redenumeste `timesheetdb` (de ex. in `timesheetdb_veche`)
si `timesheetdb_restore` in `timesheetdb` (`ALTER DATABASE ... RENAME TO ...` in psql), apoi porneste aplicatia.

## Teste

`./mvnw.cmd test` ruleaza toate testele pe o baza H2 in memorie; baza reala nu este atinsa.
