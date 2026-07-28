package br.com.systemcommerce.pos.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.entity.CashSession;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleSuspendRequest;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleDiscardRequest;
import br.com.systemcommerce.pos.sale.dto.SuspendedSaleResumeRequest;
import br.com.systemcommerce.pos.sale.service.PosSaleService;
import br.com.systemcommerce.pos.sale.service.PosSuspendedSaleService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class PosSuspendedSaleModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_suspended_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosSaleService posSaleService;

    @Autowired
    private PosSuspendedSaleService posSuspendedSaleService;

    @Autowired
    private CashSessionService cashSessionService;

    @Autowired
    private CashSessionRepository cashSessionRepository;

    @Autowired
    private PosTerminalService posTerminalService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SaleRepository saleRepository;

    private String adminToken;
    private UUID adminUserId;
    private UUID terminalId;
    private UUID cashSessionId;
    private UUID warehouseId;
    private UUID storeId;
    private Category category;

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
            warehouseId = terminal.warehouseId();
            storeId = terminal.storeId();

            cashSessionRepository.findActiveByTerminalId(terminalId).ifPresent(active -> {
                var recon = cashSessionService.reconcile(active.getId());
                cashSessionService.close(
                        active.getId(),
                        new CashSessionCloseRequest(recon.expectedCash(), "cleanup"),
                        "cleanup-" + UUID.randomUUID());
            });

            var session = cashSessionService.open(
                    new CashSessionOpenRequest(terminalId, new BigDecimal("100.00"), null),
                    "susp-open-" + UUID.randomUUID());
            cashSessionId = session.id();
        });

        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void shouldSuspendWithoutStockExitAndListWithExpiration() {
        withSecurity(() -> {
            var sale = createDraftWithItem(new BigDecimal("40.00"));
            BigDecimal stockBefore = inventoryService.availableQuantity(
                    sale.items().getFirst().productId(), warehouseId);

            var suspended = posSaleService.suspend(
                    sale.id(), new PosSaleSuspendRequest("cliente voltará", sale.version()), null);
            assertThat(suspended.status()).isEqualTo(Sale.SaleStatus.SUSPENDED);

            BigDecimal stockAfter = inventoryService.availableQuantity(
                    sale.items().getFirst().productId(), warehouseId);
            assertThat(stockAfter).isEqualByComparingTo(stockBefore);

            var entity = saleRepository.findDetailedById(sale.id()).orElseThrow();
            assertThat(entity.getSuspendExpiresAt()).isNotNull();
            assertThat(entity.getSuspendedBy()).isNotNull();
            assertThat(entity.getStore().getId()).isEqualTo(storeId);

            var listed = posSuspendedSaleService.list(storeId, null, null, null, false, Pageable.unpaged());
            assertThat(listed.getContent()).anyMatch(s -> s.id().equals(sale.id()));

            var expiration = posSuspendedSaleService.expiration(sale.id());
            assertThat(expiration.expired()).isFalse();
            assertThat(expiration.suspendExpiresAt()).isNotNull();
        });
    }

    @Test
    void shouldResumeWithEditLockAndBlockConcurrentEdit() {
        withSecurity(() -> {
            var sale = createDraftWithItem(new BigDecimal("25.00"));
            var suspended = posSaleService.suspend(
                    sale.id(), new PosSaleSuspendRequest("pause", sale.version()), null);

            var resumed = posSuspendedSaleService.resume(
                    sale.id(),
                    new SuspendedSaleResumeRequest(cashSessionId, suspended.version(), false),
                    "resume-" + UUID.randomUUID());
            assertThat(resumed.status()).isEqualTo(Sale.SaleStatus.DRAFT);

            Sale locked = saleRepository.findDetailedById(sale.id()).orElseThrow();
            assertThat(locked.getEditLockOwner()).isNotNull();
            assertThat(locked.getEditLockToken()).isNotBlank();

            CashSession session = cashSessionRepository.findById(cashSessionId).orElseThrow();
            User current = locked.getEditLockOwner();
            User other = new User();
            other.setId(UUID.randomUUID());
            other.setName("Outro operador");
            locked.setEditLockOwner(other);
            locked.setEditLockAt(Instant.now());

            assertThatThrownBy(() ->
                            posSuspendedSaleService.assertAndRefreshEditLock(
                                    locked, session, current, Instant.now()))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("bloqueada");
        });
    }

    @Test
    void shouldRejectExpiredResume() {
        withSecurity(() -> {
            var sale = createDraftWithItem(new BigDecimal("15.00"));
            posSaleService.suspend(sale.id(), new PosSaleSuspendRequest("exp", sale.version()), null);

            Sale entity = saleRepository.findById(sale.id()).orElseThrow();
            entity.setSuspendExpiresAt(Instant.now().minusSeconds(60));
            saleRepository.saveAndFlush(entity);

            assertThatThrownBy(() -> posSuspendedSaleService.resume(
                            sale.id(),
                            new SuspendedSaleResumeRequest(cashSessionId, null, false),
                            null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("expirada");
        });
    }

    @Test
    void shouldDiscardSuspendedWithAuditViaApi() throws Exception {
        UUID saleId = withSecurity(() -> {
            var sale = createDraftWithItem(new BigDecimal("18.00"));
            var suspended = posSaleService.suspend(
                    sale.id(), new PosSaleSuspendRequest("desc", sale.version()), null);
            return suspended.id();
        });

        var body = new SuspendedSaleDiscardRequest(cashSessionId, "cliente desistiu", null);
        mockMvc.perform(post("/api/v1/pos/suspended-sales/" + saleId + "/discard")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("Idempotency-Key", "discard-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/pos/suspended-sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("storeId", storeId.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void shouldSearchByNumber() throws Exception {
        String saleNumber = withSecurity(() -> {
            var sale = createDraftWithItem(new BigDecimal("12.00"));
            var suspended = posSaleService.suspend(
                    sale.id(), new PosSaleSuspendRequest("s", sale.version()), null);
            return suspended.saleNumber();
        });

        mockMvc.perform(get("/api/v1/pos/suspended-sales/by-number")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("saleNumber", saleNumber))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    private br.com.systemcommerce.sale.dto.SaleResponse createDraftWithItem(BigDecimal price) {
        Product product = createProduct(price);
        seedStock(product.getId(), "20");
        var sale = posSaleService.start(new PosSaleStartRequest(cashSessionId), "start-" + UUID.randomUUID());
        return posSaleService.addByProductId(
                sale.id(),
                new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, sale.version()),
                null);
    }

    private Product createProduct(BigDecimal price) {
        Product product = new Product();
        product.setInternalCode("SUS-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Produto suspenso " + product.getSku());
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(price);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(storeId, saved.getId()));
        return saved;
    }

    private void seedStock(UUID productId, String qty) {
        inventoryService.registerEntry(
                new InventoryEntryRequest(productId, warehouseId, new BigDecimal(qty), "seed susp", false));
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
                                    new SimpleGrantedAuthority("POS_CLOSE_CASH"),
                                    new SimpleGrantedAuthority("POS_VIEW_SESSION"),
                                    new SimpleGrantedAuthority("POS_FORCE_CLOSE_CASH"),
                                    new SimpleGrantedAuthority("POS_TERMINAL_MANAGE"),
                                    new SimpleGrantedAuthority("POS_SALE_CREATE"),
                                    new SimpleGrantedAuthority("POS_SALE_ITEM_REMOVE"),
                                    new SimpleGrantedAuthority("POS_SALE_DISCOUNT"),
                                    new SimpleGrantedAuthority("POS_SALE_SUSPEND"),
                                    new SimpleGrantedAuthority("POS_SALE_CANCEL"),
                                    new SimpleGrantedAuthority("POS_SUSPENDED_SALE_READ"),
                                    new SimpleGrantedAuthority("POS_SUSPENDED_SALE_RESUME"),
                                    new SimpleGrantedAuthority("POS_SUSPENDED_SALE_RESUME_OTHER_OPERATOR"),
                                    new SimpleGrantedAuthority("POS_SUSPENDED_SALE_DISCARD"),
                                    new SimpleGrantedAuthority("SALE_CONFIRM"),
                                    new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                    new SimpleGrantedAuthority("INVENTORY_READ"),
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
}
