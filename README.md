# Timesheet-Management-System

## Rulare locala

Parola bazei de date PostgreSQL NU mai este in `application.properties` (a fost scoasa
din motive de securitate - vezi issue #17). Aplicatia citeste parola din variabila de
mediu `DB_PASSWORD`.

Inainte de a rula proiectul:

1. Seteaza variabila de mediu `DB_PASSWORD` cu parola reala a bazei de date
   (in Windows: `setx DB_PASSWORD "parola-ta"` intr-un terminal nou, sau in
   Run Configuration din IntelliJ, la Environment variables).
2. Daca parola veche (`alaska38rom`) a fost vreodata folosita, schimb-o si in
   PostgreSQL (`ALTER USER postgres WITH PASSWORD 'parola-noua';`), pentru ca
   a fost expusa in istoricul git.
