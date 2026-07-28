package br.com.systemcommerce.stockentry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.stockentry.dto.StockEntryCreateRequest;
import br.com.systemcommerce.stockentry.dto.StockEntryItemCreateRequest;
import br.com.systemcommerce.stockentry.service.StockEntryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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

import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class StockEntryModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_stock_entry_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockEntryService stockEntryService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ProductRepository productRepository;

    private String adminToken;
    private UUID orgId;
    private UUID loja01Id;
    private UUID loja02Id;
    private UUID dep01Id;
    private UUID dep02Id;
    private UUID productId;

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

        orgId = organizationService.getDefault().id();
        loja01Id = findStoreId("LOJA-01");
        loja02Id = findStoreId("LOJA-02");
        dep01Id = findWarehouseId(loja01Id, "DEP-01");
        dep02Id = findWarehouseId(loja02Id, "DEP-02");
        productId = productRepository.findBySkuIgnoreCase("NB-001").orElseThrow().getId();

        var auth = new UsernamePasswordAuthenticationToken(
                "a0000000-0000-4000-8000-000000000001",
                null,
                List.of(
                        new SimpleGrantedAuthority("INVENTORY_READ"),
                        new SimpleGrantedAuthority("INVENTORY_MOVE"),
                        new SimpleGrantedAuthority("STOCK_ENTRY_MANAGE"),
                        new SimpleGrantedAuthority("STOCK_ENTRY_READ"),
                        new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldConfirmEntriesIndependentlyPerStoreWarehouse() {
        BigDecimal qty = new BigDecimal("7.000");
        BigDecimal loja01Before = inventoryService.getBalance(productId, dep01Id).physicalQuantity();
        BigDecimal loja02Before = inventoryService.getBalance(productId, dep02Id).physicalQuantity();

        UUID entry01 = createAndConfirmEntry(loja01Id, dep01Id, qty, "NF-LOJA01-001");
        UUID entry02 = createAndConfirmEntry(loja02Id, dep02Id, qty, "NF-LOJA02-001");

        assertThat(entry01).isNotNull();
        assertThat(entry02).isNotNull();
        assertThat(stockEntryService.getById(entry01).status().name()).isEqualTo("CONFIRMED");
        assertThat(stockEntryService.getById(entry02).status().name()).isEqualTo("CONFIRMED");

        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(loja01Before.add(qty));
        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(loja02Before.add(qty));
    }

    @Test
    void shouldExposeStockEntryFlowViaHttp() throws Exception {
        mockMvc.perform(post("/api/v1/stock-entries")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockEntryCreateRequest(
                                orgId,
                                loja01Id,
                                dep01Id,
                                "Fornecedor HTTP",
                                "NF-HTTP-1",
                                LocalDate.now(),
                                "entrada teste"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    private UUID createAndConfirmEntry(UUID storeId, UUID warehouseId, BigDecimal quantity, String documentNumber) {
        UUID entryId = stockEntryService
                .create(new StockEntryCreateRequest(
                        orgId,
                        storeId,
                        warehouseId,
                        "Fornecedor Teste",
                        documentNumber,
                        LocalDate.now(),
                        "teste módulo"))
                .id();
        stockEntryService.addItem(
                entryId,
                new StockEntryItemCreateRequest(productId, quantity, new BigDecimal("100.0000"), null));
        return stockEntryService.confirm(entryId).id();
    }

    private UUID findStoreId(String code) {
        return storeService
                .list(null, code, null, null, null, null, null, null, Pageable.unpaged())
                .getContent()
                .getFirst()
                .id();
    }

    private UUID findWarehouseId(UUID storeId, String code) {
        return warehouseService.list(storeId, null, null, code, Pageable.unpaged()).getContent().stream()
                .filter(w -> w.code().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow()
                .id();
    }
}
