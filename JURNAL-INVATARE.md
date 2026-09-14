# Jurnal de învățare — Timesheet Management System

Scop: nu doar ce am scris, ci **de ce** — raționamentul rezistă mai mult timp în memorie decât sintaxa exactă.

---

## 2026-09-12 — REST API layer (DTO + Service + Controller) pentru Client, Employee, TimesheetEntry

**Ce am făcut:**
Am construit stratul de API REST peste entitățile JPA existente (`Client`, `Employee`, `TimesheetEntry`): pentru fiecare, un DTO (record), metode `toDto`/`toEntity` în service, `findAll()`/`save()` care lucrează cu DTO-uri, și un controller cu `GET`/`POST`.

**De ce am făcut așa:**
- **DTO în loc de entitate direct în controller** — dacă expui entitatea JPA direct, orice modificare a structurii bazei de date schimbă automat și API-ul public; plus, la `TimesheetEntry`, relațiile `@ManyToOne` (`employee`, `client`) sunt `LAZY` și ar da probleme de serializare (posibil `LazyInitializationException` sau bucle infinite JSON) dacă le serializezi direct.
- **`employeeId`/`clientId` (Long) în DTO, nu obiecte `Employee`/`Client`** — un client HTTP nu poate trimite un obiect Java complet în JSON, doar valori simple (numere, string-uri). Id-ul e suficient ca să identifici relația.
- **`Optional`/`findById(...).orElseThrow(...)` în `toEntity`** — căutarea în baza de date poate eșua (id inexistent); `Optional` obligă să tratezi explicit acest caz, în loc să rămâi cu `null` neașteptat mai târziu.
- **Verificare `null` pentru `client` în `TimesheetEntry`** — spre deosebire de `employee` (obligatoriu, `optional = false`), `client` e opțional în entitate, pentru acțiuni interne/non-facturabile (pauză, ședințe interne etc.).

**Concepte noi învățate:**
- `record` Java (constructor + accesori automat generați, fără `getX()`, ci `x()`)
- `Optional<T>` și `.orElseThrow(...)`
- Operator ternar (`condiție ? a : b`)
- Separarea responsabilităților: controller (HTTP) vs. service (business logic + conversii) vs. repository (acces date)

**Unde m-am blocat / ce am înțeles greșit prima dată:**
- Am confundat `toEntity` (DTO → entitate) cu `toDto` (entitate → DTO) — direcția conversiei.
- Am folosit `dto.getName()` în loc de `dto.name()` — record-urile nu au getteri cu prefix `get`.
- Am uitat să declar câmpurile `private final` corespunzătoare parametrilor din constructor (`EmployeeRepository`, `ClientRepository` în `TimesheetEntryService`).
- Am scris `TimesheetEntryService.save(dto)` (numele clasei) în loc de `timesheetEntryService.save(dto)` (numele variabilei injectate) — confuzie clasă vs. instanță.

---

## 2026-09-14 — Sesiune cu Claude Code: audit stadiu real, UI `/pontaj`, auto-reload, curățare issues

**Ce am făcut (cu asistență Claude Code — nu doar eu):**
- Am cerut o comparație între `PROJECT-STATUS.md`/issues GitHub și codul real din `src/`. A ieșit că repository/service/DTO/controller/security erau deja implementate (commit-uri `service + repository`, `CRUD API layer done`, `beginning of security config`), dar issue-urile #10–#13 rămăseseră deschise pe board — board-ul nu reflecta realitatea.
- Am pornit aplicația local (`./mvnw.cmd spring-boot:run`), am descoperit că logarea ca `admin` dădea 404 (`HomeController` redirecționează adminii la `/admin`, rută care nu există încă).
- Am creat un cont nou de test (`alexandra` / `ANGAJAT`) prin `POST /api/employees`, autentificat cu sesiunea admin.
- Am reprodus o eroare 500 la adăugare de pontaj cu un `clientId` inexistent (4) — cauza: `TimesheetEntryService.toEntity` aruncă `RuntimeException` necapturată (nu există `@ControllerAdvice`), exact ce lipsea și în lista de task-uri (#15).
- Am redenumit pagina `/pontajele-mele` → `/pontaj`, cu 2 taburi (butoane): „Vezi pontaje" și „Adaugă pontaj", în loc de tot pe o singură pagină lungă.
- Am adăugat `spring-boot-devtools` în `pom.xml` + `java.autobuild.enabled: true` în `.vscode/settings.json`, ca să nu mai fie nevoie de restart manual al serverului la fiecare schimbare (funcționează cu extensiile Java/Maven din VS Code, fără IntelliJ).
- Am stilizat `login.html` și `pontaj.html` (temă roșu/negru: header negru, taburi active roșii, card alb).
- Am închis issue-urile **#10, #11, #12, #13** pe GitHub (deja implementate — board neactualizat) și **#14** (import Excel istoric — decizie: nu se mai face).

**De ce am făcut așa:**
- **Board-ul GitHub trebuie să reflecte codul real**, altfel devine o sursă de neîncredere — mai bine închis la timp decât lăsat să acumuleze discrepanțe.
- **Taburi în loc de o singură pagină lungă** — separă clar acțiunea de "citire" (listă) de cea de "scriere" (formular), mai ales pe măsură ce pagina va crește (rapoarte, filtrare etc.).
- **DevTools + autobuild** — ciclul editare → restart manual → testare era lent; automatizarea lui scurtează bucla de feedback.
- **Renunțare la importul Excel** — decizie conștientă de scop, nu incapacitate; task-ul rămâne documentat ca respins explicit (won't-do), nu doar uitat.

**Concepte noi învățate:**
- CSRF în Spring Security: formularele Thymeleaf randate cu `th:action` primesc automat un `<input type="hidden" name="_csrf">` prin `RequestDataValueProcessor` — un POST care nu trimite acest token (ex. din `curl` fără el) eșuează silențios cu redirect, nu cu eroare explicită.
- `spring-boot-devtools` face restart automat doar când apar `.class`-uri noi în `target/classes` — recompilarea trebuie declanșată de altundeva (IDE cu auto-build, sau manual).
- O excepție neprinsă (`RuntimeException` fără `@ControllerAdvice`) devine automat 500 generic — fără tratare explicită a erorilor, orice bug de date (id inexistent) arată identic cu un bug real de server.

**Unde m-am blocat / ce am înțeles greșit prima dată:**
- Am presupus că `HomeController` are deja o pagină `/admin` funcțională — de fapt doar redirecționează acolo, pagina nu există (404).
- Am încercat să adaug un pontaj cu un `Client ID` ales la întâmplare (4), fără să știu că tabela `client` era complet goală — de aici eroarea 500.

---
