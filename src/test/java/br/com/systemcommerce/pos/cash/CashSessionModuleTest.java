package br.com.systemcommerce.pos.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.dto.CashSupplyRequest;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashMovementService;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
class CashSessionModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_cash_session_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private CashMovementService cashMovementService;

    @Autowired
    private CashSessionRepository cashSessionRepository;

    @Autowired
    private PosTerminalService posTerminalService;

    private String adminToken;
    private UUID adminUserId;
    private UUID terminalId;

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
        var data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        adminToken = data.path("accessToken").asText();
        adminUserId = UUID.fromString(data.path("user").path("id").asText());

        var available = posTerminalService.listAvailable(null, org.springframework.data.domain.Pageable.unpaged());
        terminalId = available.getContent().stream()
                .filter(t -> "TERM-01".equals(t.code()))
                .findFirst()
                .orElseGet(() -> available.getContent().getFirst())
                .id();

        ensureNoActiveSession();
    }

    private void ensureNoActiveSession() {
        withAdminSecurity(() -> cashSessionRepository
                .findActiveByTerminalId(terminalId)
                .ifPresent(active -> {
                    var recon = cashSessionService.reconcile(active.getId());
                    cashSessionService.close(
                            active.getId(),
                            new CashSessionCloseRequest(recon.expectedCash(), "cleanup"),
                            "cleanup-" + UUID.randomUUID());
                }));
    }

    @Test
    void shouldOpenCashSession() throws Exception {
        mockMvc.perform(post("/api/v1/cash-sessions/open")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("Idempotency-Key", "open-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CashSessionOpenRequest(terminalId, new BigDecimal("100.00"), "abertura"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("OPEN"))
                .andExpect(jsonPath("$.data.openingAmount").value(100.00));
    }

    @Test
    void shouldRejectDuplicateOpenOnSameTerminal() {
        withAdminSecurity(() -> {
            cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("50.00"), null), "dup-a-" + UUID.randomUUID());
            assertThatThrownBy(() -> cashSessionService.open(
                            new CashSessionOpenRequest(terminalId, new BigDecimal("60.00"), null),
                            "dup-b-" + UUID.randomUUID()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("sessão aberta");
        });
    }

    @Test
    void shouldRejectInactiveTerminal() {
        withAdminSecurity(() -> {
            posTerminalService.inactivate(terminalId);
            try {
                assertThatThrownBy(() -> cashSessionService.open(
                                new CashSessionOpenRequest(terminalId, BigDecimal.TEN, null), null))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("inativo");
            } finally {
                posTerminalService.activate(terminalId);
            }
        });
    }

    @Test
    void shouldRejectOperatorWithoutPermission() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(), null, java.util.List.of(new SimpleGrantedAuthority("PRODUCT_READ"))));
        try {
            assertThatThrownBy(() -> cashSessionService.open(
                            new CashSessionOpenRequest(terminalId, BigDecimal.TEN, null), null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("permissão");
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    void shouldCloseWithPositiveAndNegativeDifference() {
        withAdminSecurity(() -> {
            var opened = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("100.00"), null),
                    "close-diff-" + UUID.randomUUID());

            cashMovementService.registerSupply(
                    opened.id(),
                    new CashSupplyRequest(
                            opened.id(),
                            new BigDecimal("20.00"),
                            UUID.fromString("c2000000-0000-4000-8000-000000000001"),
                            "suprimento",
                            null),
                    null);

            // expected cash = 120
            var positive = cashSessionService.close(
                    opened.id(), new CashSessionCloseRequest(new BigDecimal("125.00"), "sobra"), null);
            assertThat(positive.status()).isEqualTo(CashSession.CashSessionStatus.CLOSED);
            assertThat(positive.expectedAmount()).isEqualByComparingTo("120.00");
            assertThat(positive.differenceAmount()).isEqualByComparingTo("5.00");

            // nova sessão para diferença negativa
            var opened2 = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("100.00"), null),
                    "close-neg-" + UUID.randomUUID());
            var negative = cashSessionService.close(
                    opened2.id(), new CashSessionCloseRequest(new BigDecimal("90.00"), "falta"), null);
            assertThat(negative.differenceAmount()).isEqualByComparingTo("-10.00");
        });
    }

    @Test
    void shouldRequireJustificationWhenClosingWithDifference() {
        withAdminSecurity(() -> {
            var opened = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("50.00"), null),
                    "just-" + UUID.randomUUID());
            assertThatThrownBy(() -> cashSessionService.close(
                            opened.id(), new CashSessionCloseRequest(new BigDecimal("40.00"), null), null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Justificativa");

            var conference = cashSessionService.conference(
                    opened.id(),
                    new br.com.systemcommerce.pos.cash.dto.CashConferenceRequest(
                            new BigDecimal("40.00"), null));
            assertThat(conference.requiresJustification()).isTrue();
            assertThat(conference.differenceAmount()).isEqualByComparingTo("-10.00");

            var closed = cashSessionService.close(
                    opened.id(), new CashSessionCloseRequest(new BigDecimal("40.00"), "falta justificada"), null);
            assertThat(closed.status()).isEqualTo(CashSession.CashSessionStatus.CLOSED);
            var receipt = cashSessionService.closingReceipt(closed.id());
            assertThat(receipt.differenceAmount()).isEqualByComparingTo("-10.00");
            assertThat(receipt.closingNotes()).contains("falta");
        });
    }

    @Test
    void shouldRejectMovementOnClosedSession() {
        withAdminSecurity(() -> {
            var opened = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("30.00"), null),
                    "closed-mov-" + UUID.randomUUID());
            cashSessionService.close(
                    opened.id(), new CashSessionCloseRequest(new BigDecimal("30.00"), null), null);

            assertThatThrownBy(() -> cashMovementService.registerSupply(
                            opened.id(),
                            new CashSupplyRequest(
                                    opened.id(),
                                    BigDecimal.TEN,
                                    UUID.fromString("c2000000-0000-4000-8000-000000000001"),
                                    "suprimento",
                                    null),
                            null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("fechada");

            assertThatThrownBy(() -> cashSessionService.requireOpenSession(opened.id()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("vendas");
        });
    }

    @Test
    void shouldBeIdempotentOnOpenAndClose() {
        withAdminSecurity(() -> {
            String openKey = "idem-open-" + UUID.randomUUID();
            var first = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("40.00"), null), openKey);
            var second = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("40.00"), null), openKey);
            assertThat(second.id()).isEqualTo(first.id());

            String closeKey = "idem-close-" + UUID.randomUUID();
            var closed1 = cashSessionService.close(
                    first.id(), new CashSessionCloseRequest(new BigDecimal("40.00"), null), closeKey);
            var closed2 = cashSessionService.close(
                    first.id(), new CashSessionCloseRequest(new BigDecimal("40.00"), null), closeKey);
            assertThat(closed2.id()).isEqualTo(closed1.id());
            assertThat(closed2.status()).isEqualTo(CashSession.CashSessionStatus.CLOSED);
        });
    }

    @Test
    void shouldHandleConcurrentOpen() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable task = () -> {
            try {
                start.await();
                withAdminSecurity(() -> cashSessionService.open(
                        new CashSessionOpenRequest(terminalId, new BigDecimal("10.00"), null),
                        "race-" + UUID.randomUUID()));
                successes.incrementAndGet();
            } catch (Exception ex) {
                failures.incrementAndGet();
            } finally {
                done.countDown();
            }
        };
        executor.submit(task);
        executor.submit(task);
        start.countDown();
        assertThat(done.await(60, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(successes.get() + failures.get()).isEqualTo(2);
        assertThat(successes.get()).isBetween(1, 1);
        assertThat(failures.get()).isBetween(1, 1);
    }

    @Test
    void shouldStartClosingAndReconcileViaApi() throws Exception {
        MvcResult openResult = mockMvc.perform(post("/api/v1/cash-sessions/open")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("Idempotency-Key", "api-rec-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CashSessionOpenRequest(terminalId, new BigDecimal("75.00"), null))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID sessionId = UUID.fromString(objectMapper
                .readTree(openResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        mockMvc.perform(post("/api/v1/cash-sessions/" + sessionId + "/start-closing")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CLOSING"));

        mockMvc.perform(get("/api/v1/cash-sessions/" + sessionId + "/reconciliation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openingAmount").value(75.00))
                .andExpect(jsonPath("$.data.expectedCash").value(75.00));

        mockMvc.perform(get("/api/v1/cash-sessions/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(sessionId.toString()));
    }

    private void withAdminSecurity(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("POS_OPEN_CASH"),
                                new SimpleGrantedAuthority("POS_CLOSE_CASH"),
                                new SimpleGrantedAuthority("POS_VIEW_SESSION"),
                                new SimpleGrantedAuthority("POS_FORCE_CLOSE_CASH"),
                                new SimpleGrantedAuthority("POS_TERMINAL_MANAGE"),
                                new SimpleGrantedAuthority("POS_CASH_SUPPLY"),
                                new SimpleGrantedAuthority("POS_CASH_WITHDRAWAL"),
                                new SimpleGrantedAuthority("POS_CASH_MOVEMENT_READ"),
                                new SimpleGrantedAuthority("POS_CASH_MOVEMENT_REVERSE"),
                                new SimpleGrantedAuthority("POS_AUTHORIZE_HIGH_WITHDRAWAL"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
