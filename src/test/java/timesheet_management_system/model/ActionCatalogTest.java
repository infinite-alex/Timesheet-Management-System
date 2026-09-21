package timesheet_management_system.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ActionCatalogTest {

    private final ActionCatalog catalog = new ActionCatalog();

    @Test
    void knownTask_isFound_withItsCategory() {
        assertThat(catalog.contains("Inchidere luna")).isTrue();
        assertThat(catalog.categoryOf("Inchidere luna")).contains("Verificare si inchidere luna");
        assertThat(catalog.categoryOf("Diverse birou")).contains("Administrativ / non-facturabil");
    }

    @Test
    void unknownOrEmptyTask_isNotFound() {
        assertThat(catalog.contains("Sarcina inventata")).isFalse();
        assertThat(catalog.contains("")).isFalse();
        assertThat(catalog.contains(null)).isFalse();
        assertThat(catalog.contains("inchidere luna")).isFalse();
    }

    @Test
    void everyTaskBelongsToExactlyOneCategory() {
        Set<String> seen = new HashSet<>();
        for (List<String> tasks : catalog.getGroups().values()) {
            for (String task : tasks) {
                assertThat(seen.add(task)).as("sarcina duplicata: " + task).isTrue();
            }
        }
        assertThat(seen).hasSize(catalog.getAllActions().size());
    }

    @Test
    void noCategoryIsEmpty_andNoTaskIsBlank() {
        catalog.getGroups().forEach((category, tasks) -> {
            assertThat(tasks).as(category).isNotEmpty();
            assertThat(tasks).allMatch(t -> !t.isBlank());
        });
    }

    @Test
    void getGroups_returnsACopy() {
        catalog.getGroups().get("Raportare").add("Intrusa");
        catalog.getGroups().clear();

        assertThat(catalog.contains("Intrusa")).isFalse();
        assertThat(catalog.getGroups()).isNotEmpty();
    }
}
