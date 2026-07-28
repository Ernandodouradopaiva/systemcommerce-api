package br.com.systemcommerce.sale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.sale.dto.SaleCancelRequest;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleCustomerRequest;
import br.com.systemcommerce.sale.dto.SaleDiscountRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.service.SaleService;
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
class SaleModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_sale_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaleService saleService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private StoreProductService storeProductService;

    private String adminToken;
    private UUID adminUserId;
    private UUID customerId;
    private UUID loja01Id;
    private UUID dep01Id;
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
        var loginJson = objectMapper.readTree(login.getResponse().getContentAsString()).path("data");
        adminToken = loginJson.path("accessToken").asText();
        adminUserId = UUID.fromString(loginJson.path("user").path("id").asText());

        authenticateAs(adminUserId);

        Customer maria = customerRepository.findByDocument("52998224725").orElseThrow();
        maria.markActive();
        customerRepository.saveAndFlush(maria);
        customerId = maria.getId();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
        loja01Id = findStoreId("LOJA-01");
        dep01Id = findWarehouseId(loja01Id, "DEP-01");
    }

    @Test
    void shouldCreateValidSaleConfirmAndList() throws Exception {
        Product product = createProduct(false, new BigDecimal("50"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));

        MvcResult created = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"storeId":"%s","warehouseId":"%s","customerId":"%s","notes":"Pedido teste"}
                                """
                                        .formatted(loja01Id, dep01Id, customerId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.saleNumber").isNotEmpty())
                .andReturn();

        UUID saleId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        mockMvc.perform(post("/api/v1/sales/{id}/items", saleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"productId":"%s","quantity":2,"unitPrice":25.00,"discountAmount":0}
                                """
                                        .formatted(product.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal").value(50.0))
                .andExpect(jsonPath("$.data.totalAmount").value(50.0));

        mockMvc.perform(post("/api/v1/sales/{id}/confirm", saleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        // MockMvc limpa o SecurityContext ao final da requisição
        authenticateAs(adminUserId);
        assertThat(inventoryService.getBalance(product.getId()).physicalQuantity()).isEqualByComparingTo("8");

        mockMvc.perform(get("/api/v1/sales/{id}/status-history", saleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    void shouldRejectInactiveCustomerAndProduct() {
        Customer dedicated = new Customer();
        dedicated.setType(Customer.CustomerType.PF);
        dedicated.setName("Cliente Inativo Teste");
        dedicated.setDocument("15350946056");
        dedicated.markActive();
        dedicated = customerRepository.saveAndFlush(dedicated);

        dedicated.markInactive();
        customerRepository.saveAndFlush(dedicated);

        UUID inactiveId = dedicated.getId();
        assertThatThrownBy(() -> saleService.createDraft(draftRequest(inactiveId, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");

        Product inactive = createProduct(false, new BigDecimal("10"));
        inactive.markInactive();
        productRepository.saveAndFlush(inactive);

        SaleResponse draft = saleService.createDraft(draftRequest(customerId, null));
        assertThatThrownBy(() -> saleService.addItem(
                        draft.id(),
                        new SaleItemRequest(inactive.getId(), BigDecimal.ONE, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");
    }

    @Test
    void shouldRejectInsufficientStockAndAllowNegativeWhenConfigured() {
        Product limited = createProduct(false, new BigDecimal("5"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(limited.getId(), null, new BigDecimal("2"), "seed", false));

        SaleResponse sale = prepareSale(limited.getId(), new BigDecimal("3"));
        assertThatThrownBy(() -> saleService.confirm(sale.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(saleService.getById(sale.id()).status()).isEqualTo(Sale.SaleStatus.DRAFT);

        Product flexible = createProduct(true, new BigDecimal("5"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(flexible.getId(), null, new BigDecimal("1"), "seed", false));
        SaleResponse sale2 = prepareSale(flexible.getId(), new BigDecimal("3"));
        SaleResponse confirmed = saleService.confirm(sale2.id());
        assertThat(confirmed.status()).isEqualTo(Sale.SaleStatus.CONFIRMED);
        assertThat(inventoryService.getBalance(flexible.getId()).physicalQuantity()).isEqualByComparingTo("-2");
    }

    @Test
    void shouldRejectInvalidDiscount() {
        Product product = createProduct(false, new BigDecimal("100"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));
        SaleResponse sale = prepareSale(product.getId(), BigDecimal.ONE);

        assertThatThrownBy(() -> saleService.applyDiscount(sale.id(), new SaleDiscountRequest(new BigDecimal("999"))))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void shouldBeIdempotentOnConfirmAndCancelRestoringStock() {
        Product product = createProduct(false, new BigDecimal("20"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));
        SaleResponse sale = prepareSale(product.getId(), new BigDecimal("4"));

        SaleResponse first = saleService.confirm(sale.id());
        SaleResponse second = saleService.confirm(sale.id());
        assertThat(first.status()).isEqualTo(Sale.SaleStatus.CONFIRMED);
        assertThat(second.status()).isEqualTo(Sale.SaleStatus.CONFIRMED);
        assertThat(inventoryService.getBalance(product.getId()).physicalQuantity()).isEqualByComparingTo("6");

        SaleResponse cancelled = saleService.cancel(sale.id(), new SaleCancelRequest("Cliente desistiu"));
        SaleResponse cancelledAgain = saleService.cancel(sale.id(), new SaleCancelRequest("Ignorado"));
        assertThat(cancelled.status()).isEqualTo(Sale.SaleStatus.CANCELLED);
        assertThat(cancelledAgain.status()).isEqualTo(Sale.SaleStatus.CANCELLED);
        assertThat(inventoryService.getBalance(product.getId()).physicalQuantity()).isEqualByComparingTo("10");
    }

    @Test
    void shouldProtectConcurrentConfirmsAgainstOverSell() throws Exception {
        Product product = createProduct(false, new BigDecimal("10"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("5"), "seed", false));

        SaleResponse saleA = prepareSale(product.getId(), new BigDecimal("4"));
        SaleResponse saleB = prepareSale(product.getId(), new BigDecimal("4"));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Runnable taskA = () -> runConfirm(saleA.id(), start, done, successes, failures);
        Runnable taskB = () -> runConfirm(saleB.id(), start, done, successes, failures);
        executor.submit(taskA);
        executor.submit(taskB);
        start.countDown();
        assertThat(done.await(40, TimeUnit.SECONDS)).isTrue();
        executor.shutdownNow();

        assertThat(successes.get()).isEqualTo(1);
        assertThat(failures.get()).isEqualTo(1);
        assertThat(inventoryService.getBalance(product.getId()).physicalQuantity()).isEqualByComparingTo("1");
    }

    @Test
    void shouldBlockItemChangeAfterConfirm() {
        Product product = createProduct(false, new BigDecimal("10"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));
        SaleResponse sale = prepareSale(product.getId(), BigDecimal.ONE);
        saleService.confirm(sale.id());

        assertThatThrownBy(() -> saleService.addItem(
                        sale.id(),
                        new SaleItemRequest(product.getId(), BigDecimal.ONE, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("status atual");
    }

    @Test
    void shouldRejectConfirmWithoutItemsOrCustomer() {
        SaleResponse empty = saleService.createDraft(draftRequest(null, null));
        assertThatThrownBy(() -> saleService.confirm(empty.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cliente");

        saleService.setCustomer(empty.id(), new SaleCustomerRequest(customerId));
        assertThatThrownBy(() -> saleService.confirm(empty.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("item");
    }

    @Test
    void shouldCancelDraftWithoutTouchingStock() {
        Product product = createProduct(false, new BigDecimal("10"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("8"), "seed", false));
        SaleResponse draft = prepareSale(product.getId(), BigDecimal.ONE);

        SaleResponse cancelled = saleService.cancel(draft.id(), new SaleCancelRequest("Desistência no rascunho"));
        assertThat(cancelled.status()).isEqualTo(Sale.SaleStatus.CANCELLED);
        assertThat(inventoryService.getBalance(product.getId()).physicalQuantity()).isEqualByComparingTo("8");
    }

    @Test
    void shouldUpdateAndRemoveItemOnDraftViaHttp() throws Exception {
        Product product = createProduct(false, new BigDecimal("25"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("10"), "seed", false));

        MvcResult created = mockMvc.perform(post("/api/v1/sales")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"storeId":"%s","warehouseId":"%s","customerId":"%s"}
                                """
                                        .formatted(loja01Id, dep01Id, customerId)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID saleId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        MvcResult withItem = mockMvc.perform(post("/api/v1/sales/{id}/items", saleId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"productId":"%s","quantity":2,"unitPrice":25.00,"discountAmount":0}
                                """
                                        .formatted(product.getId())))
                .andExpect(status().isCreated())
                .andReturn();
        UUID itemId = UUID.fromString(objectMapper
                .readTree(withItem.getResponse().getContentAsString())
                .path("data")
                .path("items")
                .get(0)
                .path("id")
                .asText());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/v1/sales/{id}/items/{itemId}", saleId, itemId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"productId":"%s","quantity":1,"unitPrice":25.00,"discountAmount":0}
                                """
                                        .formatted(product.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAmount").value(25.0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                                "/api/v1/sales/{id}/items/{itemId}", saleId, itemId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    private SaleCreateRequest draftRequest(UUID customerId, String notes) {
        return new SaleCreateRequest(loja01Id, dep01Id, customerId, null, null, notes);
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

    private void runConfirm(
            UUID saleId,
            CountDownLatch start,
            CountDownLatch done,
            AtomicInteger successes,
            AtomicInteger failures) {
        try {
            start.await();
            authenticateAs(adminUserId);
            saleService.confirm(saleId);
            successes.incrementAndGet();
        } catch (Exception ex) {
            failures.incrementAndGet();
        } finally {
            SecurityContextHolder.clearContext();
            done.countDown();
        }
    }

    private SaleResponse prepareSale(UUID productId, BigDecimal quantity) {
        SaleResponse draft = saleService.createDraft(draftRequest(customerId, null));
        if (draft.customerId() == null) {
            saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        }
        return saleService.addItem(
                draft.id(), new SaleItemRequest(productId, quantity, null, BigDecimal.ZERO, null));
    }

    private Product createProduct(boolean allowNegative, BigDecimal salePrice) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("SALE-" + suffix);
        product.setSku("SKU-SALE-" + suffix);
        product.setName("Produto venda " + suffix);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(salePrice);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(allowNegative);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(loja01Id, saved.getId()));
        return saved;
    }

    private void authenticateAs(UUID userId) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                java.util.List.of(
                        new SimpleGrantedAuthority("SALE_CREATE"),
                        new SimpleGrantedAuthority("SALE_CONFIRM"),
                        new SimpleGrantedAuthority("SALE_CANCEL"),
                        new SimpleGrantedAuthority("SALE_READ"),
                        new SimpleGrantedAuthority("INVENTORY_MOVE"),
                        new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
