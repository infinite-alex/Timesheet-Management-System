package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class AccountTest extends IntegrationTestBase {

    private static final String URL = "/api/account/password";

    private ResultActions change(RequestPostProcessor auth, String current, String next, String confirm) throws Exception {
        return postJson(URL, "{\"currentPassword\":%s,\"newPassword\":%s,\"confirmPassword\":%s}"
            .formatted(quote(current), quote(next), quote(confirm)), auth);
    }

    private void assertCanLogIn(String username, String password) throws Exception {
        mvc.perform(formLogin().user(username).password(password)).andExpect(authenticated().withUsername(username));
    }

    private void assertCannotLogIn(String username, String password) throws Exception {
        mvc.perform(formLogin().user(username).password(password)).andExpect(unauthenticated());
    }

    @Test
    void changingThePassword_worksAndTheNewOneLogsIn() throws Exception {
        change(asWorker(), PASSWORD, "ParolaNoua-2026", "ParolaNoua-2026").andExpect(status().isNoContent());

        assertCanLogIn("worker", "ParolaNoua-2026");
        assertCannotLogIn("worker", PASSWORD);
        assertThat(employees.findByUsername("worker").orElseThrow().getPassword()).startsWith("$2").doesNotContain("ParolaNoua");
    }

    @Test
    void adminsCanChangeTheirOwnPasswordToo() throws Exception {
        change(asAdmin(), PASSWORD, "AdminNou-2026-ok", "AdminNou-2026-ok").andExpect(status().isNoContent());

        assertCanLogIn("boss", "AdminNou-2026-ok");
        assertCannotLogIn("boss", PASSWORD);
    }

    @Test
    void changingOnePassword_doesNotTouchOtherAccounts() throws Exception {
        change(asWorker(), PASSWORD, "ParolaNoua-2026", "ParolaNoua-2026").andExpect(status().isNoContent());

        assertCanLogIn("other", PASSWORD);
        assertCanLogIn("boss", PASSWORD);
    }

    @Test
    void wrongCurrentPassword_isRejected_andNothingChanges() throws Exception {
        change(asWorker(), "gresita-total", "ParolaNoua-2026", "ParolaNoua-2026")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Parola curentă nu este corectă."));

        assertCanLogIn("worker", PASSWORD);
        assertCannotLogIn("worker", "ParolaNoua-2026");
    }

    @Test
    void newPasswordProblems_areReportedClearly() throws Exception {
        change(asWorker(), PASSWORD, "scurta", "scurta")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(containsString("cel puțin 8")));
        change(asWorker(), PASSWORD, "a".repeat(73), "a".repeat(73))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(containsString("prea lungă")));
        change(asWorker(), PASSWORD, "ParolaNoua-2026", "Altceva-2026-x")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value("Parolele noi nu coincid."));
        change(asWorker(), PASSWORD, PASSWORD, PASSWORD)
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error").value(containsString("diferită")));

        assertCanLogIn("worker", PASSWORD);
    }

    @Test
    void missingFields_areRejectedByField() throws Exception {
        postJson(URL, "{}", asWorker()).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.currentPassword").exists())
            .andExpect(jsonPath("$.fields.newPassword").exists())
            .andExpect(jsonPath("$.fields.confirmPassword").exists());
        change(asWorker(), "", "", "").andExpect(status().isBadRequest());
        change(asWorker(), PASSWORD, "        ", "        ").andExpect(status().isBadRequest());
        assertCanLogIn("worker", PASSWORD);
    }

    @Test
    void anonymousUsers_cannotChangeAPassword() throws Exception {
        postJson(URL, "{\"currentPassword\":\"x\",\"newPassword\":\"yyyyyyyy\",\"confirmPassword\":\"yyyyyyyy\"}",
            org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous())
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void errorsNeverEchoThePasswords() throws Exception {
        change(asWorker(), "gresita-total", "SecretNou-98765", "SecretNou-98765")
            .andExpect(content().string(not(containsString("gresita-total"))))
            .andExpect(content().string(not(containsString("SecretNou-98765"))));
    }

    @Test
    void accountPage_showsTheProfile_forEveryRole() throws Exception {
        String worker = mvc.perform(get("/cont").with(asWorker())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String admin = mvc.perform(get("/cont").with(asAdmin())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertThat(worker).contains("Nume worker").contains(">worker<").contains("Angajat").contains("Schimbă parola");
        assertThat(admin).contains("Nume boss").contains("Administrator");
        assertThat(worker).doesNotContain("$2a$").doesNotContain("$2b$");
    }

    @Test
    void accountPage_needsALogin() throws Exception {
        mvc.perform(get("/cont")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void headerChip_linksToTheAccountPage() throws Exception {
        for (RequestPostProcessor user : new RequestPostProcessor[] {asWorker(), asAdmin()}) {
            String page = mvc.perform(get(user == null ? "/cont" : "/cont").with(user)).andReturn().getResponse().getContentAsString();
            assertThat(page).contains("href=\"/cont\"");
        }
        assertThat(mvc.perform(get("/pontaj").with(asWorker())).andReturn().getResponse().getContentAsString()).contains("href=\"/cont\"");
    }
}
