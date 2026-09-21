# Status proiect — Timesheet Management System

## Stack
Spring Boot 4.1.0 (Java 21), Spring Data JPA, Spring Security, Bean Validation, Spring Web MVC, Thymeleaf, PostgreSQL, Maven. Lombok e în `pom.xml`, dar nu e folosit în cod. Teste pe H2 în memorie.

## Obiectiv
Aplicație de gestionare a pontajelor pentru un birou de contabilitate, folosită zilnic de angajați și administrată de un admin. Lista tipurilor de sarcini a fost derivată din analiza a 32 fișiere Excel de pontaj istoric (~64.400 înregistrări). Importul datelor istorice a fost respins explicit (decizie din 14 septembrie).

## Stadiu actual (21 septembrie 2026)
Funcțiile de bază sunt gata și acoperite de teste (287 de teste automate, toate trec). Aplicația e pregătită pentru folosire zilnică: are backup, blocare la parole greșite și pornire automată (scripturile din `scripts/` trebuie instalate o dată, vezi README).

### Funcționalități
- **Conturi** — autentificare pe sesiune, roluri `ADMIN` / `ANGAJAT`. Înregistrare liberă la `/register` (rol angajat). Pagina „Contul meu” (`/cont`) pentru schimbarea parolei. Politică de parolă: 8–72 de caractere. Un cont dezactivat sau cu rol schimbat pierde accesul imediat, chiar cu sesiune deschisă.
- **Pontaje** — angajatul își adaugă, editează și șterge propriile pontaje (`/pontaj`); adminul le gestionează pe toate. Fiecare pontaj are dată, client (opțional), lună de lucru, durată, sarcină din catalog și notă.
- **Administrare** (`/admin`) — statistici, toate pontajele cu căutare, angajați (creare, editare nume/rol/stare/parolă nouă, ștergere doar dacă nu are pontaje, altfel dezactivare) și clienți (creare, **redenumire, ștergere doar dacă nu are pontaje**). Regula de siguranță: rămâne mereu cel puțin un admin activ.
- **Rapoarte** (`/rapoarte`, doar admin) — pe angajați, pe clienți și pe sarcini, cu filtre pe perioadă, client, angajat și sarcină; export CSV protejat împotriva injecției de formule. Acoperă vechile cerințe #4 (persoană–dată), #5 (persoană–client) și #9 (timp adunat).
- **Securitate** — vezi mai jos.

### Securitate și operare (adăugate pe 21 septembrie)
- **Blocare la parole greșite** — 5 parole greșite pentru același username (indiferent de litere mari/mici) blochează logarea 15 minute; și 20 de eșecuri de la aceeași adresă IP. Blocarea se aplică înainte de verificarea parolei și și pentru username-uri inexistente, ca răspunsul să nu arate ce conturi există. Înregistrarea e limitată la 10 pe adresă IP în 15 minute. Pragurile se schimbă din `application.properties` (`app.security.*`). Starea e în memorie: repornirea aplicației o resetează.
- **Backup** — `scripts/backup-db.ps1` face `pg_dump` (format custom), verifică fișierul cu `pg_restore --list`, păstrează 30 de zile și poate copia și pe alt disc (`-CopyTo`). `scripts/restore-db.ps1` restaurează într-o bază nouă, fără să suprascrie baza reală. Testat: backup + restaurare, cu aceleași numere de rânduri.
- **Pornire automată** — `scripts/install-autostart.ps1` creează două sarcini programate Windows: `Timesheet-Server` (pornește aplicația la boot și o repornește dacă cade) și `Timesheet-Backup` (zilnic la 22:00). E sarcină programată, nu serviciu Windows propriu-zis, ca să nu depindem de un program terț.
- **Configurare** — parola bazei de date și a adminului vin din variabile de mediu (`DB_PASSWORD`, `ADMIN_PASSWORD`). `show-sql` e oprit.

## Ce mai lipsește
**De făcut de tine, o singură dată:** rulează `scripts\install-autostart.ps1` dintr-un PowerShell „ca administrator” (nu l-am rulat eu, cere drepturi de administrator). Apoi verifică pe http://localhost:8080 și în `logs\run-server.log`.

**Lipsuri de funcționalitate:**
1. Blocarea unei luni închise (nimeni nu mai modifică pontajele după închiderea lunii).
2. Lista de sarcini e fixă în cod (`ActionCatalog`); adminul nu poate adăuga sarcini.
3. Adminul nu poate adăuga pontaje pe numele altcuiva din interfață (doar prin API).
4. Un angajat care și-a uitat parola depinde de admin.
5. Raport lunar per angajat gata de tipărit sau export Excel (acum doar CSV).

**Mai târziu, dacă crește:** HTTPS, CSRF pe API (acum dezactivat pe `/api/**`), aprobarea conturilor noi de către admin, paginare pe liste.

**Limite cunoscute:**
- Backup-ul implicit e pe același disc cu baza de date; folosește `-CopyTo` spre un disc extern sau alt loc, altfel un disc defect îl pierde odată cu baza.
- Blocarea după parole greșite poate fi folosită și de un atacator ca să blocheze temporar contul cuiva (15 minute). E compromisul obișnuit; adminul se deblochează repornind aplicația.
- Adresa IP e cea văzută de server. Dacă pui aplicația în spatele unui proxy, trebuie configurat `server.forward-headers-strategy`, altfel toți utilizatorii apar cu aceeași adresă.

## Context pentru planificare
Lucrul la proiect e part-time, în afara unui job de contabilitate.
