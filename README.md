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

## Teste

`./mvnw.cmd test` ruleaza toate testele pe o baza H2 in memorie; baza reala nu este atinsa.
