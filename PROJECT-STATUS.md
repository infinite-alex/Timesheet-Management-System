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
  - `TimesheetEntry` — câmpuri `date`, `employee`, `client`, `workingmonth`, `totalMinutes`, `actions`, `extranote` — **fără getteri/setteri/constructor** (clasă neterminată).
  - `ActionCatalog` — nu e entitate; e o listă hardcodată de tipuri de acțiuni (nume) cu metodă `addActions`.
- Nu există încă: repository-uri, service layer, DTO-uri, controllere REST, configurare Spring Security, validare, logică de import din Excel, teste (doar testul default generat de Spring Initializr, `DemoApplicationTests`).

## Probleme de rezolvat cu prioritate
1. **`application.properties` conține parola bazei de date în clar și e commisă în git** (`src/main/resources/application.properties`) — trebuie mutată în variabilă de mediu sau fișier ignorat de git, și schimbată parola.
2. Modelele nu sunt încă entități JPA reale — lipsesc adnotările și relațiile (FK) dintre `TimesheetEntry` ↔ `Employee`/`Client`.
3. `TimesheetEntry` e incomplet (fără getteri/setteri/constructor) — probabil nu compilează dacă e folosit în altă parte.

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
