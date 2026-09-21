package timesheet_management_system;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AccessControlTest extends IntegrationTestBase {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/pontaj", "/admin", "/api/employees", "/api/clients", "/api/timesheet-entries"})
    void anonymousUser_isSentToLogin(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/employees", "/api/clients", "/api/timesheet-entries"})
    void anonymousUser_cannotWriteThroughTheApi(String url) throws Exception {
        mvc.perform(post(url).contentType("application/json").content("{}"))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/login", "/register", "/css/app.css", "/js/app.js"})
    void publicPages_areReachableWithoutLogin(String url) throws Exception {
        mvc.perform(get(url)).andExpect(status().isOk());
    }

    @Test
    void employee_cannotOpenAdminPage() throws Exception {
        mvc.perform(get("/admin").with(asWorker())).andExpect(status().isForbidden());
    }

    @Test
    void employee_cannotCreateEmployees() throws Exception {
        postJson("/api/employees", "{\"name\":\"X\",\"username\":\"x\",\"password\":\"p\",\"role\":\"ADMIN\"}", asWorker())
            .andExpect(status().isForbidden());
    }

    @Test
    void employee_cannotCreateClients() throws Exception {
        postJson("/api/clients", "{\"name\":\"Firma\"}", asWorker()).andExpect(status().isForbidden());
    }

    @Test
    void employee_canOpenOwnTimesheetPage() throws Exception {
        mvc.perform(get("/pontaj").with(asWorker())).andExpect(status().isOk());
    }

    @Test
    void admin_canOpenAdminPage() throws Exception {
        mvc.perform(get("/admin").with(asAdmin())).andExpect(status().isOk());
    }

    @Test
    void homePage_sendsAdminToAdminAndEmployeeToTimesheet() throws Exception {
        mvc.perform(get("/").with(asAdmin())).andExpect(redirectedUrl("/admin"));
        mvc.perform(get("/").with(asWorker())).andExpect(redirectedUrl("/pontaj"));
    }

    @Test
    void login_withCorrectPassword_succeeds() throws Exception {
        mvc.perform(formLogin().user("worker").password(PASSWORD))
            .andExpect(authenticated().withUsername("worker").withRoles("ANGAJAT"))
            .andExpect(redirectedUrl("/"));
    }

    @Test
    void login_asAdmin_getsAdminRole() throws Exception {
        mvc.perform(formLogin().user("boss").password(PASSWORD))
            .andExpect(authenticated().withRoles("ADMIN"));
    }

    @Test
    void login_withWrongPassword_isRejected() throws Exception {
        mvc.perform(formLogin().user("worker").password("gresit"))
            .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void login_withUnknownUser_looksTheSameAsWrongPassword() throws Exception {
        mvc.perform(formLogin().user("nu-exista").password(PASSWORD))
            .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void login_isCaseSensitiveOnUsername() throws Exception {
        mvc.perform(formLogin().user("WORKER").password(PASSWORD)).andExpect(unauthenticated());
    }

    @Test
    void login_withSqlInjectionAttempt_isRejected() throws Exception {
        for (String attack : List.of("' OR '1'='1", "admin'--", "\" OR 1=1 --")) {
            mvc.perform(formLogin().user(attack).password(attack)).andExpect(unauthenticated());
        }
    }

    @Test
    void login_withoutCsrfToken_isForbidden() throws Exception {
        mvc.perform(post("/login").param("username", "worker").param("password", PASSWORD))
            .andExpect(status().isForbidden());
    }

    @Test
    void logout_needsPostAndRedirectsToLogin() throws Exception {
        mvc.perform(post("/logout").with(asWorker()).with(csrf()))
            .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(post("/logout").with(asWorker())).andExpect(status().isForbidden());
    }

    @Test
    void apiPosts_doNotNeedACsrfToken_butStillNeedRoles() throws Exception {
        postJson("/api/clients", "{\"name\":\"Firma\"}", asAdmin()).andExpect(status().isOk());
    }
}
