package br.com.systemcommerce.multistore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.employee.dto.EmployeeCreateRequest;
import br.com.systemcommerce.employee.entity.Employee;
import br.com.systemcommerce.employee.service.EmployeeService;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseCreateRequest;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.pricing.entity.PriceChannel;
import br.com.systemcommerce.pricing.service.PriceResolutionService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleCustomerRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.entity.SaleItem;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.seller.dto.SellerEnableRequest;
import br.com.systemcommerce.seller.dto.SellerStoreAuthorizeRequest;
import br.com.systemcommerce.seller.service.SellerService;
import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.stocktransfer.dto.StockTransferCreateRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferItemCreateRequest;
import br.com.systemcommerce.stocktransfer.dto.StockTransferReceiveRequest;
import br.com.systemcommerce.stocktransfer.service.StockTransferService;
import br.com.systemcommerce.storeaccess.dto.UserStoreAccessGrantRequest;
import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import br.com.systemcommerce.storeaccess.service.StoreAccessService;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.dto.StoreProductUpdateRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.support.IntegrationTestUsers;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prompt 82 — cenário funcional multilojas (org, duas lojas, preços locais, estoque, transferência).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MultistoreFunctionalScenarioModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_multistore_functional_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private StoreProductService storeProductService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SaleService saleService;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private StockTransferService stockTransferService;

    @Autowired
    private PriceResolutionService priceResolutionService;

    @Autowired
    private StoreAccessService storeAccessService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UUID orgId;
    private UUID loja01Id;
    private UUID loja02Id;
    private UUID dep01Id;
    private UUID dep01BId;
    private UUID dep02Id;
    private UUID customerId;
    private UUID adminUserId;
    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk());

        adminUserId = userRepository.findByLoginIgnoreCase("admin").orElseThrow().getId();
        asAdmin();

        orgId = organizationService.getDefault().id();
        assertThat(orgId).isNotNull();

        var loja01 = storeService
                .list(null, "LOJA-01", null, null, null, null, null, null, Pageable.unpaged())
                .getContent()
                .getFirst();
        var loja02 = storeService
                .list(null, "LOJA-02", null, null, null, null, null, null, Pageable.unpaged())
                .getContent()
                .getFirst();
        loja01Id = loja01.id();
        loja02Id = loja02.id();
        // Seeds: LOJA-01 principal (Centro operacional); LOJA-02 filial
        assertThat(loja01.code()).isEqualTo("LOJA-01");
        assertThat(loja02.code()).isEqualTo("LOJA-02");

        dep01Id = findWarehouseId(loja01Id, "DEP-01");
        dep02Id = findWarehouseId(loja02Id, "DEP-02");

        String secondCode = "DEP-01B-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        dep01BId = warehouseService
                .create(new WarehouseCreateRequest(loja01Id, secondCode, "Depósito Secundário Centro", true))
                .id();

        customerId = customerRepository.findByDocument("52998224725").orElseThrow().getId();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void fullMultistoreFunctionalScenario() {
        asAdmin();

        Product product = createProduct(new BigDecimal("50.00"));

        var sp01 = storeProductService.enable(new StoreProductEnableRequest(loja01Id, product.getId()));
        var sp02 = storeProductService.enable(new StoreProductEnableRequest(loja02Id, product.getId()));

        BigDecimal priceCentro = new BigDecimal("99.90");
        BigDecimal priceShopping = new BigDecimal("109.90");
        storeProductService.update(
                sp01.id(),
                new StoreProductUpdateRequest(
                        null, null, null, null, null, null, priceCentro, null, null, null, null, null, null, null,
                        null, null, null));
        storeProductService.update(
                sp02.id(),
                new StoreProductUpdateRequest(
                        null, null, null, null, null, null, priceShopping, null, null, null, null, null, null, null,
                        null, null, null));

        var resolved01 = priceResolutionService.resolve(
                product.getId(), loja01Id, BigDecimal.ONE, Instant.now(), PriceChannel.ERP);
        var resolved02 = priceResolutionService.resolve(
                product.getId(), loja02Id, BigDecimal.ONE, Instant.now(), PriceChannel.ERP);
        assertThat(resolved01.unitPrice()).isEqualByComparingTo(priceCentro);
        assertThat(resolved01.priceSource()).isEqualTo(SaleItem.PriceSource.STORE_LOCAL);
        assertThat(resolved02.unitPrice()).isEqualByComparingTo(priceShopping);
        assertThat(resolved02.priceSource()).isEqualTo(SaleItem.PriceSource.STORE_LOCAL);

        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), dep01Id, new BigDecimal("20"), "entrada centro", false));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), dep01BId, new BigDecimal("5"), "entrada dep B", false));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), dep02Id, new BigDecimal("8"), "entrada shopping", false));

        assertThat(inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity())
                .isEqualByComparingTo("20");
        assertThat(inventoryService.getBalance(product.getId(), dep01BId).physicalQuantity())
                .isEqualByComparingTo("5");
        assertThat(inventoryService.getBalance(product.getId(), dep02Id).physicalQuantity())
                .isEqualByComparingTo("8");

        UUID sellerCentro = createSellerForStore(loja01Id, "SC");
        UUID sellerShopping = createSellerForStore(loja02Id, "SS");

        SaleResponse saleCentro = createDraftWithItem(loja01Id, dep01Id, sellerCentro, product.getId());
        SaleResponse saleShopping = createDraftWithItem(loja02Id, dep02Id, sellerShopping, product.getId());

        assertThat(saleCentro.storeId()).isEqualTo(loja01Id);
        assertThat(saleCentro.warehouseId()).isEqualTo(dep01Id);
        assertThat(saleCentro.sellerProfileId()).isEqualTo(sellerCentro);
        assertThat(saleCentro.items()).isNotEmpty();
        assertThat(saleCentro.items().getFirst().unitPrice()).isEqualByComparingTo(priceCentro);
        assertThat(saleCentro.items().getFirst().priceSource()).isEqualTo(SaleItem.PriceSource.STORE_LOCAL);

        assertThat(saleShopping.storeId()).isEqualTo(loja02Id);
        assertThat(saleShopping.warehouseId()).isEqualTo(dep02Id);
        assertThat(saleShopping.sellerProfileId()).isEqualTo(sellerShopping);
        assertThat(saleShopping.items().getFirst().unitPrice()).isEqualByComparingTo(priceShopping);

        BigDecimal originBefore = inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity();
        BigDecimal destBefore = inventoryService.getBalance(product.getId(), dep02Id).physicalQuantity();
        BigDecimal transferQty = new BigDecimal("4.000");

        UUID transferId = stockTransferService
                .create(new StockTransferCreateRequest(
                        orgId,
                        loja01Id,
                        dep01Id,
                        loja02Id,
                        dep02Id,
                        "reposição funcional",
                        "cenário 82",
                        "func-" + UUID.randomUUID()))
                .id();
        stockTransferService.addItem(
                transferId, new StockTransferItemCreateRequest(product.getId(), transferQty, null));
        stockTransferService.request(transferId, null);
        stockTransferService.approve(transferId, null);
        stockTransferService.prepare(transferId, null);
        stockTransferService.dispatch(transferId, null);
        stockTransferService.receive(transferId, new StockTransferReceiveRequest(List.of(), null, "recv-func-1"));

        assertThat(inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity())
                .isEqualByComparingTo(originBefore.subtract(transferQty));
        assertThat(inventoryService.getBalance(product.getId(), dep02Id).physicalQuantity())
                .isEqualByComparingTo(destBefore.add(transferQty));

        User limited = IntegrationTestUsers.createUser(
                userRepository, roleRepository, passwordEncoder, "SELLER");
        storeAccessService.grant(new UserStoreAccessGrantRequest(
                limited.getId(),
                loja01Id,
                LocalDate.now(),
                null,
                true,
                UserStoreAccess.AccessType.PERMANENT,
                "só centro"));

        asUser(limited.getId(), "SALE_READ", "INVENTORY_READ");
        assertThat(saleService.getById(saleCentro.id()).id()).isEqualTo(saleCentro.id());
        assertThatThrownBy(() -> saleService.getById(saleShopping.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_ACCESS_DENIED);
        assertThatThrownBy(() -> inventoryService.list(null, loja02Id, null, null, null, Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_ACCESS_DENIED);
    }

    private SaleResponse createDraftWithItem(
            UUID storeId, UUID warehouseId, UUID sellerId, UUID productId) {
        SaleResponse draft = saleService.createDraft(
                new SaleCreateRequest(storeId, warehouseId, null, sellerId, null, "cenário funcional"));
        saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        return saleService.addItem(
                draft.id(), new SaleItemRequest(productId, BigDecimal.ONE, null, BigDecimal.ZERO, null));
    }

    private UUID createSellerForStore(UUID storeId, String codeSuffix) {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var employee = employeeService.create(new EmployeeCreateRequest(
                orgId,
                "SF-" + suffix,
                "Seller Func " + suffix,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                LocalDate.now(),
                null,
                "Vendedor",
                Employee.EmployeeStatus.ACTIVE,
                true,
                null,
                null));
        var seller = sellerService.enable(new SellerEnableRequest(
                employee.id(), codeSuffix + "-" + suffix, BigDecimal.ZERO, false, true, null, null, null, null));
        sellerService.authorizeStore(
                seller.id(),
                new SellerStoreAuthorizeRequest(
                        storeId, LocalDate.now(), null, true, false, true, null, null, null));
        return seller.id();
    }

    private Product createProduct(BigDecimal catalogPrice) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("FN-" + suffix);
        product.setSku("SKU-FN-" + suffix);
        product.setName("Produto funcional " + suffix);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(catalogPrice);
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        return productRepository.saveAndFlush(product);
    }

    private UUID findWarehouseId(UUID storeId, String code) {
        return warehouseService.list(storeId, null, null, null, Pageable.unpaged()).stream()
                .filter(w -> code.equals(w.code()))
                .findFirst()
                .orElseThrow()
                .id();
    }

    private void asAdmin() {
        asUser(
                adminUserId,
                "SALE_CREATE",
                "SALE_CONFIRM",
                "SALE_READ",
                "INVENTORY_READ",
                "INVENTORY_MOVE",
                "GLOBAL_STORE_ACCESS",
                "STORE_MANAGE",
                "WAREHOUSE_MANAGE",
                "SELLER_CREATE",
                "SELLER_UPDATE",
                "SELLER_ASSIGN_STORE",
                "SELLER_AUTHORIZE_OTHER_STORE",
                "USER_STORE_ACCESS_MANAGE",
                "STOCK_TRANSFER_CREATE",
                "STOCK_TRANSFER_APPROVE",
                "STOCK_TRANSFER_DISPATCH",
                "STOCK_TRANSFER_RECEIVE");
    }

    private void asUser(UUID userId, String... authorities) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        java.util.Arrays.stream(authorities)
                                .map(SimpleGrantedAuthority::new)
                                .toList()));
    }
}
