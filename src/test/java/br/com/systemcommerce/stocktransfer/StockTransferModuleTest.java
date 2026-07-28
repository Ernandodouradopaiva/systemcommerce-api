package br.com.systemcommerce.stocktransfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryAdjustmentRequest;
import br.com.systemcommerce.inventory.entity.InventoryMovement;
import br.com.systemcommerce.inventory.repository.InventoryAdjustmentReasonRepository;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.stocktransfer.dto.StockTransferCreateRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferItemCreateRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferReceiveRequest;
import br.com.systemcommerce.stocktransfer.service.StockTransferService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
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
class StockTransferModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_stock_transfer_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockTransferService stockTransferService;

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

    @Autowired
    private InventoryAdjustmentReasonRepository reasonRepository;

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
                        new SimpleGrantedAuthority("STOCK_TRANSFER_CREATE"),
                        new SimpleGrantedAuthority("STOCK_TRANSFER_READ"),
                        new SimpleGrantedAuthority("STOCK_TRANSFER_DISPATCH"),
                        new SimpleGrantedAuthority("STOCK_TRANSFER_RECEIVE"),
                        new SimpleGrantedAuthority("STOCK_TRANSFER_CANCEL"),
                        new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCompleteTransferFlowUpdatingOriginAndDestinationBalances() {
        BigDecimal originBefore = inventoryService.getBalance(productId, dep01Id).physicalQuantity();
        BigDecimal destBefore = inventoryService.getBalance(productId, dep02Id).physicalQuantity();
        BigDecimal transferQty = new BigDecimal("10.000");

        UUID transferId = createAndDispatchTransfer(transferQty);

        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(originBefore.subtract(transferQty));
        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(destBefore);

        stockTransferService.receive(transferId, new StockTransferReceiveRequest(List.of(), null, "recv-full-1"));

        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(destBefore.add(transferQty));
    }

    @Test
    void shouldSupportPartialReceive() {
        BigDecimal originBefore = inventoryService.getBalance(productId, dep01Id).physicalQuantity();
        BigDecimal destBefore = inventoryService.getBalance(productId, dep02Id).physicalQuantity();
        BigDecimal transferQty = new BigDecimal("8.000");
        BigDecimal firstReceive = new BigDecimal("3.000");
        BigDecimal secondReceive = new BigDecimal("5.000");

        UUID transferId = createAndDispatchTransfer(transferQty);
        UUID itemId = stockTransferService.getById(transferId).items().getFirst().id();

        stockTransferService.receivePartial(
                transferId,
                new StockTransferReceiveRequest(
                        List.of(new StockTransferReceiveRequest.ReceiveLine(itemId, firstReceive)),
                        "parcial 1",
                        "recv-partial-1"));

        var partial = stockTransferService.getById(transferId);
        assertThat(partial.status().name()).isEqualTo("PARTIALLY_RECEIVED");
        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(destBefore.add(firstReceive));

        stockTransferService.receivePartial(
                transferId,
                new StockTransferReceiveRequest(
                        List.of(new StockTransferReceiveRequest.ReceiveLine(itemId, secondReceive)),
                        "parcial 2",
                        "recv-partial-2"));

        var completed = stockTransferService.getById(transferId);
        assertThat(completed.status().name()).isEqualTo("RECEIVED");
        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(originBefore.subtract(transferQty));
        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(destBefore.add(transferQty));
    }

    @Test
    void shouldRejectSameWarehouseTransfer() {
        assertThatThrownBy(() -> stockTransferService.create(new StockTransferCreateRequest(
                        orgId, loja01Id, dep01Id, loja01Id, dep01Id, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("diferentes");
    }

    @Test
    void shouldNotReplaceTransferWithInventoryAdjustmentBetweenWarehouses() {
        BigDecimal originBefore = inventoryService.getBalance(productId, dep01Id).physicalQuantity();
        BigDecimal destBefore = inventoryService.getBalance(productId, dep02Id).physicalQuantity();
        UUID reasonId = reasonRepository.findByActiveTrueOrderByDescriptionAsc().stream()
                .findFirst()
                .orElseThrow()
                .getId();

        inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                productId,
                dep01Id,
                new BigDecimal("5.000"),
                InventoryMovement.MovementType.ADJUSTMENT_NEGATIVE,
                null,
                reasonId,
                "Tentativa indevida"));
        inventoryService.registerAdjustment(new InventoryAdjustmentRequest(
                productId,
                dep02Id,
                new BigDecimal("5.000"),
                InventoryMovement.MovementType.ADJUSTMENT_POSITIVE,
                null,
                reasonId,
                "Tentativa indevida"));

        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(originBefore.subtract(new BigDecimal("5.000")));
        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(destBefore.add(new BigDecimal("5.000")));

        UUID transferId = createAndDispatchTransfer(new BigDecimal("4.000"));
        stockTransferService.receive(transferId, new StockTransferReceiveRequest(List.of(), null, "adj-test-receive"));

        assertThat(inventoryService.getBalance(productId, dep01Id).physicalQuantity())
                .isEqualByComparingTo(originBefore.subtract(new BigDecimal("9.000")));
        assertThat(inventoryService.getBalance(productId, dep02Id).physicalQuantity())
                .isEqualByComparingTo(destBefore.add(new BigDecimal("9.000")));
    }

    @Test
    void shouldExposeTransferFlowViaHttp() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/v1/stock-transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new StockTransferCreateRequest(
                                orgId, loja01Id, dep01Id, loja02Id, dep02Id, "obs", "reposição", "http-create-1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andReturn();

        UUID transferId = UUID.fromString(readData(created).path("id").asText());

        mockMvc.perform(post("/api/v1/stock-transfers/{id}/items", transferId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new StockTransferItemCreateRequest(productId, new BigDecimal("2.000"), null))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/stock-transfers/{id}/request", transferId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REQUESTED"));

        mockMvc.perform(post("/api/v1/stock-transfers/{id}/approve", transferId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(post("/api/v1/stock-transfers/{id}/prepare", transferId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARING"));

        mockMvc.perform(post("/api/v1/stock-transfers/{id}/dispatch", transferId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("IN_TRANSIT"));
    }

    private UUID createAndDispatchTransfer(BigDecimal quantity) {
        UUID transferId = stockTransferService
                .create(new StockTransferCreateRequest(
                        orgId,
                        loja01Id,
                        dep01Id,
                        loja02Id,
                        dep02Id,
                        null,
                        "teste",
                        "idem-" + UUID.randomUUID()))
                .id();
        stockTransferService.addItem(
                transferId, new StockTransferItemCreateRequest(productId, quantity, null));
        stockTransferService.request(transferId, null);
        stockTransferService.approve(transferId, null);
        stockTransferService.prepare(transferId, null);
        stockTransferService.dispatch(transferId, null);
        return transferId;
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

    private JsonNode readData(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }
}
