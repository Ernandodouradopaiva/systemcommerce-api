package br.com.systemcommerce.pos.receipt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cash.dto.CashSessionCloseRequest;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.repository.CashSessionRepository;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.checkout.dto.PosPaymentAddRequest;
import br.com.systemcommerce.pos.checkout.service.PosCheckoutService;
import br.com.systemcommerce.pos.receipt.dto.ReceiptPrintRequest;
import br.com.systemcommerce.pos.receipt.dto.ReceiptReprintRequest;
import br.com.systemcommerce.pos.receipt.entity.ReceiptPrintLog;
import br.com.systemcommerce.pos.receipt.service.PosReceiptService;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.service.PosSaleService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
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
class PosReceiptModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_receipt_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosReceiptService posReceiptService;

    @Autowired
    private PosCheckoutService posCheckoutService;

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

        withReceiptSecurity(() -> {
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
                    new CashSessionOpenRequest(terminalId, new BigDecimal("150.00"), null),
                    "receipt-open-" + UUID.randomUUID());
            cashSessionId = session.id();
        });

        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void shouldReturnOfficialSaleReceiptWithStoreAndNonFiscalFlag() {
        withReceiptSecurity(() -> {
            var saleId = finalizeSale(new BigDecimal("50.00"));
            var receipt = posReceiptService.getReceipt(
                    ReceiptPrintLog.PrintType.SALE, saleId, null, null, null, null);

            assertThat(receipt.nonFiscal()).isTrue();
            assertThat(receipt.documentDisclaimer()).contains("FISCAL");
            assertThat(receipt.store()).isNotNull();
            assertThat(receipt.store().name()).isNotBlank();
            assertThat(receipt.terminal()).isNotNull();
            assertThat(receipt.operator()).isNotNull();
            assertThat(receipt.sale()).isNotNull();
            assertThat(receipt.items()).isNotEmpty();
            assertThat(receipt.totals().total()).isEqualByComparingTo("50.00");
            assertThat(receipt.footerMessage()).isNotBlank();
            assertThat(receipt.title()).doesNotContainIgnoringCase("fiscal");
        });
    }

    @Test
    void shouldRegisterPrintAndAuditedReprint() throws Exception {
        UUID saleId = withReceiptSecurity(() -> finalizeSale(new BigDecimal("30.00")));

        var printBody = new ReceiptPrintRequest(
                ReceiptPrintLog.PrintType.SALE,
                saleId,
                null,
                null,
                null,
                null,
                ReceiptPrintLog.PrintLayout.THERMAL_80,
                1,
                null);

        MvcResult printed = mockMvc.perform(post("/api/v1/pos/receipts/print")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(printBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.authenticationId").isNotEmpty())
                .andExpect(jsonPath("$.data.sequenceNo").isNumber())
                .andExpect(jsonPath("$.data.nonFiscal").value(true))
                .andReturn();

        UUID printLogId = UUID.fromString(objectMapper
                .readTree(printed.getResponse().getContentAsString())
                .path("data")
                .path("printLogId")
                .asText());

        var reprintBody = new ReceiptReprintRequest(
                printLogId,
                ReceiptPrintLog.PrintType.SALE,
                saleId,
                null,
                null,
                null,
                null,
                ReceiptPrintLog.PrintLayout.THERMAL_58,
                2,
                "Cliente solicitou 2a via",
                null);

        mockMvc.perform(post("/api/v1/pos/receipts/reprint")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reprintBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.reprint").value(true))
                .andExpect(jsonPath("$.data.copies").value(2))
                .andExpect(jsonPath("$.data.reprintReason").value("Cliente solicitou 2a via"))
                .andExpect(jsonPath("$.data.title").value(org.hamcrest.Matchers.containsString("via")));

        mockMvc.perform(get("/api/v1/pos/receipts/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("saleId", saleId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    void shouldRequireReasonOnReprint() {
        withReceiptSecurity(() -> {
            UUID saleId = finalizeSale(new BigDecimal("20.00"));
            var printed = posReceiptService.registerPrint(new ReceiptPrintRequest(
                    ReceiptPrintLog.PrintType.SALE,
                    saleId,
                    null,
                    null,
                    null,
                    null,
                    ReceiptPrintLog.PrintLayout.A4,
                    1,
                    null));

            assertThatThrownBy(() -> posReceiptService.registerReprint(new ReceiptReprintRequest(
                            printed.printLogId(),
                            ReceiptPrintLog.PrintType.SALE,
                            saleId,
                            null,
                            null,
                            null,
                            null,
                            ReceiptPrintLog.PrintLayout.A4,
                            1,
                            "   ",
                            null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Motivo");
        });
    }

    @Test
    void shouldBuildOpeningAndClosingReceipts() {
        withReceiptSecurity(() -> {
            var opening = posReceiptService.getReceipt(
                    ReceiptPrintLog.PrintType.OPENING, null, null, cashSessionId, null, null);
            assertThat(opening.type()).isEqualTo(ReceiptPrintLog.PrintType.OPENING);
            assertThat(opening.cashMovement()).isNotNull();
            assertThat(opening.cashMovement().amount()).isEqualByComparingTo("150.00");
            assertThat(opening.nonFiscal()).isTrue();

            var recon = cashSessionService.reconcile(cashSessionId);
            cashSessionService.close(
                    cashSessionId,
                    new CashSessionCloseRequest(recon.expectedCash(), "fecho teste"),
                    "close-receipt-" + UUID.randomUUID());

            var closing = posReceiptService.getReceipt(
                    ReceiptPrintLog.PrintType.SESSION_CLOSE, null, null, cashSessionId, null, null);
            assertThat(closing.sessionClose()).isNotNull();
            assertThat(closing.sessionClose().openingAmount()).isEqualByComparingTo("150.00");
            assertThat(closing.title()).containsIgnoringCase("fechamento");
        });
    }

    private UUID finalizeSale(BigDecimal price) {
        Product product = createProduct(price);
        seedStock(product.getId(), "10");
        var sale = posSaleService.start(new PosSaleStartRequest(cashSessionId), "start-" + UUID.randomUUID());
        posSaleService.addByProductId(
                sale.id(),
                new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, sale.version()),
                null);
        posCheckoutService.addPayment(
                sale.id(),
                new PosPaymentAddRequest(
                        Payment.PaymentMethod.PIX,
                        price,
                        null,
                        1,
                        "PIX-R-" + UUID.randomUUID(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        false),
                "pay-" + UUID.randomUUID());
        var finalized = posCheckoutService.finalizeSale(sale.id(), "fin-" + UUID.randomUUID());
        assertThat(finalized.printData()).isNotNull();
        assertThat(finalized.printData().store()).isNotNull();
        assertThat(finalized.printData().documentDisclaimer()).contains("FISCAL");
        return finalized.sale().id();
    }

    private Product createProduct(BigDecimal price) {
        Product product = new Product();
        product.setInternalCode("REC-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Produto receipt " + product.getSku());
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
                new InventoryEntryRequest(productId, warehouseId, new BigDecimal(qty), "seed receipt", false));
    }

    private void withReceiptSecurity(Runnable action) {
        withReceiptSecurity(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withReceiptSecurity(java.util.concurrent.Callable<T> action) {
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
                                    new SimpleGrantedAuthority("POS_PAYMENT_MANAGE"),
                                    new SimpleGrantedAuthority("POS_PAYMENT_REFUND"),
                                    new SimpleGrantedAuthority("POS_SALE_FINALIZE"),
                                    new SimpleGrantedAuthority("POS_RECEIPT_PRINT"),
                                    new SimpleGrantedAuthority("POS_RECEIPT_REPRINT"),
                                    new SimpleGrantedAuthority("SALE_CONFIRM"),
                                    new SimpleGrantedAuthority("PAYMENT_MANAGE"),
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
