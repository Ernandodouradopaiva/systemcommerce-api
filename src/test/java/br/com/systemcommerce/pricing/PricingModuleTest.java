package br.com.systemcommerce.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.pricing.dto.DiscountPolicyCreateRequest;
import br.com.systemcommerce.pricing.dto.PriceTableCreateRequest;
import br.com.systemcommerce.pricing.dto.ProductPriceLinkRequest;
import br.com.systemcommerce.pricing.entity.DiscountPolicy;
import br.com.systemcommerce.pricing.entity.ProductPrice;
import br.com.systemcommerce.pricing.service.DiscountLimitService;
import br.com.systemcommerce.pricing.service.DiscountPolicyService;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.pricing.service.PriceTableService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
class PricingModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pricing_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PriceTableService priceTableService;

    @Autowired
    private PriceResolutionService priceResolutionService;

    @Autowired
    private DiscountPolicyService discountPolicyService;

    @Autowired
    private DiscountLimitService discountLimitService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StoreRepository storeRepository;

    private UUID adminUserId;
    private Category category;
    private UUID storeId;

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
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
        storeId = storeRepository.findAll().stream()
                .filter(s -> "LOJA-01".equals(s.getCode()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    @Test
    void shouldResolvePromotionalPriceWithinPeriod() {
        withPricingSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            var table = priceTableService.create(new PriceTableCreateRequest(
                    "PROMO-" + UUID.randomUUID().toString().substring(0, 8),
                    "Promo loja",
                    null,
                    10,
                    null,
                    null,
                    null,
                    Instant.now().minus(1, ChronoUnit.DAYS),
                    Instant.now().plus(30, ChronoUnit.DAYS)));
            priceTableService.linkStore(table.id(), storeId);
            priceTableService.linkProduct(
                    table.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("79.90"),
                            ProductPrice.PriceType.PROMOTIONAL,
                            BigDecimal.ONE,
                            5,
                            Instant.now().minus(1, ChronoUnit.HOURS),
                            Instant.now().plus(1, ChronoUnit.DAYS),
                            null));

            var resolved = priceResolutionService.resolve(product.getId(), storeId, BigDecimal.ONE, Instant.now());
            assertThat(resolved.unitPrice()).isEqualByComparingTo("79.90");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.PROMOTIONAL);
            assertThat(resolved.priceTableId()).isEqualTo(table.id());
        });
    }

    @Test
    void shouldFallbackToCatalogWhenNoTableMatches() {
        withPricingSecurity(() -> {
            Product product = createProduct(new BigDecimal("55.00"));
            var resolved = priceResolutionService.resolve(product.getId(), storeId, BigDecimal.ONE, Instant.now());
            assertThat(resolved.unitPrice()).isEqualByComparingTo("55.00");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.CATALOG);
        });
    }

    @Test
    void shouldRejectConflictingPriorityInSamePeriod() {
        withPricingSecurity(() -> {
            Product product = createProduct(new BigDecimal("40.00"));
            var table = priceTableService.create(new PriceTableCreateRequest(
                    "CONF-" + UUID.randomUUID().toString().substring(0, 8),
                    "Conflito",
                    null,
                    1,
                    null,
                    null,
                    null,
                    null,
                    null));
            Instant from = Instant.now().minus(1, ChronoUnit.DAYS);
            Instant to = Instant.now().plus(10, ChronoUnit.DAYS);
            priceTableService.linkProduct(
                    table.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("35.00"),
                            ProductPrice.PriceType.STANDARD,
                            BigDecimal.ZERO,
                            3,
                            from,
                            to,
                            null));
            assertThatThrownBy(() -> priceTableService.linkProduct(
                            table.id(),
                            new ProductPriceLinkRequest(
                                    product.getId(),
                                    new BigDecimal("33.00"),
                                    ProductPrice.PriceType.STANDARD,
                                    BigDecimal.ZERO,
                                    3,
                                    from,
                                    to,
                                    null)))
                    .isInstanceOf(ConflictException.class);
        });
    }

    @Test
    void shouldIgnoreExpiredPromotionalPrice() {
        withPricingSecurity(() -> {
            Product product = createProduct(new BigDecimal("90.00"));
            var table = priceTableService.create(new PriceTableCreateRequest(
                    "EXP-" + UUID.randomUUID().toString().substring(0, 8),
                    "Expirada",
                    null,
                    20,
                    null,
                    null,
                    null,
                    null,
                    null));
            priceTableService.linkStore(table.id(), storeId);
            priceTableService.linkProduct(
                    table.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("50.00"),
                            ProductPrice.PriceType.PROMOTIONAL,
                            BigDecimal.ONE,
                            10,
                            Instant.now().minus(10, ChronoUnit.DAYS),
                            Instant.now().minus(1, ChronoUnit.DAYS),
                            null));

            var resolved = priceResolutionService.resolve(product.getId(), storeId, BigDecimal.ONE, Instant.now());
            assertThat(resolved.unitPrice()).isEqualByComparingTo("90.00");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.CATALOG);
        });
    }

    @Test
    void shouldRespectMinQuantityOnPriceResolution() {
        withPricingSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            var table = priceTableService.create(new PriceTableCreateRequest(
                    "MINQ-" + UUID.randomUUID().toString().substring(0, 8),
                    "Min qty",
                    null,
                    15,
                    null,
                    null,
                    null,
                    null,
                    null));
            priceTableService.linkStore(table.id(), storeId);
            priceTableService.linkProduct(
                    table.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("80.00"),
                            ProductPrice.PriceType.STANDARD,
                            new BigDecimal("5"),
                            8,
                            null,
                            null,
                            null));

            var below = priceResolutionService.resolve(product.getId(), storeId, new BigDecimal("2"), Instant.now());
            assertThat(below.unitPrice()).isEqualByComparingTo("100.00");
            assertThat(below.priceSource()).isEqualTo(SaleItem.PriceSource.CATALOG);

            var ok = priceResolutionService.resolve(product.getId(), storeId, new BigDecimal("5"), Instant.now());
            assertThat(ok.unitPrice()).isEqualByComparingTo("80.00");
            assertThat(ok.priceSource()).isEqualTo(SaleItem.PriceSource.PRICE_TABLE);
        });
    }

    @Test
    void shouldIgnoreInactiveProductPrice() {
        withPricingSecurity(() -> {
            Product product = createProduct(new BigDecimal("70.00"));
            var table = priceTableService.create(new PriceTableCreateRequest(
                    "INACT-" + UUID.randomUUID().toString().substring(0, 8),
                    "Inativo",
                    null,
                    12,
                    null,
                    null,
                    null,
                    null,
                    null));
            priceTableService.linkStore(table.id(), storeId);
            var linked = priceTableService.linkProduct(
                    table.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("50.00"),
                            ProductPrice.PriceType.STANDARD,
                            BigDecimal.ONE,
                            5,
                            null,
                            null,
                            null));
            priceTableService.updateProductPrice(
                    table.id(),
                    linked.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("50.00"),
                            ProductPrice.PriceType.STANDARD,
                            BigDecimal.ONE,
                            5,
                            null,
                            null,
                            ProductPrice.Status.INACTIVE));

            var resolved = priceResolutionService.resolve(product.getId(), storeId, BigDecimal.ONE, Instant.now());
            assertThat(resolved.unitPrice()).isEqualByComparingTo("70.00");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.CATALOG);
        });
    }

    @Test
    void shouldEnforceProductDiscountPolicy() {
        withPricingSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            discountPolicyService.create(new DiscountPolicyCreateRequest(
                    "PROD-" + UUID.randomUUID().toString().substring(0, 8),
                    "Política produto",
                    null,
                    DiscountPolicy.AppliesTo.PRODUCT,
                    product.getId(),
                    null,
                    new BigDecimal("5.0000"),
                    null,
                    50,
                    null,
                    null));

            assertThatThrownBy(() -> discountLimitService.assertDiscountAllowed(
                            UUID.randomUUID(),
                            null,
                            product,
                            new BigDecimal("100.00"),
                            new BigDecimal("10.00"),
                            null))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("política");
        });
    }

    private Product createProduct(BigDecimal price) {
        Product product = new Product();
        product.setInternalCode("PRC-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-P-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Produto preço " + product.getSku());
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(price);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(true);
        product.markActive();
        return productRepository.saveAndFlush(product);
    }

    private void withPricingSecurity(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("PRICE_TABLE_READ"),
                                new SimpleGrantedAuthority("PRICE_TABLE_MANAGE"),
                                new SimpleGrantedAuthority("DISCOUNT_POLICY_READ"),
                                new SimpleGrantedAuthority("DISCOUNT_POLICY_MANAGE"),
                                new SimpleGrantedAuthority("POS_DISCOUNT_AUTHORIZE"),
                                new SimpleGrantedAuthority("STORE_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
