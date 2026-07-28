package br.com.systemcommerce.commission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.commission.dto.CommissionCalculatePeriodResponse;
import br.com.systemcommerce.commission.dto.CommissionPolicyCreateRequest;
import br.com.systemcommerce.commission.entity.CommissionPolicy.PolicyChannel;
import br.com.systemcommerce.commission.service.CommissionService;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleCustomerRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.seller.service.SellerService;
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
import org.springframework.data.domain.Pageable;
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
class CommissionModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_commission_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CommissionService commissionService;

    @Autowired
    private SaleService saleService;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private CustomerRepository customerRepository;

    private UUID adminUserId;
    private UUID defaultOrgId;
    private UUID loja01Id;
    private UUID dep01Id;
    private UUID sellerProfileId;
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
        defaultOrgId = organizationService.getDefault().id();
        loja01Id = storeRepository.findAll().stream()
                .filter(s -> "LOJA-01".equals(s.getCode()))
                .findFirst()
                .orElseThrow()
                .getId();
        dep01Id = warehouseService.list(loja01Id, null, null, null, Pageable.unpaged()).stream()
                .filter(w -> "DEP-01".equals(w.code()))
                .findFirst()
                .orElseThrow()
                .id();
        sellerProfileId = sellerService.listByStore(loja01Id).stream()
                .filter(s -> "VEND-0001".equals(s.sellerCode()))
                .findFirst()
                .orElseThrow()
                .id();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
        asAdmin();
    }

    @Test
    void shouldCreatePolicyAndCalculateCommissionForConfirmedSale() {
        Product product = createProduct(new BigDecimal("100.00"));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("5"), "seed commission", false));

        SaleResponse draft = saleService.createDraft(
                new SaleCreateRequest(loja01Id, dep01Id, null, sellerProfileId, null, "comissão"));
        UUID customerId = customerRepository.findByDocument("52998224725").orElseThrow().getId();
        saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        saleService.addItem(
                draft.id(),
                new SaleItemRequest(product.getId(), new BigDecimal("2"), new BigDecimal("100.00"), BigDecimal.ZERO, null));
        SaleResponse confirmed = saleService.confirm(draft.id());
        assertThat(confirmed.totalAmount()).isEqualByComparingTo(new BigDecimal("200.00"));

        commissionService.createPolicy(new CommissionPolicyCreateRequest(
                defaultOrgId,
                "COM-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                "Comissão loja padrão",
                loja01Id,
                null,
                null,
                null,
                PolicyChannel.ANY,
                new BigDecimal("5.00"),
                BigDecimal.ZERO,
                false,
                true,
                null,
                null));

        Instant from = confirmed.saleDate().minus(1, ChronoUnit.DAYS);
        Instant to = confirmed.saleDate().plus(1, ChronoUnit.DAYS);
        CommissionCalculatePeriodResponse result = commissionService.calculatePeriod(loja01Id, from, to);

        assertThat(result.salesProcessed()).isEqualTo(1);
        assertThat(result.calculationsCreated()).isEqualTo(1);
        assertThat(result.calculations()).hasSize(1);
        assertThat(result.calculations().getFirst().commissionAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(result.calculations().getFirst().sellerProfileId()).isEqualTo(sellerProfileId);
    }

    private Product createProduct(BigDecimal price) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("COM-P-" + suffix);
        product.setSku("COM-SKU-" + suffix);
        product.setName("Produto Comissão " + suffix);
        product.setCategory(category);
        product.setSalePrice(price);
        product.setCostPrice(price.multiply(new BigDecimal("0.5")));
        product.setStatus(Product.ProductStatus.ACTIVE);
        Product saved = productRepository.save(product);
        storeProductService.enable(new StoreProductEnableRequest(loja01Id, saved.getId()));
        return saved;
    }

    private void asAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        "n/a",
                        java.util.List.of(
                                new SimpleGrantedAuthority("SALE_CREATE"),
                                new SimpleGrantedAuthority("SALE_UPDATE"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("SALE_READ"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"),
                                new SimpleGrantedAuthority("STORE_PRODUCT_MANAGE"),
                                new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                new SimpleGrantedAuthority("COMMISSION_MANAGE"),
                                new SimpleGrantedAuthority("COMMISSION_READ"))));
    }
}
