# Status proiect — Timesheet Management System

## Stack
Spring Boot 4.1.0 (Java 21), Spring Data JPA, Spring Security, Spring Validation (dependință prezentă, neconectată încă — vezi mai jos), Spring Web MVC, Thymeleaf, PostgreSQL, Lombok (dependință, dar neutilizat încă în cod), Maven.

## Obiectiv
Aplicație de gestionare a pontajelor pentru un birou de contabilitate. Modelul de date și lista de tipuri de acțiuni au fost derivate din analiza a 32 fișiere Excel de pontaj istoric (~64.400 înregistrări).

## Stadiu actual (13 septembrie 2026)
Proiectul a avansat mult față de update-ul din 24 iulie: entitățile sunt acum JPA reale, iar layerele de repository/service/controller/security există și funcționează.

- **Modele** — entități JPA complete, cu adnotări și relații:
  - `Client`, `Employee` (cu `Role` — enum ADMIN/EMPLOYEE), `TimesheetEntry` (relații `@ManyToOne` către `Employee`/`Client`, `workingmonth` acum `enum WorkingMonth` în loc de `String`).
  - `ActionCatalog` — listă hardcodată de tipuri de acțiuni.
- **Repository layer** (Spring Data JPA) — `ClientRepository`, `EmployeeRepository`, `TimesheetEntryRepository`. Făcut.
- **Service layer** — `ClientService`, `EmployeeService`, `TimesheetEntryService`. `TimesheetEntryService.findAll()` filtrează după rol: ADMIN vede toate înregistrările, EMPLOYEE doar pe ale lui (prin `SecurityContextHolder`).
- **DTO-uri + controllere REST** — `ClientDto`, `EmployeeDto`, `EmployeeCreateDto`, `TimesheetEntryDto` + `ClientController`, `EmployeeController`, `TimesheetEntryController` (`/api/...`, CRUD de bază: GET/POST).
- **Securitate** — `SecurityConfig`, `EmployeeUserDetailsService`, `AdminSeeder` (creează un admin la pornire), `LoginController` + `login.html`. Autentificare pe bază de sesiune, cu rol per `Employee`.
- **Frontend (început 13 septembrie)** — primele pagini Thymeleaf: `login.html`, `pontajele-mele.html` (pagina de pontaje a angajatului), servite prin `HomeController` și `TimesheetEntryPageController`.
- **Parola bazei de date** — nu mai e în clar în `application.properties`; se citește din variabila de mediu `DB_PASSWORD` (issue #17, închis).

## Ce lipsește încă (verificat direct în cod, nu doar din issues)
1. **Import date istorice din Excel** (#14) — nicio dependință Apache POI în `pom.xml`, niciun serviciu/endpoint de import. Nefăcut.
2. **Validare (Bean Validation) + tratare globală a erorilor** (#15) — dependința `spring-boot-starter-validation` e în `pom.xml`, dar nu e folosită: niciun `@Valid` pe controllere, niciun `@ControllerAdvice`/`@ExceptionHandler`. Erorile curente aruncă `RuntimeException` simplu (ex. în `TimesheetEntryService.getCurrentEmployee`/`toEntity`), fără mapare la coduri HTTP sau mesaje curate.
3. **Teste unitare și de integrare** (#16) — doar testul default generat de Spring Initializr, `DemoApplicationTests`. Nimic pentru service/repository/controller.
4. **Rapoarte** — `raport persoana-client` (#5) și `raport persoana - data` (#4) — nu există cod dedicat de agregare/raportare.
5. **`timpul adunat`** (#9, agregare ore lucrate) — nu există logică de sumă/agregare în `TimesheetEntryService` sau altundeva încă.
6. **Encapsulare `TimesheetEntry`** — `getActions()`/`setActions()` fac deja copie defensivă (`new ArrayList<>(...)`), deci nota veche despre asta e rezolvată.

## Discrepanță issues vs. cod (de curățat pe GitHub)
Issue-urile #10 (Repository layer), #11 (Service layer), #12 (Securitate), #13 (DTO-uri + controllere REST) apar încă **deschise** pe GitHub, dar munca corespunzătoare e deja făcută și commisă (`service + repository`, `CRUD API layer done`, `beginning of security config`). Ar trebui închise pentru ca board-ul să reflecte realitatea.

## Ce urmează (ordine sugerată, actualizată)
1. Validare (Bean Validation pe DTO-uri) + `@ControllerAdvice` pentru tratare globală a erorilor — înlocuiește `RuntimeException`-urile simple din servicii.
2. Teste unitare (service layer) și de integrare (repository/controller).
3. Import date istorice din cele 32 fișiere Excel (~64.400 rânduri).
4. Logică de agregare a orelor (`timpul adunat`) și rapoarte per persoană/client/dată.
5. Continuare frontend (CRUD complet din UI, nu doar listare pontaje proprii).
6. Închiderea issue-urilor #10–#13 pe GitHub, ca board-ul să reflecte stadiul real.

## Context pentru planificare
Lucrul la proiect e part-time, în afara unui job de contabilitate. În august disponibilitatea a fost aproape zero — planul de mai sus presupune reluarea ritmului din septembrie.
