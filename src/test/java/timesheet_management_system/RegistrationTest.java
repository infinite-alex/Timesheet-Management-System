package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;

class RegistrationTest extends IntegrationTestBase {

    private ResultActions register(String name, String username, String password, String confirm) throws Exception {
        return mvc.perform(post("/register").with(csrf())
            .param("name", name).param("username", username)
            .param("password", password).param("confirmPassword", confirm));
    }

    private void assertRejected(ResultActions result, String username, String messagePart) throws Exception {
        result.andExpect(status().isOk()).andExpect(content().string(containsString(messagePart)));
        assertThat(employees.findByUsername(username)).isEmpty();
    }

    @Test
    void registrationPage_isShownToAnonymousUsers() throws Exception {
        mvc.perform(get("/register")).andExpect(status().isOk()).andExpect(content().string(containsString("Creează cont")));
    }

    @Test
    void loginPage_linksToRegistration() throws Exception {
        mvc.perform(get("/login")).andExpect(content().string(containsString("/register")));
    }

    @Test
    void validRegistration_createsAnEmployeeAndRedirectsToLogin() throws Exception {
        register("Maria Ionescu", "maria.i", "parolaBuna1", "parolaBuna1")
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?registered"));

        Employee created = employees.findByUsername("maria.i").orElseThrow();
        assertThat(created.getName()).isEqualTo("Maria Ionescu");
        assertThat(created.getRole()).isEqualTo(Role.ANGAJAT);
        assertThat(created.getPassword()).isNotEqualTo("parolaBuna1").startsWith("$2");
    }

    @Test
    void registeredUser_canLogInImmediately() throws Exception {
        register("Maria", "maria", "parolaBuna1", "parolaBuna1");

        mvc.perform(formLogin().user("maria").password("parolaBuna1"))
            .andExpect(authenticated().withUsername("maria").withRoles("ANGAJAT"));
    }

    @Test
    void registration_cannotGrantTheAdminRole() throws Exception {
        mvc.perform(post("/register").with(csrf())
            .param("name", "Hacker").param("username", "hacker")
            .param("password", "parolaBuna1").param("confirmPassword", "parolaBuna1")
            .param("role", "ADMIN").param("ROLE", "ADMIN").param("authorities", "ROLE_ADMIN"))
            .andExpect(redirectedUrl("/login?registered"));

        assertThat(employees.findByUsername("hacker").orElseThrow().getRole()).isEqualTo(Role.ANGAJAT);
        mvc.perform(get("/admin").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
            .user("hacker").roles("ANGAJAT"))).andExpect(status().isForbidden());
    }

    @Test
    void registration_withoutCsrfToken_isForbidden() throws Exception {
        mvc.perform(post("/register").param("name", "A").param("username", "abc")
            .param("password", "parolaBuna1").param("confirmPassword", "parolaBuna1"))
            .andExpect(status().isForbidden());
        assertThat(employees.findByUsername("abc")).isEmpty();
    }

    @Test
    void mismatchedPasswords_areRejected() throws Exception {
        assertRejected(register("Ana", "ana1", "parolaBuna1", "altceva123"), "ana1", "Parolele nu coincid");
    }

    @Test
    void shortPassword_isRejected() throws Exception {
        assertRejected(register("Ana", "ana2", "scurta1", "scurta1"), "ana2", "cel puțin 8");
    }

    @Test
    void passwordAboveBcryptLimit_isRejected() throws Exception {
        String tooLong = "a".repeat(73);
        assertRejected(register("Ana", "ana3", tooLong, tooLong), "ana3", "prea lungă");
    }

    @Test
    void blankName_isRejected() throws Exception {
        assertRejected(register("   ", "ana4", "parolaBuna1", "parolaBuna1"), "ana4", "numele complet");
    }

    @Test
    void veryLongName_isRejected() throws Exception {
        assertRejected(register("A".repeat(101), "ana5", "parolaBuna1", "parolaBuna1"), "ana5", "numele complet");
    }

    @Test
    void invalidUsernames_areRejected() throws Exception {
        for (String bad : new String[] {"ab", "cu spatiu", "a'; DROP TABLE employee;--", "<script>", "x".repeat(31), "diacritice-ă"}) {
            register("Ana", bad, "parolaBuna1", "parolaBuna1")
                .andExpect(status().isOk()).andExpect(content().string(containsString("Username-ul trebuie")));
            assertThat(employees.findByUsername(bad)).isEmpty();
        }
    }

    @Test
    void duplicateUsername_isRejected_evenWithDifferentCase() throws Exception {
        register("Ana", "Popescu", "parolaBuna1", "parolaBuna1").andExpect(redirectedUrl("/login?registered"));

        register("Alta Ana", "popescu", "parolaBuna1", "parolaBuna1")
            .andExpect(status().isOk()).andExpect(content().string(containsString("deja folosit")));
        register("Alta Ana", "POPESCU", "parolaBuna1", "parolaBuna1")
            .andExpect(status().isOk()).andExpect(content().string(containsString("deja folosit")));
        assertThat(employees.findAll().stream().filter(e -> e.getUsername().equalsIgnoreCase("popescu"))).hasSize(1);
    }

    @Test
    void cannotRegisterOverTheSeededAdmin() throws Exception {
        register("Fals Admin", "ADMIN", "parolaBuna1", "parolaBuna1")
            .andExpect(status().isOk()).andExpect(content().string(containsString("deja folosit")));
    }

    @Test
    void failedRegistration_keepsTheTypedNameAndUsername_butNeverThePassword() throws Exception {
        register("Ana Pop", "ana.pop", "scurta", "scurta")
            .andExpect(content().string(containsString("value=\"Ana Pop\"")))
            .andExpect(content().string(containsString("value=\"ana.pop\"")))
            .andExpect(content().string(not(containsString("scurta\""))));
    }

    @Test
    void registrationErrorPage_escapesHtmlInTheNameField() throws Exception {
        register("<script>alert(1)</script>", "x1", "scurta", "scurta")
            .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
            .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    @Test
    void missingFields_areTreatedAsEmptyInsteadOfCrashing() throws Exception {
        mvc.perform(post("/register").with(csrf())).andExpect(status().isOk());
    }

    @Test
    void nameIsTrimmed() throws Exception {
        register("   Ana Trim   ", "ana.trim", "parolaBuna1", "parolaBuna1");

        assertThat(employees.findByUsername("ana.trim").orElseThrow().getName()).isEqualTo("Ana Trim");
    }
}
