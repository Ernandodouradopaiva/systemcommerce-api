package br.com.systemcommerce.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryAdjustmentRequest;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.dto.InventoryExitRequest;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.repository.InventoryAdjustmentReasonRepository;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
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
class InventoryModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_inventory_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryAdjustmentReasonRepository reasonRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private String adminToken;
    private UUID reasonId;
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
        adminToken = objectMapper
                .readTree(login.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();

        reasonId = reasonRepository.findByActiveTrueOrderByDescriptionAsc().stream()
                .findFirst()
                .orElseThrow()
                .getId();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void shouldListBalanceMovementsAndRegisterEntry() throws Exception {
        UUID productId = createProduct(false).getId();
        inventoryService.registerEntry(new InventoryEntryRequest(productId, null, new BigDecimal("100"), "seed", false));

        mockMvc.perform(get("/api/v1/inventory/products/{productId}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.physicalQuantity").value(100.0));

        mockMvc.perform(post("/api/v1/inventory/entries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"productId":"%s","quantity":5,"observation":"Compra"}
                                """
                                        .formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("ENTRY"))
                .andExpect(jsonPath("$.data.previousBalance").value(100.0))
                .andExpect(jsonPath("$.data.newBalance").value(105.0));

        mockMvc.perform(get("/api/v1/inventory/movements")
                        .param("productId", productId.toString())
                        .param("type", "ENTRY")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldRejectNegativeStockWhenNotAllowed() {
        UUID productId = createProduct(false).getId();
        inventoryService.registerEntry(new InventoryEntryRequest(productId, null, new BigDecimal("5"), "seed", false));

        assertThatThrownBy(() -> inventoryService.registerExit(
                        new InventoryExitRequest(productId, null, new BigDecimal("6"), "excesso")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negativo");
    }

    @Test
    void shouldAllowNegativeWhenProductPermits() {
        UUID productId = createProduct(true).getId();
        inventoryService.registerEntry(new InventoryEntryRequest(productId, null, new BigDecimal("5"), "seed", false));

        var movement = inventoryService.registerExit(
                new InventoryExitRequest(productId, null, new BigDecimal("7"), "permitido"));

        assertThat(movement.newBalance()).isEqualByComparingTo("-2");
        assertThat(movement.type()).isEqualTo(InventoryMovement.MovementType.EXIT);
    }

    @Test
    void shouldRequireReasonForAdjustmentAndReverseSale() {
        UUID productId = createProduct(false).getId();
        inventoryService.registerEntry(new InventoryEntryRequest(productId, null, new BigDecimal("20"), "seed", false));

        assertThatThrownBy(() -> inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                productId,
                null,
                BigDecimal.ONE,
                        InventoryMovement.MovementType.ADJUSTMENT_POSITIVE,
                        null,
                        UUID.randomUUID(),
                        null)))
                .isInstanceOf(BusinessRuleException.class);

        var positive = inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                productId,
                null,
                new BigDecimal("3"),
                InventoryMovement.MovementType.ADJUSTMENT_POSITIVE,
                null,
                reasonId,
                "inventário"));
        assertThat(positive.newBalance()).isEqualByComparingTo("23");

        UUID saleId = UUID.randomUUID();
        var sale = inventoryService.registerSale(productId, new BigDecimal("2"), saleId);
        var cancel = inventoryService.registerSaleCancel(productId, new BigDecimal("2"), saleId);

        assertThat(sale.type()).isEqualTo(InventoryMovement.MovementType.SALE);
        assertThat(cancel.type()).isEqualTo(InventoryMovement.MovementType.SALE_CANCEL);
        assertThat(cancel.newBalance()).isEqualByComparingTo(sale.previousBalance());
    }

    @Test
    void shouldProtectConcurrentExitsWithPessimisticLock() throws Exception {
        UUID productId = createProduct(false).getId();
        inventoryService.registerEntry(new InventoryEntryRequest(productId, null, new BigDecimal("10"), "seed", false));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable task = () -> {
            try {
                start.await();
                inventoryService.registerExit(new InventoryExitRequest(productId, null, new BigDecimal("7"), "race"));
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
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(1);
        mockMvc.perform(get("/api/v1/inventory/products/{productId}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.physicalQuantity").value(3.0));
    }

    @Test
    void shouldListBelowMinimum() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/below-minimum")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldRegisterNegativeAdjustmentAndCorrection() {
        UUID productId = createProduct(false).getId();
        inventoryService.registerEntry(new InventoryEntryRequest(productId, null, new BigDecimal("10"), "seed", false));

        var negative = inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                productId,
                null,
                new BigDecimal("2"),
                InventoryMovement.MovementType.ADJUSTMENT_NEGATIVE,
                null,
                reasonId,
                "perda"));
        assertThat(negative.newBalance()).isEqualByComparingTo("8");

        assertThatThrownBy(() -> inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                productId,
                null,
                BigDecimal.ONE,
                        InventoryMovement.MovementType.CORRECTION,
                        null,
                        reasonId,
                        "sem effect")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("effect");

        var correction = inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                productId,
                null,
                new BigDecimal("3"),
                InventoryMovement.MovementType.CORRECTION,
                InventoryAdjustmentRequest.StockEffect.INCREASE,
                reasonId,
                "correção"));
        assertThat(correction.newBalance()).isEqualByComparingTo("11");
    }

    @Test
    void shouldProtectConcurrentEntriesWithPessimisticLock() throws Exception {
        UUID productId = createProduct(false).getId();
        // Garante linha de inventário antes da disputa (evita corrida em createInventory)
        inventoryService.registerEntry(new InventoryEntryRequest(productId, null, BigDecimal.ONE, "seed", false));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable entryTask = () -> {
            try {
                start.await();
                inventoryService.registerEntry(
                        new InventoryEntryRequest(productId, null, new BigDecimal("5"), "concurrent", false));
                successes.incrementAndGet();
            } catch (Exception ex) {
                failures.incrementAndGet();
            } finally {
                done.countDown();
            }
        };
        executor.submit(entryTask);
        executor.submit(entryTask);
        start.countDown();
        assertThat(done.await(40, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(successes.get()).isGreaterThanOrEqualTo(1);
        assertThat(failures.get()).isLessThanOrEqualTo(1);
        BigDecimal expectedQty = BigDecimal.ONE.add(new BigDecimal("5").multiply(BigDecimal.valueOf(successes.get())));
        mockMvc.perform(get("/api/v1/inventory/products/{productId}", productId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.physicalQuantity").value(expectedQty.doubleValue()));
    }

    private Product createProduct(boolean allowNegative) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("INV-" + suffix);
        product.setSku("SKU-" + suffix);
        product.setName("Produto estoque " + suffix);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(BigDecimal.TEN);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(new BigDecimal("2"));
        product.setAllowNegativeStock(allowNegative);
        product.markActive();
        return productRepository.saveAndFlush(product);
    }
}
