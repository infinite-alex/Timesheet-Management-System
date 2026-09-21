package timesheet_management_system.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class ActionCatalog {

    private final Map<String, List<String>> actionsByCategory = new LinkedHashMap<>();

    public ActionCatalog() {
        addCategory("Declaratii fiscale",
                "Semnare / depunere declaratii + recipise",
                "Decont TVA (D300)",
                "D394 - jurnal cumparare/vanzare",
                "D390 - declaratie recapitulativa",
                "D406 / SAF-T",
                "D100 - impozite",
                "D112 - contributii salarii",
                "D101 - impozit pe profit",
                "Alte declaratii (D205, D700, D212, D107, D150)",
                "Rezolvare erori declaratii",
                "Intrastat",
                "Intocmit F4109",
                "Mutare declaratii in folderul firmei");

        addCategory("Operare documente contabile",
                "Operare / inregistrare documente",
                "Import documente (Saga)",
                "Operare extras de cont",
                "Gestiune facturi + banca (in timp real)",
                "Preluare facturi din SPV (e-Factura)",
                "Preluare tranzactii + asociere cu document",
                "OP-uri / ordine de plata",
                "Operare Z-uri (case de marcat)",
                "Bonuri de consum");

        addCategory("Verificare si inchidere luna",
                "Balanta - generare, verificare, finalizare",
                "Verificare documente / conturi",
                "Verificare jurnale cumparari/vanzari",
                "Nota contabila",
                "Inchidere luna",
                "Calcul impozit",
                "Calcul TVA");

        addCategory("Raportare",
                "P&L - realizare, verificare, trimitere",
                "Bilant + anexe",
                "Initializare bilant",
                "Scadentar / datorii si creante",
                "Cash-flow",
                "Completat tabel/raport informatii clienti",
                "Raport financiar");

        addCategory("Salarizare & HR",
                "Calcul salarii / stat de plata",
                "Nota salarii",
                "Fluturasi",
                "Calcul taxe + transmis catre client",
                "Inregistrare in Reges / Revisal",
                "Concedii (CO / CM)",
                "Acte angajare / incetare / suspendare CIM",
                "Acte aditionale",
                "Tichete de masa / prime");

        addCategory("Clienti si comunicare",
                "Citit corespondenta / raspuns la mail-uri",
                "Facturare clienti",
                "Trimis notificare de plata",
                "Solicitare fisa ANAF / vector fiscal",
                "Sedinta / prezentare societati",
                "Discutii / telefoane",
                "Deplasare la client",
                "Raspuns la somatii");

        addCategory("Administrativ / non-facturabil",
                "Initializare societati / inrolare SPV",
                "Arhivare / listare / asezare documente",
                "Salvare pontaj / raport de activitate",
                "Diverse birou",
                "Pauza de masa",
                "Liber / concediu / recuperare");
    }

    private void addCategory(String category, String... actions) {
        actionsByCategory.put(category, new ArrayList<>(List.of(actions)));
    }

    public List<String> getCategories() {
        return new ArrayList<>(actionsByCategory.keySet());
    }

    public List<String> getActions(String category) {
        return actionsByCategory.getOrDefault(category, new ArrayList<>());
    }

    public Map<String, List<String>> getGroups() {
        Map<String, List<String>> copy = new LinkedHashMap<>();
        actionsByCategory.forEach((category, actions) -> copy.put(category, new ArrayList<>(actions)));
        return copy;
    }

    public boolean contains(String action) {
        return categoryOf(action).isPresent();
    }

    public Optional<String> categoryOf(String action) {
        for (Map.Entry<String, List<String>> entry : actionsByCategory.entrySet()) {
            if (entry.getValue().contains(action)) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    public List<String> getAllActions() {
        List<String> all = new ArrayList<>();
        for (List<String> actions : actionsByCategory.values()) {
            all.addAll(actions);
        }
        return all;
    }

    public void addAction(String category, String action) {
        List<String> actions = actionsByCategory.computeIfAbsent(category, key -> new ArrayList<>());
        if (!actions.contains(action)) {
            actions.add(action);
        }
    }
}
