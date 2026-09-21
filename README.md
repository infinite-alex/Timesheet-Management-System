# Timesheet-Management-System

## Rulare locala

Parolele NU sunt in cod si nici in `application.properties`; aplicatia le citeste
din variabile de mediu.

Inainte de a rula proiectul:

1. Seteaza `DB_PASSWORD` cu parola reala a bazei de date PostgreSQL
   (in Windows: `setx DB_PASSWORD "parola-ta"` intr-un terminal nou, apoi redeschide
   terminalul / editorul).
2. Seteaza `ADMIN_PASSWORD` cu parola contului `admin` (minimum 12 caractere,
   de preferat lunga si aleatorie): `setx ADMIN_PASSWORD "parola-admin"`.
   - La pornire, contul `admin` este creat sau i se actualizeaza parola din aceasta variabila.
   - Daca variabila lipseste si contul nu exista, aplicatia genereaza o parola aleatorie
     si o afiseaza o singura data in consola.
   - Daca variabila lipseste si contul exista, parola lui nu este modificata.

Daca ai folosit vreodata o parola de baza de date care a aparut in istoricul git,
schimb-o in PostgreSQL: `ALTER USER postgres WITH PASSWORD 'parola-noua';`.
