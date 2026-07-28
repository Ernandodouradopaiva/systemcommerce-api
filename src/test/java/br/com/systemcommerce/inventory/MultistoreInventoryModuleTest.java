package br.com.systemcommerce.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryAdjustmentRequest;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.repository.InventoryAdjustmentReasonRepository;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.repository.ProductRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MultistoreInventoryModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_multistore_inventory_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryAdjustmentReasonRepository reasonRepository;

    private static final UUID TEST_ADMIN_ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private UUID loja01Id;
    private UUID loja02Id;
    private UUID dep01Id;
    private UUID dep02Id;
    private UUID productId;
    private UUID reasonId;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk());

        // Configura SecurityContext para chamadas diretas ao serviço (getBalance, etc.)
        var auth = new UsernamePasswordAuthenticationToken(
                TEST_ADMIN_ID.toString(),
                null,
                List.of(
                        new SimpleGrantedAuthority("INVENTORY_READ"),
                        new SimpleGrantedAuthority("INVENTORY_MOVE"),
                        new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        loja01Id = findStoreId("LOJA-01");
        loja02Id = findStoreId("LOJA-02");
        dep01Id = findWarehouseId(loja01Id, "DEP-01");
        dep02Id = findWarehouseId(loja02Id, "DEP-02");
        productId = productRepository.findBySkuIgnoreCase("NB-001").orElseThrow().getId();
        reasonId = reasonRepository.findByActiveTrueOrderByDescriptionAsc().stream()
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sameProductHasIndependentBalancesPerStore() {
        var bal01 = inventoryService.getBalance(productId, dep01Id);
        var bal02 = inventoryService.getBalance(productId, dep02Id);

        assertThat(bal01.storeId()).isEqualTo(loja01Id);
        assertThat(bal02.storeId()).isEqualTo(loja02Id);
        assertThat(bal01.physicalQuantity()).isNotEqualByComparingTo(bal02.physicalQuantity());
        assertThat(bal01.availableQuantity())
                .isEqualByComparingTo(bal01.physicalQuantity()
                        .subtract(bal01.reservedQuantity())
                        .subtract(bal01.blockedQuantity()));
    }

    @Test
    void saleInOneWarehouseDoesNotChangeOtherStore() {
        BigDecimal before01 = inventoryService.getBalance(productId, dep01Id).physicalQuantity();
        BigDecimal before02 = inventoryService.getBalance(productId, dep02Id).physicalQuantity();
        UUID saleId = UUID.randomUUID();

        inventoryService.registerSale(productId, dep01Id, new BigDecimal("2"), saleId);

        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(before01.subtract(new BigDecimal("2")));
        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(before02);

        inventoryService.registerSaleCancel(productId, dep01Id, new BigDecimal("2"), saleId);

        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(before01);
        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(before02);
    }

    @Test
    void adjustmentIsPerWarehouse() {
        BigDecimal before02 = inventoryService.getBalance(productId, dep02Id).physicalQuantity();
        BigDecimal before01 = inventoryService.getBalance(productId, dep01Id).physicalQuantity();

        inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                productId,
                dep02Id,
                new BigDecimal("3"),
                InventoryMovement.MovementType.ADJUSTMENT_POSITIVE,
                null,
                reasonId,
                "ajuste filial"));

        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(before02.add(new BigDecimal("3")));
        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(before01);
    }

    @Test
    void consolidatedBalanceSumsWarehouses() {
        var c = inventoryService.getConsolidatedBalance(productId);
        BigDecimal expected = inventoryService
                .getBalance(productId, dep01Id)
                .physicalQuantity()
                .add(inventoryService.getBalance(productId, dep02Id).physicalQuantity());
        assertThat(c.physicalQuantity()).isEqualByComparingTo(expected);
        assertThat(c.balancesByWarehouse()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void cannotAffectOtherStoreWhenOperatingOnWrongWarehouse() {
        BigDecimal before01 = inventoryService.getBalance(productId, dep01Id).physicalQuantity();
        inventoryService.registerEntry(
                new InventoryEntryRequest(productId, dep02Id, new BigDecimal("1"), "entrada filial", false));
        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(before01);
        assertThat(inventoryService.availableQuantity(productId, dep01Id))
                .isEqualByComparingTo(before01);
    }

    @Test
    void concurrentAdjustmentsOnSameWarehouseAreSerialized() throws Exception {
        inventoryService.registerEntry(
                new InventoryEntryRequest(productId, dep01Id, new BigDecimal("20"), "base concorrencia", false));
        BigDecimal before = inventoryService.getBalance(productId, dep01Id).physicalQuantity();

        int threads = 4;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger successes = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                            productId,
                            dep01Id,
                            new BigDecimal("1"),
                            InventoryMovement.MovementType.ADJUSTMENT_NEGATIVE,
                            null,
                            reasonId,
                            "conc"));
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // conflito ocasional ainda deve deixar saldo consistente
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();
        BigDecimal after = inventoryService.getBalance(productId, dep01Id).physicalQuantity();
        assertThat(successes.get()).isGreaterThan(0);
        assertThat(after).isEqualByComparingTo(before.subtract(BigDecimal.valueOf(successes.get())));
    }

    @Test
    void listByStoreFiltersCorrectly() {
        var page = inventoryService.list(productId, loja02Id, null, null, null, Pageable.unpaged());
        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allMatch(b -> loja02Id.equals(b.storeId()));
    }

    private UUID findStoreId(String code) {
        return storeService
                .list(null, code, null, null, null, null, null, null, Pageable.unpaged())
                .stream()
                .findFirst()
                .orElseThrow()
                .id();
    }

    private UUID findWarehouseId(UUID storeId, String code) {
        return warehouseService.list(storeId, null, null, null, Pageable.unpaged()).stream()
                .filter(w -> code.equals(w.code()))
                .findFirst()
                .orElseThrow()
                .id();
    }
}
