package br.com.systemcommerce.pos.resilience;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.payment.entity.Payment;
import br.com.systemcommerce.pos.cash.dto.CashSessionOpenRequest;
import br.com.systemcommerce.pos.cash.service.CashSessionService;
import br.com.systemcommerce.pos.checkout.dto.PosPaymentAddRequest;
import br.com.systemcommerce.pos.checkout.service.PosCheckoutService;
import br.com.systemcommerce.pos.resilience.service.PosOperationLookupService;
import br.com.systemcommerce.pos.sale.dto.PosSaleAddByProductIdRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleDiscardRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleQuantityRequest;
import br.com.systemcommerce.pos.sale.dto.PosSaleStartRequest;
import br.com.systemcommerce.pos.sale.service.PosSaleService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.shared.exception.ConflictException;
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

/**
 * Resiliência PDV: consulta por Idempotency-Key, reenvio seguro, versão e ausência de duplicidade.
 * Não cobre venda offline definitiva.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PosResilienceModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_resilience_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosSaleService posSaleService;

    @Autowired
    private PosCheckoutService posCheckoutService;

    @Autowired
    private PosOperationLookupService lookupService;

    @Autowired
    private CashSessionService cashSessionService;

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
    private UUID sessionId;
    private UUID storeId;
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
        var data = objectMapper.readTree(login.getResponse().getContentAsString()).path("data");
        adminToken = data.path("accessToken").asText();
        adminUserId = UUID.fromString(data.path("user").path("id").asText());

        withSecurity(() -> {
            var available = posTerminalService.listAvailable(null, Pageable.unpaged()).getContent().stream()
                    .filter(t -> "TERM-01".equals(t.code()))
                    .findFirst()
                    .orElseGet(() -> posTerminalService.listAvailable(null, Pageable.unpaged()).getContent().getFirst());
            terminalId = available.id();
            storeId = available.storeId();
            try {
                sessionId = cashSessionService.getCurrent(terminalId).id();
            } catch (Exception ex) {
                var open = cashSessionService.open(
                        new CashSessionOpenRequest(terminalId, new BigDecimal("100.00"), "resilience"),
                        "resilience-open-" + UUID.randomUUID());
                sessionId = open.id();
            }
            productId = seedProduct().getId();
        });
    }

    @Test
    void shouldRecoverAfterItemAddUsingIdempotencyLookup() {
        withSecurity(() -> {
            discardCurrentDraftIfAny();
            var sale = posSaleService.start(new PosSaleStartRequest(sessionId), "start-" + UUID.randomUUID());
            String itemKey = "item-add-" + UUID.randomUUID();
            var afterAdd = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(productId, BigDecimal.ONE, sale.version()),
                    itemKey);

            // Queda após inclusão: resposta perdida — consulta oficial pela key
            var lookup = lookupService.lookup(itemKey);
            assertThat(lookup.found()).isTrue();
            assertThat(lookup.saleId()).isEqualTo(afterAdd.id());
            assertThat(lookup.saleVersion()).isEqualTo(afterAdd.version());
            assertThat(lookup.operationType()).isEqualTo("SALE_MUTATION");

            // Reenvio seguro: mesma key não duplica item
            var replay = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(productId, BigDecimal.ONE, afterAdd.version()),
                    itemKey);
            assertThat(replay.items()).hasSize(afterAdd.items().size());
            assertThat(replay.version()).isEqualTo(afterAdd.version());
        });
    }

    @Test
    void shouldRecoverPaymentAfterDropUsingIdempotencyLookup() {
        withSecurity(() -> {
            var sale = prepareSaleWithItem();
            String payKey = "pay-" + UUID.randomUUID();
            var payment = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CASH,
                            sale.totalAmount(),
                            sale.totalAmount(),
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    payKey);

            var lookup = lookupService.lookup(payKey);
            assertThat(lookup.found()).isTrue();
            assertThat(lookup.operationType()).isEqualTo("PAYMENT");
            assertThat(lookup.paymentId()).isEqualTo(payment.id());
            assertThat(lookup.paymentStatus()).isIn(Payment.PaymentStatus.PENDING, Payment.PaymentStatus.CONFIRMED);

            // Reenvio seguro — mesmo paymentId
            var replay = posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.CASH,
                            sale.totalAmount(),
                            sale.totalAmount(),
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    payKey);
            assertThat(replay.id()).isEqualTo(payment.id());
            assertThat(posCheckoutService.listPayments(sale.id())).hasSize(1);
        });
    }

    @Test
    void shouldRecoverLostFinalizeResponseAndAvoidDuplicate() {
        withSecurity(() -> {
            Product product = seedProduct();
            discardCurrentDraftIfAny();
            var sale = posSaleService.start(new PosSaleStartRequest(sessionId), "fin-start-" + UUID.randomUUID());
            sale = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, sale.version()),
                    "fin-item-" + UUID.randomUUID());
            BigDecimal total = sale.totalAmount();
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX, total, null, 1, null, null, null, null, null, null, false),
                    "pay-fin-" + UUID.randomUUID());

            String finalizeKey = "finalize-" + UUID.randomUUID();
            var first = posCheckoutService.finalizeSale(sale.id(), finalizeKey);
            assertThat(first.status()).isEqualTo(Sale.SaleStatus.PAID);

            var lookup = lookupService.lookup(finalizeKey);
            assertThat(lookup.found()).isTrue();
            assertThat(lookup.saleStatus()).isEqualTo(Sale.SaleStatus.PAID);

            var replay = posCheckoutService.finalizeSale(sale.id(), finalizeKey);
            assertThat(replay.status()).isEqualTo(Sale.SaleStatus.PAID);
            assertThat(replay.sale().id()).isEqualTo(first.sale().id());
        });
    }

    @Test
    void shouldReportAlreadyCompletedOperation() {
        withSecurity(() -> {
            Product product = seedProduct();
            discardCurrentDraftIfAny();
            var sale = posSaleService.start(new PosSaleStartRequest(sessionId), "done-start-" + UUID.randomUUID());
            sale = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(product.getId(), BigDecimal.ONE, sale.version()),
                    "done-item-" + UUID.randomUUID());
            String payKey = "completed-pay-" + UUID.randomUUID();
            posCheckoutService.addPayment(
                    sale.id(),
                    new PosPaymentAddRequest(
                            Payment.PaymentMethod.PIX,
                            sale.totalAmount(),
                            null,
                            1,
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            false),
                    payKey);
            posCheckoutService.finalizeSale(sale.id(), "fin-completed-" + UUID.randomUUID());

            var lookup = lookupService.lookup(payKey);
            assertThat(lookup.found()).isTrue();
            assertThat(lookup.outcome()).isEqualTo("COMPLETED");
            assertThat(lookup.paymentStatus()).isEqualTo(Payment.PaymentStatus.CONFIRMED);
        });
    }

    @Test
    void shouldDetectVersionDivergenceOnStaleMutation() {
        withSecurity(() -> {
            var sale = posSaleService.start(new PosSaleStartRequest(sessionId), "v-" + UUID.randomUUID());
            var item = posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(productId, BigDecimal.ONE, sale.version()),
                    "v-item-" + UUID.randomUUID());

            Long staleVersion = sale.version();
            assertThatThrownBy(() -> posSaleService.updateQuantity(
                            item.id(),
                            item.items().getFirst().id(),
                            new PosSaleQuantityRequest(new BigDecimal("2"), staleVersion),
                            "v-qty-" + UUID.randomUUID()))
                    .isInstanceOf(ConflictException.class);
        });
    }

    @Test
    void shouldExposeLookupViaHttpApi() throws Exception {
        String itemKey = "http-item-" + UUID.randomUUID();
        withSecurity(() -> {
            var sale = posSaleService.start(new PosSaleStartRequest(sessionId), "http-start-" + UUID.randomUUID());
            posSaleService.addByProductId(
                    sale.id(),
                    new PosSaleAddByProductIdRequest(productId, BigDecimal.ONE, sale.version()),
                    itemKey);
        });

        mockMvc.perform(get("/api/v1/pos/operations/by-idempotency-key")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("key", itemKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.found").value(true))
                .andExpect(jsonPath("$.data.idempotencyKey").value(itemKey))
                .andExpect(jsonPath("$.data.saleId").isNotEmpty());

        mockMvc.perform(get("/api/v1/pos/operations/by-idempotency-key/{key}", "missing-" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.found").value(false))
                .andExpect(jsonPath("$.data.outcome").value("NOT_FOUND"));
    }

    private br.com.systemcommerce.sale.dto.SaleResponse prepareSaleWithItem() {
        discardCurrentDraftIfAny();
        var sale = posSaleService.start(new PosSaleStartRequest(sessionId), "prep-" + UUID.randomUUID());
        return posSaleService.addByProductId(
                sale.id(),
                new PosSaleAddByProductIdRequest(productId, BigDecimal.ONE, sale.version()),
                "prep-item-" + UUID.randomUUID());
    }

    private void discardCurrentDraftIfAny() {
        try {
            var current = posSaleService.currentByTerminal(terminalId);
            if (current.status() == Sale.SaleStatus.DRAFT
                    || current.status() == Sale.SaleStatus.SUSPENDED) {
                posSaleService.discard(
                        current.id(),
                        new PosSaleDiscardRequest("limpeza teste resiliência", current.version()),
                        "discard-" + UUID.randomUUID());
            }
        } catch (Exception ignored) {
            // sem rascunho atual
        }
    }

    private Product seedProduct() {
        Category cat = categoryRepository.findByNameIgnoreCase("Informática").orElseGet(() -> {
            Category c = new Category();
            c.setName("Informática");
            c.markActive();
            return categoryRepository.saveAndFlush(c);
        });
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product p = new Product();
        p.setInternalCode("RES-" + suffix);
        p.setSku("SKU-RES-" + suffix);
        p.setBarcode("789" + suffix);
        p.setName("Produto resiliência " + suffix);
        p.setCategory(cat);
        p.setUnitOfMeasure("UN");
        p.setSalePrice(new BigDecimal("25.00"));
        p.setCostPrice(BigDecimal.TEN);
        p.setMinStock(BigDecimal.ZERO);
        p.setAllowNegativeStock(false);
        p.markActive();
        Product saved = productRepository.saveAndFlush(p);
        storeProductService.enable(new StoreProductEnableRequest(storeId, saved.getId()));
        inventoryService.registerEntry(
                new InventoryEntryRequest(saved.getId(), null, new BigDecimal("50"), "seed resilience", false));
        return saved;
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
                                    new SimpleGrantedAuthority("POS_VIEW_SESSION"),
                                    new SimpleGrantedAuthority("POS_SALE_CREATE"),
                                    new SimpleGrantedAuthority("POS_SALE_CANCEL"),
                                    new SimpleGrantedAuthority("POS_SALE_ITEM_REMOVE"),
                                    new SimpleGrantedAuthority("POS_PAYMENT_MANAGE"),
                                    new SimpleGrantedAuthority("POS_SALE_FINALIZE"),
                                    new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                    new SimpleGrantedAuthority("PRODUCT_READ"),
                                    new SimpleGrantedAuthority("POS_TERMINAL_READ"),
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

