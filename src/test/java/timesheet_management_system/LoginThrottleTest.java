package timesheet_management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class LoginThrottleTest extends IntegrationTestBase {

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(formLogin().user(username).password(password));
    }

    private ResultActions loginFrom(String ip, String username, String password) throws Exception {
        RequestPostProcessor fromIp = request -> {
            request.setRemoteAddr(ip);
            return request;
        };
        return mvc.perform(post("/login").with(csrf()).with(fromIp)
            .param("username", username).param("password", password));
    }

    private void failFiveTimes(String username) throws Exception {
        for (int i = 0; i < 5; i++) {
            login(username, "gresita-" + i).andExpect(redirectedUrl("/login?error"));
        }
    }

    @Test
    void afterFiveWrongPasswords_evenTheCorrectPasswordIsRefused() throws Exception {
        failFiveTimes("worker");

        login("worker", PASSWORD).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?locked"));
    }

    @Test
    void fourWrongPasswords_doNotLockTheAccount() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("worker", "gresita-" + i).andExpect(redirectedUrl("/login?error"));
        }

        login("worker", PASSWORD).andExpect(authenticated().withUsername("worker"));
    }

    @Test
    void aSuccessfulLogin_resetsTheCounter() throws Exception {
        for (int i = 0; i < 4; i++) {
            login("worker", "gresita-" + i);
        }
        login("worker", PASSWORD).andExpect(authenticated());
        for (int i = 0; i < 4; i++) {
            login("worker", "iar-gresita-" + i);
        }

        login("worker", PASSWORD).andExpect(authenticated());
    }

    @Test
    void lockingOneAccount_doesNotAffectAnother() throws Exception {
        failFiveTimes("worker");

        login("other", PASSWORD).andExpect(authenticated().withUsername("other"));
    }

    @Test
    void usernameCaseDoesNotEscapeTheLock() throws Exception {
        failFiveTimes("worker");

        login("WORKER", PASSWORD).andExpect(unauthenticated()).andExpect(redirectedUrl("/login?locked"));
    }

    @Test
    void unknownUsernames_areLockedToo_soTheResponseDoesNotRevealWhichNamesExist() throws Exception {
        failFiveTimes("nu-exista");

        login("nu-exista", PASSWORD).andExpect(redirectedUrl("/login?locked"));
    }

    @Test
    void lockedLoginPage_explainsWhatHappened() throws Exception {
        mvc.perform(get("/login").param("locked", ""))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Prea multe încercări greșite")))
            .andExpect(content().string(containsString("15 de minute")));
    }

    @Test
    void manyFailuresFromOneIp_blockEveryLoginFromThatIp_butNotOtherIps() throws Exception {
        for (int i = 0; i < 20; i++) {
            loginFrom("10.0.0.9", "atacator-" + i, "x").andExpect(redirectedUrl("/login?error"));
        }

        loginFrom("10.0.0.9", "worker", PASSWORD).andExpect(redirectedUrl("/login?locked"));
        loginFrom("10.0.0.10", "worker", PASSWORD).andExpect(authenticated());
    }

    @Test
    void registration_isLimitedPerIp() throws Exception {
        for (int i = 0; i < 10; i++) {
            register("user" + i, "parola-buna-" + i).andExpect(redirectedUrl("/login?registered"));
        }

        register("user-prea-mult", "parola-buna-x")
            .andExpect(status().isTooManyRequests())
            .andExpect(content().string(containsString("Prea multe încercări de înregistrare")));
        assertThat(employees.findByUsername("user-prea-mult")).isEmpty();
    }

    @Test
    void registration_limitDoesNotBlockOtherIps() throws Exception {
        for (int i = 0; i < 10; i++) {
            register("user" + i, "parola-buna-" + i);
        }

        mvc.perform(post("/register").with(csrf()).with(request -> {
                request.setRemoteAddr("10.0.0.77");
                return request;
            })
            .param("name", "Alt Birou").param("username", "alt-birou")
            .param("password", "parola-buna-1").param("confirmPassword", "parola-buna-1"))
            .andExpect(redirectedUrl("/login?registered"));
    }

    private ResultActions register(String username, String password) throws Exception {
        return mvc.perform(post("/register").with(csrf())
            .param("name", "Nume " + username).param("username", username)
            .param("password", password).param("confirmPassword", password));
    }
}
