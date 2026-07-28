package br.com.systemcommerce.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.employee.dto.EmployeeAssignmentCreateRequest;
import br.com.systemcommerce.employee.dto.EmployeeAssignmentEndRequest;
import br.com.systemcommerce.employee.dto.EmployeeCreateRequest;
import br.com.systemcommerce.employee.dto.EmployeeLinkUserRequest;
import br.com.systemcommerce.employee.dto.EmployeeUpdateRequest;
import br.com.systemcommerce.employee.entity.Employee;
import br.com.systemcommerce.employee.entity.EmployeeStoreAssignment;
import br.com.systemcommerce.employee.service.EmployeeService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.dto.StoreCreateRequest;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class EmployeeModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_employee_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID loja01Id;
    private UUID defaultOrgId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
        defaultOrgId = organizationService.getDefault().id();
        loja01Id = storeService
                .list(null, "LOJA-01", null, null, null, null, null, null, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .getFirst()
                .id();
    }

    @Test
    void shouldSeedDefaultEmployeeSeparateFromUser() throws Exception {
        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("search", "EMP-0001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].registrationNumber").value("EMP-0001"))
                .andExpect(jsonPath("$.data[0].userId", nullValue()));

        mockMvc.perform(get("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("storeId", loja01Id.toString())
                        .param("jobTitle", "Vendedor")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.registrationNumber == 'EMP-0001')]").exists());
    }

    @Test
    void shouldCreateEmployeeWithMultipleStoreAssignmentsAndPrimary() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var branch = storeService.create(new StoreCreateRequest(
                defaultOrgId,
                "LJ-E-" + suffix,
                "Loja Emp " + suffix,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        MvcResult created = mockMvc.perform(post("/api/v1/employees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeCreate("MAT-" + suffix, "Prof " + suffix))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.registrationNumber").value("MAT-" + suffix))
                .andReturn();

        UUID employeeId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmployeeAssignmentCreateRequest(
                                loja01Id,
                                EmployeeStoreAssignment.AssignmentType.PERMANENT,
                                LocalDate.now(),
                                null,
                                true,
                                "Caixa",
                                null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.primaryAssignment").value(true));

        mockMvc.perform(post("/api/v1/employees/" + employeeId + "/assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmployeeAssignmentCreateRequest(
                                branch.id(),
                                EmployeeStoreAssignment.AssignmentType.TEMPORARY,
                                LocalDate.now(),
                                LocalDate.now().plusMonths(1),
                                false,
                                "Apoio",
                                null))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        mockMvc.perform(get("/api/v1/employees/" + employeeId + "/primary-store")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(loja01Id.toString()));
    }

    @Test
    void shouldKeepAssignmentHistoryWhenEnding() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var employee = employeeService.create(employeeCreate("HIS-" + suffix, "Hist " + suffix));
        var assignment = employeeService.createAssignment(
                employee.id(),
                new EmployeeAssignmentCreateRequest(
                        loja01Id,
                        EmployeeStoreAssignment.AssignmentType.PERMANENT,
                        LocalDate.now().minusDays(10),
                        null,
                        true,
                        "Vendedor",
                        null));

        var ended = employeeService.endAssignment(
                employee.id(),
                assignment.id(),
                new EmployeeAssignmentEndRequest(LocalDate.now(), "Encerrado para teste"));

        assertThat(ended.status()).isEqualTo(EmployeeStoreAssignment.AssignmentStatus.ENDED);
        assertThat(ended.endDate()).isEqualTo(LocalDate.now());
        assertThat(employeeService.listAssignmentHistory(employee.id())).hasSize(1);
        assertThat(employeeService.listAssignmentHistory(employee.id()).getFirst().status())
                .isEqualTo(EmployeeStoreAssignment.AssignmentStatus.ENDED);
    }

    @Test
    void shouldRejectTemporaryWithoutEndDateAndTerminatedNewAssignment() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var employee = employeeService.create(employeeCreate("TMP-" + suffix, "Temp " + suffix));

        assertThatThrownBy(() -> employeeService.createAssignment(
                        employee.id(),
                        new EmployeeAssignmentCreateRequest(
                                loja01Id,
                                EmployeeStoreAssignment.AssignmentType.TEMPORARY,
                                LocalDate.now(),
                                null,
                                false,
                                null,
                                null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("término");

        employeeService.update(
                employee.id(),
                new EmployeeUpdateRequest(
                        employee.registrationNumber(),
                        employee.name(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        employee.admissionDate(),
                        LocalDate.now(),
                        employee.jobTitle(),
                        Employee.EmployeeStatus.TERMINATED,
                        true,
                        null));

        assertThatThrownBy(() -> employeeService.createAssignment(
                        employee.id(),
                        new EmployeeAssignmentCreateRequest(
                                loja01Id,
                                EmployeeStoreAssignment.AssignmentType.PERMANENT,
                                LocalDate.now(),
                                null,
                                true,
                                null,
                                null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("desligado");
    }

    @Test
    void shouldEnforceUniqueRegistrationCpfAndSingleUserLink() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String cpf = String.format("%011d", Math.floorMod(suffix.hashCode(), 1_000_000_000));

        employeeService.create(new EmployeeCreateRequest(
                defaultOrgId,
                "CPF-" + suffix,
                "Com CPF",
                null,
                cpf,
                null,
                null,
                null,
                null,
                null,
                LocalDate.now(),
                null,
                "Estoquista",
                Employee.EmployeeStatus.ACTIVE,
                true,
                null,
                null));

        assertThatThrownBy(() -> employeeService.create(new EmployeeCreateRequest(
                        defaultOrgId,
                        "CPF2-" + suffix,
                        "Outro",
                        null,
                        cpf,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        Employee.EmployeeStatus.ACTIVE,
                        true,
                        null,
                        null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CPF");

        assertThatThrownBy(() -> employeeService.create(employeeCreate("CPF-" + suffix, "Dup matricula")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Matricula");

        User user = new User();
        user.setName("User Emp " + suffix);
        user.setEmail("emp-" + suffix.toLowerCase() + "@test.local");
        user.setLogin("emp" + suffix.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode("Admin@123"));
        user.setStatus(User.UserStatus.ACTIVE);
        user = userRepository.save(user);

        var first = employeeService.create(employeeCreate("USR-" + suffix, "Com User"));
        employeeService.linkUser(first.id(), new EmployeeLinkUserRequest(user.getId()));

        var second = employeeService.create(employeeCreate("USR2-" + suffix, "Sem User"));
        User finalUser = user;
        assertThatThrownBy(() -> employeeService.linkUser(second.id(), new EmployeeLinkUserRequest(finalUser.getId())))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Usuario");
    }

    @Test
    void shouldSwitchPrimaryAssignmentKeepingHistory() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var branch = storeService.create(new StoreCreateRequest(
                defaultOrgId,
                "LJ-P-" + suffix,
                "Loja Prim " + suffix,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                true,
                true,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
        var employee = employeeService.create(employeeCreate("PRI-" + suffix, "Prim " + suffix));
        var first = employeeService.createAssignment(
                employee.id(),
                new EmployeeAssignmentCreateRequest(
                        loja01Id,
                        EmployeeStoreAssignment.AssignmentType.PERMANENT,
                        LocalDate.now(),
                        null,
                        true,
                        "Principal",
                        null));
        var second = employeeService.createAssignment(
                employee.id(),
                new EmployeeAssignmentCreateRequest(
                        branch.id(),
                        EmployeeStoreAssignment.AssignmentType.PERMANENT,
                        LocalDate.now(),
                        null,
                        true,
                        "Nova principal",
                        null));

        assertThat(second.primaryAssignment()).isTrue();
        assertThat(employeeService.listAssignmentHistory(employee.id()).stream()
                        .filter(a -> a.id().equals(first.id()))
                        .findFirst()
                        .orElseThrow()
                        .primaryAssignment())
                .isFalse();
        assertThat(employeeService.getPrimaryStore(employee.id()).storeId()).isEqualTo(branch.id());
    }

    @Test
    void shouldProtectEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/employees")).andExpect(status().isUnauthorized());
    }

    private EmployeeCreateRequest employeeCreate(String registration, String name) {
        return new EmployeeCreateRequest(
                defaultOrgId,
                registration,
                name,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.now(),
                null,
                "Vendedor",
                Employee.EmployeeStatus.ACTIVE,
                true,
                null,
                null);
    }
}
