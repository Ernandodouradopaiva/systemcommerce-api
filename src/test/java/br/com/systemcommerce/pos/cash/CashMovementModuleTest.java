package br.com.systemcommerce.pos.cash;



import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;



import br.com.systemcommerce.pos.cash.dto.CashMovementReverseRequest;

import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;

import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;

import br.com.systemcommerce.pos.cash.dto.CashSupplyRequest;

import br.com.systemcommerce.pos.cash.dto.CashWithdrawalRequest;

import br.com.systemcommerce.pos.cash.entity.CashMovement;

import br.com.systemcommerce.pos.cash.entity.CashSession;

import br.com.systemcommerce.pos.cash.repository.CashMovementRepository;

import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;

import br.com.systemcommerce.pos.cash.service.CashMovementService;

import br.com.systemcommerce.pos.cash.service.CashSessionService;

import br.com.systemcommerce.pos.terminal.service.PosTerminalService;

import br.com.systemcommerce.shared.exception.BusinessRuleException;

import br.com.systemcommerce.shared.exception.ConflictException;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

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

class CashMovementModuleTest {



    private static final UUID REASON_SUPPLY = UUID.fromString("c2000000-0000-4000-8000-000000000001");

    private static final UUID REASON_WITHDRAW = UUID.fromString("c2000000-0000-4000-8000-000000000004");



    @Container

    @ServiceConnection

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")

            .withDatabaseName("systemcommerce_cash_movement_test")

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

    private CashMovementRepository cashMovementRepository;



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



        var available = posTerminalService.listAvailable(null, Pageable.unpaged());

        terminalId = available.getContent().stream()

                .filter(t -> "TERM-01".equals(t.code()))

                .findFirst()

                .orElseGet(() -> available.getContent().getFirst())

                .id();



        ensureNoActiveSession();

    }



    private void ensureNoActiveSession() {

        withCashSecurity(() -> cashSessionRepository

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

    void shouldRegisterSupplyAndUpdatePhysicalBalance() {

        withCashSecurity(() -> {

            var session = openSession("100.00");

            var supply = cashMovementService.registerSupply(

                    session.id(),

                    new CashSupplyRequest(

                            session.id(), new BigDecimal("25.00"), REASON_SUPPLY, "reforço", null),

                    null);



            assertThat(supply.type()).isEqualTo(CashMovement.MovementType.SUPPLY);

            assertThat(supply.amount()).isEqualByComparingTo("25.00");

            assertThat(supply.affectsPhysicalCash()).isTrue();



            var balance = cashMovementService.physicalBalance(session.id());

            assertThat(balance.expectedPhysicalCash()).isEqualByComparingTo("125.00");

            assertThat(balance.opening()).isEqualByComparingTo("100.00");

            assertThat(balance.supplies()).isEqualByComparingTo("25.00");

        });

    }



    @Test

    void shouldRegisterWithdrawalWithinBalance() {

        withCashSecurity(() -> {

            var session = openSession("100.00");

            var withdrawal = cashMovementService.registerWithdrawal(

                    session.id(),

                    new CashWithdrawalRequest(

                            session.id(),

                            new BigDecimal("40.00"),

                            REASON_WITHDRAW,

                            "cofre",

                            null,

                            null),

                    null);



            assertThat(withdrawal.type()).isEqualTo(CashMovement.MovementType.WITHDRAWAL);

            assertThat(cashMovementService.physicalBalance(session.id()).expectedPhysicalCash())

                    .isEqualByComparingTo("60.00");

        });

    }



    @Test

    void shouldRequireAuthWhenWithdrawalExceedsBalance() {

        withLimitedCashSecurity(() -> {

            var session = openSession("50.00");

            assertThatThrownBy(() -> cashMovementService.registerWithdrawal(

                            session.id(),

                            new CashWithdrawalRequest(

                                    session.id(),

                                    new BigDecimal("60.00"),

                                    REASON_WITHDRAW,

                                    null,

                                    null,

                                    null),

                            null))

                    .isInstanceOf(BusinessRuleException.class)

                    .hasMessageContaining("autorização");

        });

    }



    @Test

    void shouldRequireAuthWhenWithdrawalAboveLimit() {

        withLimitedCashSecurity(() -> {

            var session = openSession("1000.00");

            assertThatThrownBy(() -> cashMovementService.registerWithdrawal(

                            session.id(),

                            new CashWithdrawalRequest(

                                    session.id(),

                                    new BigDecimal("501.00"),

                                    REASON_WITHDRAW,

                                    null,

                                    null,

                                    null),

                            null))

                    .isInstanceOf(BusinessRuleException.class)

                    .hasMessageContaining("autorização");

        });

    }



    @Test

    void shouldAllowHighWithdrawalWithAdminPermission() {

        withCashSecurity(() -> {

            var session = openSession("1000.00");

            var withdrawal = cashMovementService.registerWithdrawal(

                    session.id(),

                    new CashWithdrawalRequest(

                            session.id(),

                            new BigDecimal("600.00"),

                            REASON_WITHDRAW,

                            "sangria elevada",

                            null,

                            adminUserId),

                    null);

            assertThat(withdrawal.amount()).isEqualByComparingTo("600.00");

            assertThat(withdrawal.authorizedById()).isEqualTo(adminUserId);

        });

    }



    @Test

    void shouldRejectMovementOnClosedSession() {

        withCashSecurity(() -> {

            var session = openSession("30.00");

            cashSessionService.close(

                    session.id(), new CashSessionCloseRequest(new BigDecimal("30.00"), null), null);



            assertThatThrownBy(() -> cashMovementService.registerSupply(

                            session.id(),

                            new CashSupplyRequest(

                                    session.id(), BigDecimal.TEN, REASON_SUPPLY, null, null),

                            null))

                    .isInstanceOf(BusinessRuleException.class)

                    .hasMessageContaining("fechada");

        });

    }



    @Test

    void shouldBeIdempotentOnSupply() {

        withCashSecurity(() -> {

            var session = openSession("80.00");

            String key = "supply-idem-" + UUID.randomUUID();

            var first = cashMovementService.registerSupply(

                    session.id(),

                    new CashSupplyRequest(

                            session.id(), new BigDecimal("15.00"), REASON_SUPPLY, null, null),

                    key);

            var second = cashMovementService.registerSupply(

                    session.id(),

                    new CashSupplyRequest(

                            session.id(), new BigDecimal("15.00"), REASON_SUPPLY, null, null),

                    key);

            assertThat(second.id()).isEqualTo(first.id());

            assertThat(cashMovementRepository.findByCashSessionIdOrderByOccurredAtAsc(session.id()).stream()

                            .filter(m -> m.getType() == CashMovement.MovementType.SUPPLY)

                            .count())

                    .isEqualTo(1);

        });

    }



    @Test

    void shouldReverseSupplyWithInverseWithdrawal() {

        withCashSecurity(() -> {

            var session = openSession("100.00");

            var supply = cashMovementService.registerSupply(

                    session.id(),

                    new CashSupplyRequest(

                            session.id(), new BigDecimal("20.00"), REASON_SUPPLY, null, null),

                    null);



            var reverse = cashMovementService.reverse(

                    supply.id(), new CashMovementReverseRequest("estorno suprimento"), null);



            assertThat(reverse.type()).isEqualTo(CashMovement.MovementType.WITHDRAWAL);

            assertThat(reverse.reversesMovementId()).isEqualTo(supply.id());

            assertThat(cashMovementService.physicalBalance(session.id()).expectedPhysicalCash())

                    .isEqualByComparingTo("100.00");



            assertThatThrownBy(() -> cashMovementService.reverse(

                            supply.id(), new CashMovementReverseRequest(null), null))

                    .isInstanceOf(ConflictException.class)

                    .hasMessageContaining("já possui estorno");

        });

    }



    @Test

    void shouldRejectOpeningReverse() {

        withCashSecurity(() -> {

            var session = openSession("50.00");

            CashMovement opening = cashMovementRepository.findByCashSessionIdOrderByOccurredAtAsc(session.id()).stream()

                    .filter(m -> m.getType() == CashMovement.MovementType.OPENING)

                    .findFirst()

                    .orElseThrow();

            assertThatThrownBy(() -> cashMovementService.reverse(

                            opening.getId(), new CashMovementReverseRequest(null), null))

                    .isInstanceOf(BusinessRuleException.class)

                    .hasMessageContaining("Abertura");

        });

    }



    @Test

    void shouldListReasonsAndSummaryViaApi() throws Exception {

        UUID sessionId = withCashSecurity(() -> openSession("75.00").id());



        mockMvc.perform(get("/api/v1/cash-movements/reasons")

                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)

                        .param("appliesTo", "SUPPLY"))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.data").isArray())

                .andExpect(jsonPath("$.data[0].code").exists());



        mockMvc.perform(post("/api/v1/cash-movements/supply")

                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)

                        .header("Idempotency-Key", "api-supply-" + UUID.randomUUID())

                        .contentType(MediaType.APPLICATION_JSON)

                        .content(objectMapper.writeValueAsString(new CashSupplyRequest(

                                sessionId, new BigDecimal("10.00"), REASON_SUPPLY, "api", null))))

                .andExpect(status().isCreated())

                .andExpect(jsonPath("$.data.type").value("SUPPLY"));



        mockMvc.perform(get("/api/v1/cash-movements/physical-balance")

                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)

                        .param("cashSessionId", sessionId.toString()))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.data.expectedPhysicalCash").value(85.00));



        mockMvc.perform(get("/api/v1/cash-movements/summary-by-type")

                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)

                        .param("cashSessionId", sessionId.toString()))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.data.expectedPhysicalCash").value(85.00));



        mockMvc.perform(get("/api/v1/cash-movements")

                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)

                        .param("cashSessionId", sessionId.toString()))

                .andExpect(status().isOk())

                .andExpect(jsonPath("$.data").isArray());

    }



    @Test

    void shouldCloseWithSupplyAffectingExpectedCash() {

        withCashSecurity(() -> {

            var opened = openSession("100.00");

            cashMovementService.registerSupply(

                    opened.id(),

                    new CashSupplyRequest(

                            opened.id(), new BigDecimal("20.00"), REASON_SUPPLY, null, null),

                    null);



            var closed = cashSessionService.close(

                    opened.id(), new CashSessionCloseRequest(new BigDecimal("125.00"), "sobra"), null);

            assertThat(closed.status()).isEqualTo(CashSession.CashSessionStatus.CLOSED);

            assertThat(closed.expectedAmount()).isEqualByComparingTo("120.00");

            assertThat(closed.differenceAmount()).isEqualByComparingTo("5.00");

        });

    }



    private br.com.systemcommerce.pos.cash.dto.CashSessionResponse openSession(String opening) {

        return cashSessionService.open(

                new CashSessionOpenRequest(terminalId, new BigDecimal(opening), null),

                "open-mov-" + UUID.randomUUID());

    }



    private void withCashSecurity(Runnable action) {

        withCashSecurity(() -> {

            action.run();

            return null;

        });

    }



    private <T> T withCashSecurity(java.util.function.Supplier<T> action) {

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

            return action.get();

        } finally {

            SecurityContextHolder.clearContext();

        }

    }



    private void withLimitedCashSecurity(Runnable action) {

        SecurityContextHolder.getContext()

                .setAuthentication(new UsernamePasswordAuthenticationToken(

                        adminUserId.toString(),

                        null,

                        java.util.List.of(

                                new SimpleGrantedAuthority("POS_OPEN_CASH"),

                                new SimpleGrantedAuthority("POS_CLOSE_CASH"),

                                new SimpleGrantedAuthority("POS_VIEW_SESSION"),

                                new SimpleGrantedAuthority("POS_CASH_SUPPLY"),

                                new SimpleGrantedAuthority("POS_CASH_WITHDRAWAL"),

                                new SimpleGrantedAuthority("POS_CASH_MOVEMENT_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));

        try {

            action.run();

        } finally {

            SecurityContextHolder.clearContext();

        }

    }

}


