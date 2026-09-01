# Status proiect — Timesheet Management System

## Stack
Spring Boot 4.1.0 (Java 21), Spring Data JPA, Spring Security, Spring Validation, Spring Web MVC, PostgreSQL, Lombok (adăugat ca dependință, dar neutilizat încă în cod), Maven.

## Obiectiv
Aplicație de gestionare a pontajelor pentru un birou de contabilitate. Modelul de date și lista de tipuri de acțiuni au fost derivate din analiza a 32 fișiere Excel de pontaj istoric (~64.400 înregistrări).

## Stadiu actual (24 iulie 2026)
- 4 commit-uri în total, primul pe 24 iunie, ultimul pe 17 iulie. Există modificări necommise în working tree la data asta.
- Există doar clase model **plain Java (POJO)**, fără adnotări JPA (`@Entity`, `@Id`, `@Table` etc.):
  - `Client` — doar câmpul `name`, cu getter/setter.
  - `Employee` — doar câmpul `name`, cu getter/setter.
  - `TimesheetEntry` — **completată azi** (vezi "Progres 24 iulie 2026" mai jos): constructor + getteri/setteri corecți pentru toate cele 7 câmpuri.
  - `ActionCatalog` — nu e entitate; e o listă hardcodată de tipuri de acțiuni (nume) cu metodă `addActions`.
- Nu există încă: repository-uri, service layer, DTO-uri, controllere REST, configurare Spring Security, validare, logică de import din Excel, teste (doar testul default generat de Spring Initializr, `DemoApplicationTests`).

## Progres 24 iulie 2026
- **GitHub Project board** (`github.com/users/infinite-alex/projects/3/views/3`) actualizat: create 8 issues noi în coloana Todo (#10–#17), acoperind restul pașilor din "Ce urmează" de mai jos, plus un issue separat de securitate pentru parola din `application.properties` (problema #1 de mai jos).
- **`TimesheetEntry.java` completată** — clasă corectă acum din punct de vedere Java (nu neapărat gata pentru JPA):
  - Constructor cu toți cei 7 parametri, `this.camp = parametru`, fără `new` greșit.
  - Setteri doar pentru câmpurile decise mutabile: `client`, `workingmonth`, `totalMinutes`, `actions`, `extranote`. **Fără setter pentru `date` și `employee`** — decizie de design: `date`/`employee` se fixează la creare (employee vine din sesiunea de login, nu se editează ulterior), restul pot fi corectate.
  - Getteri pentru toate cele 7 câmpuri, fără parametri.
  - `workingmonth` a rămas `String` deocamdată — plan: transformat în `enum` (fie `java.time.Month` built-in, fie un enum propriu în română), ca să restricționeze valorile la o listă predefinită de luni și să elimine greșelile de tastare. **De făcut de user, nescris încă.**
  - Notă pentru mai târziu (encapsulare): `getActions()` returnează direct referința către lista internă `this.actions` — apelantul poate modifica lista fără să treacă prin `setActions`. De revizitat la partea de service/validare (posibil defensive copy).

## Probleme de rezolvat cu prioritate
1. **`application.properties` conține parola bazei de date în clar și e commisă în git** (`src/main/resources/application.properties`) — trebuie mutată în variabilă de mediu sau fișier ignorat de git, și schimbată parola. Tracked ca issue [#17](https://github.com/infinite-alex/Timesheet-Management-System/issues/17) pe board.
2. Modelele nu sunt încă entități JPA reale — lipsesc adnotările și relațiile (FK) dintre `TimesheetEntry` ↔ `Employee`/`Client`.
3. ~~`TimesheetEntry` e incomplet (fără getteri/setteri/constructor)~~ — **rezolvat 24 iulie 2026**, vezi "Progres 24 iulie 2026" mai sus. Rămâne doar `workingmonth` de trecut pe `enum` în loc de `String`.

## Ce urmează (ordine sugerată)
1. Transformă `Client`, `Employee`, `TimesheetEntry`, `ActionCatalog` (sau echivalent) în entități JPA reale, cu id-uri și relații corecte.
2. Repository layer (Spring Data JPA).
3. Service layer + logică de business (validări, calcule ore/minute etc.).
4. DTO-uri + controllere REST (CRUD pentru Client, Employee, TimesheetEntry).
5. Securitate (autentificare/autorizare — de decis: basic auth, JWT etc.).
6. Import date istorice din cele 32 fișiere Excel (~64.400 rânduri) — script sau endpoint dedicat de import/migrare.
7. Validare (Bean Validation) + tratare globală a erorilor.
8. Teste unitare și de integrare.

## Context pentru planificare
Lucrul la proiect e part-time, în afara unui job de contabilitate. În august disponibilitatea va fi aproape zero — orice plan de task-uri/sprint ar trebui să țină cont de pauza asta.
