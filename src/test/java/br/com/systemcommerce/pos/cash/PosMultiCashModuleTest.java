package br.com.systemcommerce.pos.cash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalCreateRequest;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.repository.ProductRepository;
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

/**
 * Prompt 67 — vários caixas na mesma loja e isolamento entre lojas.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PosMultiCashModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_multi_cash_test")
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
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    private UUID loja01Id;
    private UUID loja02Id;
    private UUID dep01Id;
    private UUID dep02Id;
    private UUID term01Id;
    private UUID term02Id;
    private UUID productId;
    private UUID adminUserId = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    @BeforeEach
    void setUp() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        // token unused; service calls use security context below
        objectMapper.readTree(login.getResponse().getContentAsString());

        loja01Id = findStore("LOJA-01");
        loja02Id = findStore("LOJA-02");
        dep01Id = findWarehouse(loja01Id, "DEP-01");
        dep02Id = findWarehouse(loja02Id, "DEP-02");
        productId = productRepository.findBySkuIgnoreCase("NB-001").orElseThrow().getId();

        term01Id = findOrCreateTerminal(loja01Id, dep01Id, "TERM-01");
        // segundo terminal na mesma loja
        term02Id = findOrCreateTerminal(loja01Id, dep01Id, "TERM-M2");
    }

    @Test
    void twoTerminalsSameStoreCanHaveIndependentSessions() {
        withAdminAuth(() -> {
            var s1 = cashSessionService.open(new CashSessionOpenRequest(term01Id, BigDecimal.TEN, null), "open-t1");
            // Admin has POS_MULTI_SESSION — can open second
            var s2 = cashSessionService.open(new CashSessionOpenRequest(term02Id, BigDecimal.ONE, null), "open-t2");
            assertThat(s1.id()).isNotEqualTo(s2.id());
            assertThat(s1.storeId()).isEqualTo(loja01Id);
            assertThat(s2.storeId()).isEqualTo(loja01Id);
            assertThat(s1.terminalId()).isEqualTo(term01Id);
            assertThat(s2.terminalId()).isEqualTo(term02Id);
        });
    }

    @Test
    void stockRemainsIsolatedBetweenStores() {
        BigDecimal before01 = inventoryService.availableQuantity(productId, dep01Id);
        BigDecimal before02 = inventoryService.availableQuantity(productId, dep02Id);
        UUID saleId = UUID.randomUUID();
        inventoryService.registerSale(productId, dep01Id, new BigDecimal("1"), saleId);
        assertThat(inventoryService.availableQuantity(productId, dep01Id))
                .isEqualByComparingTo(before01.subtract(BigDecimal.ONE));
        assertThat(inventoryService.availableQuantity(productId, dep02Id)).isEqualByComparingTo(before02);
        inventoryService.registerSaleCancel(productId, dep01Id, new BigDecimal("1"), saleId);
    }

    @Test
    void cannotOpenSecondSessionWithoutMultiSessionPermission() {
        withLimitedAuth(() -> assertThatThrownBy(() -> {
                    cashSessionService.open(
                            new CashSessionOpenRequest(term01Id, BigDecimal.ZERO, null), "lim-1-" + UUID.randomUUID());
                    cashSessionService.open(
                            new CashSessionOpenRequest(term02Id, BigDecimal.ZERO, null), "lim-2-" + UUID.randomUUID());
                })
                .isInstanceOfAny(BusinessRuleException.class, ConflictException.class));
    }

    private UUID findOrCreateTerminal(UUID storeId, UUID warehouseId, String code) {
        return posTerminalService.list(storeId, null, null, null, Pageable.unpaged()).stream()
                .filter(t -> code.equals(t.code()))
                .findFirst()
                .map(t -> t.id())
                .orElseGet(() -> withAdminAuthReturn(() -> posTerminalService
                        .create(new PosTerminalCreateRequest(
                                storeId,
                                warehouseId,
                                code,
                                "Terminal " + code,
                                90 + code.hashCode() % 50,
                                null,
                                null,
                                null))
                        .id()));
    }

    private UUID findStore(String code) {
        return storeService
                .list(null, code, null, null, null, null, null, null, Pageable.unpaged())
                .stream()
                .findFirst()
                .orElseThrow()
                .id();
    }

    private UUID findWarehouse(UUID storeId, String code) {
        return warehouseService.list(storeId, null, null, null, Pageable.unpaged()).stream()
                .filter(w -> code.equals(w.code()))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private void withAdminAuth(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("POS_OPEN_CASH"),
                                new SimpleGrantedAuthority("POS_MULTI_SESSION"),
                                new SimpleGrantedAuthority("POS_TERMINAL_MANAGE"),
                                new SimpleGrantedAuthority("POS_TERMINAL_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private <T> T withAdminAuthReturn(java.util.function.Supplier<T> action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("POS_OPEN_CASH"),
                                new SimpleGrantedAuthority("POS_MULTI_SESSION"),
                                new SimpleGrantedAuthority("POS_TERMINAL_MANAGE"),
                                new SimpleGrantedAuthority("POS_TERMINAL_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void withLimitedAuth(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("POS_OPEN_CASH"),
                                new SimpleGrantedAuthority("POS_TERMINAL_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
