package br.com.systemcommerce.pos.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByBarcodeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleDiscardRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleHeaderDiscountRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleQuantityRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleResumeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleSurchargeRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleSuspendRequest;
import br.com.systemcommerce.pos.sale.service.PosSaleService;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.pricing.dto.DiscountAuthorizationDecisionRequest;
import br.com.systemcommerce.pricing.dto.DiscountAuthorizationRequest;
import br.com.systemcommerce.pricing.dto.PriceTableCreateRequest;
import br.com.systemcommerce.pricing.dto.ProductPriceLinkRequest;
import br.com.systemcommerce.pricing.entity.DiscountAuthorization;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PriceTableScopeType;
import br.com.systemcommerce.pricing.entity.ProductPrice;
import br.com.systemcommerce.pricing.service.DiscountAuthorizationService;
import br.com.systemcommerce.pricing.service.PriceTableService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class PosSaleModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_sale_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosSaleService posSaleService;

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
    private SaleService saleService;

    @Autowired
    private PriceTableService priceTableService;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private DiscountAuthorizationService discountAuthorizationService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        adminUserId = UUID.fromString(data.path("user").path("id").asText());

        withPosSecurity(() -> {
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
                    "pos-sale-open-" + UUID.randomUUID());
            cashSessionId = session.id();
        });

        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void shouldAddByValidBarcode() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000001", true, new BigDecimal("25.00"));
            seedStock(product.getId(), "10");
            var sale = startSale();
            var updated = posSaleService.addByBarcode(
                    sale.id(),
                    new PosSaleAddByBarcodeRequest("7899000000001", BigDecimal.ONE, sale.version()),
                    null);
            assertThat(updated.items()).hasSize(1);
            assertThat(updated.items().getFirst().unitPrice()).isEqualByComparingTo("25.00");
            assertThat(updated.totalAmount()).isEqualByComparingTo("25.00");
            assertThat(updated.channel()).isEqualTo(Sale.SaleChannel.POS);
            assertThat(updated.warehouseId()).isEqualTo(warehouseId);
        });
    }

    @Test
    void shouldRejectUnknownBarcode() {
        withPosSecurity(() -> {
            var sale = startSale();
            assertThatThrownBy(() -> posSaleService.addByBarcode(
                            sale.id(),
                            new PosSaleAddByBarcodeRequest("0000000000000", BigDecimal.ONE, null),
                            null))
                    .isInstanceOf(ResourceNotFoundException.class);
        });
    }

    @Test
    void shouldRejectDuplicateBarcode() {
        withPosSecurity(() -> {
            Product a = createProduct(null, true, new BigDecimal("10"));
            Product b = createProduct(null, true, new BigDecimal("12"));
            // força ambiguidade removendo o índice único temporariamente
            jdbcTemplate.execute("DROP INDEX IF EXISTS uk_products_barcode_not_null");
            jdbcTemplate.update("UPDATE products SET barcode = ? WHERE id = ?", "DUPBARCODE999", a.getId());
            jdbcTemplate.update("UPDATE products SET barcode = ? WHERE id = ?", "DUPBARCODE999", b.getId());

            var sale = startSale();
            try {
                assertThatThrownBy(() -> posSaleService.addByBarcode(
                                sale.id(),
                                new PosSaleAddByBarcodeRequest("DUPBARCODE999", BigDecimal.ONE, null),
                                null))
                        .isInstanceOf(ConflictException.class)
                        .hasMessageContaining("mais de um produto");
            } finally {
                jdbcTemplate.update("UPDATE products SET barcode = NULL WHERE id IN (?, ?)", a.getId(), b.getId());
                jdbcTemplate.execute(
                        """
                        CREATE UNIQUE INDEX IF NOT EXISTS uk_products_barcode_not_null
                            ON products (barcode)
                            WHERE barcode IS NOT NULL AND LENGTH(TRIM(barcode)) > 0
                        """);
            }
        });
    }

    @Test
    void shouldRejectInactiveProduct() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000002", true, new BigDecimal("10"));
            seedStock(product.getId(), "5");
            product.markInactive();
            productRepository.saveAndFlush(product);

            var sale = startSale();
            assertThatThrownBy(() -> posSaleService.addByBarcode(
                            sale.id(),
                            new PosSaleAddByBarcodeRequest("7899000000002", BigDecimal.ONE, null),
                            null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("inativo");
        });
    }

    @Test
    void shouldRejectInsufficientStock() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000003", false, new BigDecimal("10"));
            seedStock(product.getId(), "1");
            var sale = startSale();
            assertThatThrownBy(() -> posSaleService.addByProductId(
                            sale.id(),
                            new PosSaleAddByProductIdRequest(product.getId(), new BigDecimal("2"), null),
                            null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Estoque insuficiente");
        });
    }

    @Test
    void shouldRejectInvalidQuantity() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000004", true, new BigDecimal("10"));
            seedStock(product.getId(), "5");
            var sale = startSale();
            var added = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, null),
                    null);
            assertThatThrownBy(() -> posSaleService.updateQuantity(
                            sale.id(),
                            added.items().getFirst().id(),
                            new PosSaleQuantityRequest(BigDecimal.ZERO, null),
                            null))
                    .isInstanceOf(BusinessRuleException.class);
        });
    }

    @Test
    void shouldAllowDiscountWithinOperatorLimit() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000005", true, new BigDecimal("100.00"));
            seedStock(product.getId(), "5");
            var sale = startSale();
            sale = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, null),
                    null);
            // 10% = limite do operador
            var discounted = posSaleService.headerDiscount(
                    sale.id(),
                    new PosSaleHeaderDiscountRequest(new BigDecimal("10.00"), sale.version(), null),
                    null);
            assertThat(discounted.discountAmount()).isEqualByComparingTo("10.00");
            assertThat(discounted.totalAmount()).isEqualByComparingTo("90.00");
        });
    }

    @Test
    void shouldApplySurchargeAndRecalculateTotal() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000015", true, new BigDecimal("100.00"));
            seedStock(product.getId(), "5");
            var sale = startSale();
            sale = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, null),
                    null);
            var withSurcharge = posSaleService.applySurcharge(
                    sale.id(),
                    new PosSaleSurchargeRequest(new BigDecimal("7.50"), sale.version()),
                    "surcharge-" + UUID.randomUUID());
            assertThat(withSurcharge.surchargeAmount()).isEqualByComparingTo("7.50");
            assertThat(withSurcharge.totalAmount()).isEqualByComparingTo("107.50");
        });
    }

    @Test
    void shouldRejectDiscountAboveOperatorLimitWithoutHighPermission() {
        withLimitedDiscountSecurity(() -> {
            // Força limite baixo no perfil ADMIN para este cenário
            jdbcTemplate.update(
                    "UPDATE operator_discount_limits SET max_percent = 10 WHERE role_id = (SELECT id FROM roles WHERE code = 'ADMIN')");
            try {
                Product product = createProduct("7899000000006", true, new BigDecimal("100.00"));
                seedStock(product.getId(), "5");
                var sale = startSale();
                var prepared = posSaleService.addByProductId(
                        sale.id(),
                        new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, null),
                        null);
                assertThatThrownBy(() -> posSaleService.headerDiscount(
                                prepared.id(),
                                new PosSaleHeaderDiscountRequest(new BigDecimal("15.00"), null, null),
                                null))
                        .isInstanceOf(BusinessRuleException.class)
                        .hasMessageContaining("POS_DISCOUNT_AUTHORIZE");
            } finally {
                jdbcTemplate.update(
                        "UPDATE operator_discount_limits SET max_percent = 100 WHERE role_id = (SELECT id FROM roles WHERE code = 'ADMIN')");
            }
        });
    }

    @Test
    void shouldPreservePriceSnapshotAfterCatalogChange() {
        withPosSecurity(() -> {
            Product product = createProduct(
                    "7899" + UUID.randomUUID().toString().replace("-", "").substring(0, 9),
                    true,
                    new BigDecimal("100.00"));
            seedStock(product.getId(), "5");
            UUID storeId = storeRepository.findByCodeIgnoreCase("LOJA-01").orElseThrow().getId();
            var table = priceTableService.create(new PriceTableCreateRequest(
                    "SNAP-" + UUID.randomUUID().toString().substring(0, 8),
                    "Snapshot",
                    null,
                    30,
                    PriceChannel.POS,
                    PriceTableScopeType.STORE,
                    null,
                    null,
                    null));
            priceTableService.linkStore(table.id(), storeId);
            var productPrice = priceTableService.linkProduct(
                    table.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("85.00"),
                            ProductPrice.PriceType.STANDARD,
                            BigDecimal.ONE,
                            10,
                            null,
                            null,
                            null));

            var sale = startSale();
            sale = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, null),
                    null);
            var item = sale.items().getFirst();
            assertThat(item.unitPrice()).isEqualByComparingTo("85.00");
            assertThat(item.priceSource()).isEqualTo(SaleItem.PriceSource.PRICE_TABLE);
            assertThat(item.priceTableId()).isEqualTo(table.id());
            assertThat(item.productPriceId()).isEqualTo(productPrice.id());

            product.setSalePrice(new BigDecimal("200.00"));
            productRepository.saveAndFlush(product);
            priceTableService.updateProductPrice(
                    table.id(),
                    productPrice.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("10.00"),
                            ProductPrice.PriceType.STANDARD,
                            BigDecimal.ONE,
                            10,
                            null,
                            null,
                            null));

            var reloaded = posSaleService.summary(sale.id());
            assertThat(reloaded.items().getFirst().unitPrice()).isEqualByComparingTo("85.00");
            assertThat(reloaded.items().getFirst().priceSource()).isEqualTo(SaleItem.PriceSource.PRICE_TABLE);
        });
    }

    @Test
    void shouldAllowDiscountAfterApprovedAuthorization() {
        jdbcTemplate.update(
                "UPDATE operator_discount_limits SET max_percent = 10 WHERE role_id = (SELECT id FROM roles WHERE code = 'ADMIN')");
        try {
            Product product = createProduct(
                    "7899" + UUID.randomUUID().toString().replace("-", "").substring(0, 9),
                    true,
                    new BigDecimal("100.00"));
            seedStock(product.getId(), "5");

            final UUID[] saleHolder = new UUID[1];
            final UUID[] authHolder = new UUID[1];

            withLimitedDiscountSecurity(() -> {
                var sale = startSale();
                var prepared = posSaleService.addByProductId(
                        sale.id(),
                        new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, null),
                        null);
                saleHolder[0] = prepared.id();
                var auth = discountAuthorizationService.request(new DiscountAuthorizationRequest(
                        prepared.id(), null, new BigDecimal("15.00"), "cliente fidelidade"));
                assertThat(auth.status()).isEqualTo(DiscountAuthorization.Status.PENDING);
                authHolder[0] = auth.id();
            });

            withAuthorizeDiscountSecurity(() -> discountAuthorizationService.approve(
                    authHolder[0], new DiscountAuthorizationDecisionRequest("ok gerente")));

            withLimitedDiscountSecurity(() -> {
                var discounted = posSaleService.headerDiscount(
                        saleHolder[0],
                        new PosSaleHeaderDiscountRequest(new BigDecimal("15.00"), null, null),
                        null);
                assertThat(discounted.discountAmount()).isEqualByComparingTo("15.00");
                assertThat(discounted.totalAmount()).isEqualByComparingTo("85.00");
            });
        } finally {
            jdbcTemplate.update(
                    "UPDATE operator_discount_limits SET max_percent = 100 WHERE role_id = (SELECT id FROM roles WHERE code = 'ADMIN')");
        }
    }

    @Test
    void shouldSuspendAndResumeWithoutStockDebit() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000007", true, new BigDecimal("20.00"));
            seedStock(product.getId(), "5");
            var sale = startSale();
            sale = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, null),
                    null);

            var suspended = posSaleService.suspend(
                    sale.id(), new PosSaleSuspendRequest("cliente saiu", sale.version()), null);
            assertThat(suspended.status()).isEqualTo(Sale.SaleStatus.SUSPENDED);
            assertThat(inventoryService.availableQuantity(product.getId(), warehouseId))
                    .isEqualByComparingTo("5.000");

            var resumed = posSaleService.resume(
                    sale.id(), new PosSaleResumeRequest(cashSessionId, suspended.version()), null);
            assertThat(resumed.status()).isEqualTo(Sale.SaleStatus.DRAFT);
            assertThat(resumed.items()).hasSize(1);
        });
    }

    @Test
    void shouldBeIdempotentOnStartAndAdd() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000008", true, new BigDecimal("15.00"));
            seedStock(product.getId(), "5");
            String startKey = "start-" + UUID.randomUUID();
            var first = posSaleService.start(new PosSaleStartRequest(cashSessionId), startKey);
            var second = posSaleService.start(new PosSaleStartRequest(cashSessionId), startKey);
            assertThat(second.id()).isEqualTo(first.id());

            String addKey = "add-" + UUID.randomUUID();
            var added1 = posSaleService.addByBarcode(
                    first.id(),
                    new PosSaleAddByBarcodeRequest("7899000000008", BigDecimal.ONE, null),
                    addKey);
            var added2 = posSaleService.addByBarcode(
                    first.id(),
                    new PosSaleAddByBarcodeRequest("7899000000008", BigDecimal.ONE, null),
                    addKey);
            assertThat(added2.items()).hasSize(1);
            assertThat(added2.items().getFirst().quantity()).isEqualByComparingTo(added1.items().getFirst().quantity());
        });
    }

    @Test
    void shouldDetectOptimisticConcurrencyConflict() throws Exception {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000009", true, new BigDecimal("10.00"));
            seedStock(product.getId(), "10");
            var sale = startSale();
            sale = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, null),
                    null);
            Long staleVersion = sale.version();
            UUID itemId = sale.items().getFirst().id();
            UUID saleId = sale.id();

            posSaleService.updateQuantity(
                    saleId, itemId, new PosSaleQuantityRequest(new BigDecimal("2"), staleVersion), null);

            assertThatThrownBy(() -> posSaleService.updateQuantity(
                            saleId,
                            itemId,
                            new PosSaleQuantityRequest(new BigDecimal("3"), staleVersion),
                            null))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("Versão");
        });
    }

    @Test
    void shouldConfirmDebitWarehouseStock() {
        withPosSecurity(() -> {
            Product product = createProduct("7899000000010", true, new BigDecimal("30.00"));
            seedStock(product.getId(), "4");
            var sale = startSale();
            sale = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), new BigDecimal("2"), null),
                    null);
            var confirmed = saleService.confirm(sale.id());
            assertThat(confirmed.status()).isEqualTo(Sale.SaleStatus.CONFIRMED);
            assertThat(inventoryService.availableQuantity(product.getId(), warehouseId))
                    .isEqualByComparingTo("2.000");
        });
    }

    @Test
    void shouldDiscardDraft() {
        withPosSecurity(() -> {
            var sale = startSale();
            var discarded = posSaleService.discard(
                    sale.id(), new PosSaleDiscardRequest("desistiu", sale.version()), null);
            assertThat(discarded.status()).isEqualTo(Sale.SaleStatus.CANCELLED);
        });
    }

    private br.com.systemcommerce.sale.dto.SaleResponse startSale() {
        return posSaleService.start(new PosSaleStartRequest(cashSessionId), "sale-" + UUID.randomUUID());
    }

    private Product createProduct(String barcode, boolean allowNegative, BigDecimal price) {
        Product product = new Product();
        product.setInternalCode("POS-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setBarcode(barcode);
        product.setName("Produto PDV " + product.getSku());
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(price);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(allowNegative);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(storeId, saved.getId()));
        return saved;
    }

    private void seedStock(UUID productId, String qty) {
        inventoryService.registerEntry(
                new InventoryEntryRequest(productId, warehouseId, new BigDecimal(qty), "seed pos", false));
    }

    private void withPosSecurity(Runnable action) {
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
                                new SimpleGrantedAuthority("POS_SALE_HIGH_DISCOUNT"),
                                new SimpleGrantedAuthority("POS_SALE_SUSPEND"),
                                new SimpleGrantedAuthority("POS_SALE_CANCEL"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                new SimpleGrantedAuthority("INVENTORY_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void withLimitedDiscountSecurity(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority("POS_OPEN_CASH"),
                                new SimpleGrantedAuthority("POS_CLOSE_CASH"),
                                new SimpleGrantedAuthority("POS_VIEW_SESSION"),
                                new SimpleGrantedAuthority("POS_SALE_CREATE"),
                                new SimpleGrantedAuthority("POS_SALE_DISCOUNT"),
                                new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                new SimpleGrantedAuthority("INVENTORY_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void withAuthorizeDiscountSecurity(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        List.of(
                                new SimpleGrantedAuthority("POS_DISCOUNT_AUTHORIZE"),
                                new SimpleGrantedAuthority("DISCOUNT_POLICY_MANAGE"),
                                new SimpleGrantedAuthority("POS_SALE_DISCOUNT"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
