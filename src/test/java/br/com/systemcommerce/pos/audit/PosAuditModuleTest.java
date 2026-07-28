package br.com.systemcommerce.pos.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.settings.dto.PosSettingUpsertRequest;
import br.com.systemcommerce.pos.settings.entity.PosSettingKeys;
import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import br.com.systemcommerce.pos.settings.service.PosSettingService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.shared.audit.AuditLogRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.support.IntegrationTestUsers;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
class PosAuditModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_audit_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private PosTerminalService posTerminalService;

    @Autowired
    private PosSettingService posSettingService;

    @Autowired
    private PosAuditService posAuditService;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID adminUserId;
    private UUID storeId;
    private UUID terminalId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult login = mockMvc.perform(postLogin())
                .andExpect(status().isOk())
                .andReturn();
        var data = objectMapper.readTree(login.getResponse().getContentAsString()).path("data");
        adminToken = data.path("accessToken").asText();
        adminUserId = UUID.fromString(data.path("user").path("id").asText());

        withSecurity(() -> {
            var available = posTerminalService.listAvailable(null, Pageable.unpaged());
            var terminal = available.getContent().stream()
                    .filter(t -> "TERM-01".equals(t.code()))
                    .findFirst()
                    .orElseGet(() -> available.getContent().getFirst());
            terminalId = terminal.id();
            storeId = terminal.storeId();
        });
    }

    @Test
    void shouldAuditCashOpenWithContextAndListViaApi() throws Exception {
        UUID sessionId = withSecurity(() -> {
            var open = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("100.00"), "audit open"),
                    "pos-audit-open-" + UUID.randomUUID());
            return open.id();
        });

        mockMvc.perform(get("/api/v1/pos/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("eventCode", "CASH_OPEN")
                        .param("storeId", storeId.toString())
                        .param("terminalId", terminalId.toString())
                        .param("cashSessionId", sessionId.toString())
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].eventCode").value("CASH_OPEN"))
                .andExpect(jsonPath("$.data[0].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$.data[0].storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data[0].terminalId").value(terminalId.toString()))
                .andExpect(jsonPath("$.data[0].cashSessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.page.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void shouldAuditDeniedOpenWithoutPermission() {
        withSecurityLimited(() -> {
            try {
                cashSessionService.open(
                        new CashSessionOpenRequest(terminalId, new BigDecimal("50.00"), null), null);
            } catch (BusinessRuleException ignored) {
                // esperado
            }
        });

        var denied = auditLogRepository.findAll().stream()
                .filter(l -> "CASH_OPEN_DENIED".equals(l.getEventCode()))
                .filter(l -> terminalId.equals(l.getTerminalId()) || l.getTerminalId() == null)
                .toList();
        assertThat(denied).isNotEmpty();
        assertThat(denied.getLast().getOutcome()).isEqualTo("DENIED");
        assertThat(denied.getLast().getErrorCode()).isEqualTo("POS_OPEN_DENIED");
    }

    @Test
    void shouldSanitizeSensitivePayloadInPosAudit() {
        withSecurity(() -> {
            posAuditService.success(
                    PosAuditEventCode.PAYMENT_ATTEMPT,
                    PosAuditContext.builder()
                            .storeId(storeId)
                            .terminalId(terminalId)
                            .entity("Payment", UUID.randomUUID())
                            .after(Map.of(
                                    "amount",
                                    10,
                                    "cvv",
                                    "123",
                                    "cardNumber",
                                    "4111111111111111",
                                    "password",
                                    "secret",
                                    "method",
                                    "CREDIT"))
                            .details("payload sanitizado")
                            .build());
        });

        var entry = auditLogRepository.findAll().stream()
                .filter(l -> "PAYMENT_ATTEMPT".equals(l.getEventCode()))
                .filter(l -> l.getDetails() != null && l.getDetails().contains("payload sanitizado"))
                .findFirst()
                .orElseThrow();
        assertThat(entry.getNewValues()).contains("[REDACTED]");
        assertThat(entry.getNewValues()).doesNotContain("4111111111111111");
        assertThat(entry.getNewValues()).doesNotContain("\"123\"");
        assertThat(entry.getNewValues()).contains("CREDIT");
    }

    @Test
    void shouldAuditSettingsChange() {
        withSecurity(() -> posSettingService.upsert(new PosSettingUpsertRequest(
                PosSettingKeys.RECEIPT_FOOTER_MESSAGE,
                PosSettingScope.STORE,
                storeId,
                null,
                "Audit footer",
                "teste auditoria",
                null)));

        var found = auditLogRepository.findAll().stream()
                .filter(l -> "SETTINGS_CHANGE".equals(l.getEventCode()))
                .filter(l -> storeId.equals(l.getStoreId()))
                .toList();
        assertThat(found).isNotEmpty();
        assertThat(found.getLast().getOutcome()).isEqualTo("SUCCESS");
    }

    @Test
    void shouldForbidAuditListWithoutPermission() throws Exception {
        User seller = IntegrationTestUsers.createUser(
                userRepository, roleRepository, passwordEncoder, "SELLER");
        MvcResult login = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"%s","password":"Test@1234"}
                                """
                                        .formatted(seller.getLogin())))
                .andExpect(status().isOk())
                .andReturn();
        String sellerToken = objectMapper
                .readTree(login.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();

        mockMvc.perform(get("/api/v1/pos/audit-logs").header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postLogin() {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                        {"username":"admin","password":"Admin@123"}
                        """);
    }

    private void withSecurity(Runnable action) {
        withSecurity(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withSecurity(java.util.concurrent.Callable<T> action) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            adminUserId.toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("POS_OPEN_CASH"),
                                    new SimpleGrantedAuthority("POS_VIEW_SESSION"),
                                    new SimpleGrantedAuthority("POS_SETTINGS_READ"),
                                    new SimpleGrantedAuthority("POS_SETTINGS_MANAGE"),
                                    new SimpleGrantedAuthority("POS_TERMINAL_READ"),
                                    new SimpleGrantedAuthority("POS_AUDIT_READ"),
                                    new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }

    private void withSecurityLimited(Runnable action) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            adminUserId.toString(),
                            null,
                            List.of(new SimpleGrantedAuthority("POS_TERMINAL_READ"))));
            action.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
