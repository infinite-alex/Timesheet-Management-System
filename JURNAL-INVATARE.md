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

## [Dată] — [Nume etapă următoare]

**Ce am făcut:**

**De ce am făcut așa:**

**Concepte noi învățate:**

**Unde m-am blocat / ce am înțeles greșit prima dată:**

---
