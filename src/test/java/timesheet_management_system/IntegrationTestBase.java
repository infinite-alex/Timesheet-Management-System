package timesheet_management_system;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import timesheet_management_system.model.Employee;
import timesheet_management_system.model.Role;
import timesheet_management_system.repository.ClientRepository;
import timesheet_management_system.repository.EmployeeRepository;
import timesheet_management_system.repository.TimesheetEntryRepository;
import timesheet_management_system.security.LoginThrottle;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class IntegrationTestBase {

    protected static final String PASSWORD = "parola123";

    @Autowired protected MockMvc mvc;
    @Autowired protected EmployeeRepository employees;
    @Autowired protected ClientRepository clients;
    @Autowired protected TimesheetEntryRepository entries;
    @Autowired protected PasswordEncoder encoder;
    @Autowired protected LoginThrottle loginThrottle;

    protected Employee boss;
    protected Employee worker;
    protected Employee other;

    @BeforeEach
    void createUsers() {
        loginThrottle.clear();
        boss = createEmployee("boss", Role.ADMIN);
        worker = createEmployee("worker", Role.ANGAJAT);
        other = createEmployee("other", Role.ANGAJAT);
    }

    protected Employee createEmployee(String username, Role role) {
        return employees.save(new Employee("Nume " + username, username, encoder.encode(PASSWORD), role));
    }

    protected RequestPostProcessor asAdmin() {
        return user("boss").roles("ADMIN");
    }

    protected RequestPostProcessor asWorker() {
        return user("worker").roles("ANGAJAT");
    }

    protected RequestPostProcessor asOther() {
        return user("other").roles("ANGAJAT");
    }

    protected ResultActions postJson(String url, String body, RequestPostProcessor auth) throws Exception {
        return mvc.perform(post(url).with(auth).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected static String entryJson(String date, Long employeeId, Long clientId, String month, int minutes, String note) {
        return """
            {"date":%s,"employeeId":%s,"clientId":%s,"workingmonth":%s,"totalMinutes":%d,"actions":[],"extranote":%s}
            """.formatted(quote(date), employeeId, clientId, quote(month), minutes, quote(note));
    }

    protected static String quote(String value) {
        return value == null ? "null" : "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
