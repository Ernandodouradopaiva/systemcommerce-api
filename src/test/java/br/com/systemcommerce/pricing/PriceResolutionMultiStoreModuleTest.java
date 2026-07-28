package br.com.systemcommerce.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.pricing.dto.PriceTableCreateRequest;
import br.com.systemcommerce.pricing.dto.PromotionCreateRequest;
import br.com.systemcommerce.pricing.dto.PromotionProductLinkRequest;
import br.com.systemcommerce.pricing.dto.PromotionStoreLinkRequest;
import br.com.systemcommerce.pricing.dto.ProductPriceLinkRequest;
import br.com.systemcommerce.pricing.dto.StoreGroupCreateRequest;
import br.com.systemcommerce.pricing.dto.StoreGroupStoreLinkRequest;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.entity.PriceTableScopeType;
import br.com.systemcommerce.pricing.entity.ProductPrice;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.pricing.service.PriceTableService;
import br.com.systemcommerce.pricing.service.PromotionService;
import br.com.systemcommerce.pricing.service.StoreGroupService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.storeproduct.entity.StoreProduct;
import br.com.systemcommerce.storeproduct.repository.StoreProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class PriceResolutionMultiStoreModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pricing_multistore_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private PriceResolutionService priceResolutionService;

    @Autowired
    private PriceTableService priceTableService;

    @Autowired
    private StoreGroupService storeGroupService;

    @Autowired
    private PromotionService promotionService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreProductRepository storeProductRepository;

    @Autowired
    private br.com.systemcommerce.storeproduct.service.StoreProductService storeProductService;

    @Autowired
    private OrganizationService organizationService;

    private Category category;
    private UUID loja01Id;
    private UUID loja02Id;
    private UUID adminUserId = UUID.fromString("a0000000-0000-4000-8000-000000000001");

    @BeforeEach
    void setUp() {
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
        loja01Id = findStore("LOJA-01");
        loja02Id = findStore("LOJA-02");
    }

    @Test
    void shouldResolvePromotionOverStoreTableAndGroupAndGlobal() {
        withSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            createGlobalTable(product, new BigDecimal("95.00"), 10);
            createGroupTable(product, new BigDecimal("90.00"), 20);
            createStoreTable(product, loja01Id, new BigDecimal("85.00"), 30, PriceChannel.POS);
            createPromotion(product, loja01Id, new BigDecimal("69.90"), 100);

            var resolved = priceResolutionService.resolve(
                    product.getId(), loja01Id, BigDecimal.ONE, Instant.now(), PriceChannel.POS);
            assertThat(resolved.unitPrice()).isEqualByComparingTo("69.90");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.PROMOTIONAL);
            assertThat(resolved.promotionId()).isNotNull();
        });
    }

    @Test
    void shouldResolveStoreTableOverGroupAndGlobal() {
        withSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            createGlobalTable(product, new BigDecimal("95.00"), 10);
            createGroupTable(product, new BigDecimal("90.00"), 20);
            createStoreTable(product, loja01Id, new BigDecimal("82.00"), 30, PriceChannel.POS);

            var resolved = priceResolutionService.resolve(
                    product.getId(), loja01Id, BigDecimal.ONE, Instant.now(), PriceChannel.POS);
            assertThat(resolved.unitPrice()).isEqualByComparingTo("82.00");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.PRICE_TABLE);
        });
    }

    @Test
    void shouldResolveGroupTableForBothStoresInGroup() {
        withSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            createGlobalTable(product, new BigDecimal("95.00"), 10);
            var group = createGroupWithStores(loja01Id, loja02Id);
            var table = priceTableService.create(new PriceTableCreateRequest(
                    "GRP-" + UUID.randomUUID().toString().substring(0, 8),
                    "Grupo Sul",
                    null,
                    25,
                    PriceChannel.POS,
                    PriceTableScopeType.STORE_GROUP,
                    group.id(),
                    null,
                    null));
            priceTableService.linkProduct(
                    table.id(),
                    new ProductPriceLinkRequest(
                            product.getId(),
                            new BigDecimal("88.00"),
                            ProductPrice.PriceType.STANDARD,
                            BigDecimal.ONE,
                            5,
                            null,
                            null,
                            null));

            var loja01 = priceResolutionService.resolve(
                    product.getId(), loja01Id, BigDecimal.ONE, Instant.now(), PriceChannel.POS);
            var loja02 = priceResolutionService.resolve(
                    product.getId(), loja02Id, BigDecimal.ONE, Instant.now(), PriceChannel.POS);
            assertThat(loja01.unitPrice()).isEqualByComparingTo("88.00");
            assertThat(loja02.unitPrice()).isEqualByComparingTo("88.00");
            assertThat(loja01.priceSource()).isEqualTo(SaleItem.PriceSource.PRICE_TABLE);
        });
    }

    @Test
    void shouldResolveGlobalTableWhenNoStoreOrGroupMatch() {
        withSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            createGlobalTable(product, new BigDecimal("93.50"), 15);

            var resolved = priceResolutionService.resolve(
                    product.getId(), loja02Id, BigDecimal.ONE, Instant.now(), PriceChannel.ERP);
            assertThat(resolved.unitPrice()).isEqualByComparingTo("93.50");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.PRICE_TABLE);
        });
    }

    @Test
    void shouldUseStoreLocalBeforeCatalog() {
        withSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            storeProductService.enable(
                    new br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest(loja01Id, product.getId()));
            StoreProduct sp = storeProductRepository
                    .findByStoreIdAndProductId(loja01Id, product.getId())
                    .orElseThrow();
            sp.setLocalDefaultPrice(new BigDecimal("77.00"));
            storeProductRepository.saveAndFlush(sp);

            var resolved = priceResolutionService.resolve(
                    product.getId(), loja01Id, BigDecimal.ONE, Instant.now(), PriceChannel.ERP);
            assertThat(resolved.unitPrice()).isEqualByComparingTo("77.00");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.STORE_LOCAL);
        });
    }

    @Test
    void shouldFallbackToCatalogWhenNothingElseMatches() {
        withSecurity(() -> {
            Product product = createProduct(new BigDecimal("55.00"));
            var resolved = priceResolutionService.resolve(
                    product.getId(), loja01Id, BigDecimal.ONE, Instant.now(), PriceChannel.POS);
            assertThat(resolved.unitPrice()).isEqualByComparingTo("55.00");
            assertThat(resolved.priceSource()).isEqualTo(SaleItem.PriceSource.CATALOG);
        });
    }

    @Test
    void shouldDefaultToErpChannelOnLegacyResolve() {
        withSecurity(() -> {
            Product product = createProduct(new BigDecimal("100.00"));
            createStoreTable(product, loja01Id, new BigDecimal("80.00"), 30, PriceChannel.POS);
            createGlobalTable(product, new BigDecimal("92.00"), 10);

            var erp = priceResolutionService.resolve(product.getId(), loja01Id, BigDecimal.ONE, Instant.now());
            assertThat(erp.unitPrice()).isEqualByComparingTo("92.00");

            var pos = priceResolutionService.resolve(
                    product.getId(), loja01Id, BigDecimal.ONE, Instant.now(), PriceChannel.POS);
            assertThat(pos.unitPrice()).isEqualByComparingTo("80.00");
        });
    }

    private void createPromotion(Product product, UUID storeId, BigDecimal price, int priority) {
        var promo = promotionService.create(new PromotionCreateRequest(
                organizationService.getDefault().id(),
                "PR-" + UUID.randomUUID().toString().substring(0, 8),
                "Promo teste",
                null,
                PriceChannel.POS,
                priority,
                null,
                null,
                null,
                null,
                null,
                null,
                null));
        promotionService.linkStore(promo.id(), new PromotionStoreLinkRequest(storeId));
        promotionService.addProduct(
                promo.id(), new PromotionProductLinkRequest(product.getId(), price, BigDecimal.ONE));
    }

    private void createStoreTable(Product product, UUID storeId, BigDecimal price, int tablePriority, PriceChannel channel) {
        var table = priceTableService.create(new PriceTableCreateRequest(
                "ST-" + UUID.randomUUID().toString().substring(0, 8),
                "Loja",
                null,
                tablePriority,
                channel,
                PriceTableScopeType.STORE,
                null,
                null,
                null));
        priceTableService.linkStore(table.id(), storeId);
        priceTableService.linkProduct(
                table.id(),
                new ProductPriceLinkRequest(
                        product.getId(), price, ProductPrice.PriceType.STANDARD, BigDecimal.ONE, 5, null, null, null));
    }

    private void createGlobalTable(Product product, BigDecimal price, int tablePriority) {
        var table = priceTableService.create(new PriceTableCreateRequest(
                "GL-" + UUID.randomUUID().toString().substring(0, 8),
                "Global",
                null,
                tablePriority,
                PriceChannel.ERP,
                PriceTableScopeType.GLOBAL,
                null,
                null,
                null));
        priceTableService.linkProduct(
                table.id(),
                new ProductPriceLinkRequest(
                        product.getId(), price, ProductPrice.PriceType.STANDARD, BigDecimal.ONE, 5, null, null, null));
    }

    private br.com.systemcommerce.pricing.dto.StoreGroupResponse createGroupWithStores(UUID... storeIds) {
        var group = storeGroupService.create(new StoreGroupCreateRequest(
                organizationService.getDefault().id(),
                "SG-" + UUID.randomUUID().toString().substring(0, 8),
                "Grupo teste",
                null));
        for (UUID storeId : storeIds) {
            storeGroupService.linkStore(group.id(), new StoreGroupStoreLinkRequest(storeId));
        }
        return storeGroupService.getById(group.id());
    }

    private void createGroupTable(Product product, BigDecimal price, int tablePriority) {
        var group = createGroupWithStores(loja01Id, loja02Id);
        var table = priceTableService.create(new PriceTableCreateRequest(
                "GT-" + UUID.randomUUID().toString().substring(0, 8),
                "Grupo",
                null,
                tablePriority,
                PriceChannel.POS,
                PriceTableScopeType.STORE_GROUP,
                group.id(),
                null,
                null));
        priceTableService.linkProduct(
                table.id(),
                new ProductPriceLinkRequest(
                        product.getId(), price, ProductPrice.PriceType.STANDARD, BigDecimal.ONE, 5, null, null, null));
    }

    private Product createProduct(BigDecimal salePrice) {
        Product product = new Product();
        product.setInternalCode("MS-" + UUID.randomUUID().toString().substring(0, 8));
        product.setSku("SKU-MS-" + UUID.randomUUID().toString().substring(0, 8));
        product.setName("Produto multistore " + product.getSku());
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(salePrice);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(true);
        product.markActive();
        return productRepository.saveAndFlush(product);
    }

    private UUID findStore(String code) {
        return storeRepository.findAll().stream()
                .filter(s -> code.equals(s.getCode()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private void withSecurity(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("PRICE_TABLE_READ"),
                                new SimpleGrantedAuthority("PRICE_TABLE_MANAGE"),
                                new SimpleGrantedAuthority("STORE_GROUP_MANAGE"),
                                new SimpleGrantedAuthority("PROMOTION_MANAGE"),
                                new SimpleGrantedAuthority("STORE_PRODUCT_MANAGE"),
                                new SimpleGrantedAuthority("STORE_PRODUCT_READ"),
                                new SimpleGrantedAuthority("STORE_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
