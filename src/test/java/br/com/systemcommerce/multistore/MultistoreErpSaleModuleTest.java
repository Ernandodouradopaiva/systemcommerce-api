package br.com.systemcommerce.multistore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.employee.dto.EmployeeCreateRequest;
import br.com.systemcommerce.employee.entity.Employee;
import br.com.systemcommerce.employee.service.EmployeeService;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.dto.SaleCancelRequest;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleCustomerRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.seller.dto.SellerEnableRequest;
import br.com.systemcommerce.seller.dto.SellerStoreAuthorizeRequest;
import br.com.systemcommerce.seller.service.SellerService;
import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.storeaccess.dto.UserStoreAccessGrantRequest;
import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import br.com.systemcommerce.storeaccess.service.StoreAccessService;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class MultistoreErpSaleModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_multistore_erp_sale_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SaleService saleService;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private StoreAccessService storeAccessService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private StoreProductService storeProductService;

    private UUID loja01Id;
    private UUID loja02Id;
    private UUID dep01Id;
    private UUID dep02Id;
    private UUID customerId;
    private UUID defaultOrgId;
    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        loja01Id = findStoreId("LOJA-01");
        loja02Id = findStoreId("LOJA-02");
        dep01Id = findWarehouseId(loja01Id, "DEP-01");
        dep02Id = findWarehouseId(loja02Id, "DEP-02");
        defaultOrgId = organizationService.getDefault().id();
        customerId = customerRepository.findByDocument("52998224725").orElseThrow().getId();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
        asAdmin();
    }

    @Test
    void shouldCreateSaleOnEachStoreWithStoreSequenceNumber() {
        SaleResponse loja01 = saleService.createDraft(draft(loja01Id, dep01Id, null));
        SaleResponse loja02 = saleService.createDraft(draft(loja02Id, dep02Id, null));

        assertThat(loja01.storeId()).isEqualTo(loja01Id);
        assertThat(loja01.warehouseId()).isEqualTo(dep01Id);
        assertThat(loja01.saleNumber()).startsWith("V-LOJA-01-");

        assertThat(loja02.storeId()).isEqualTo(loja02Id);
        assertThat(loja02.warehouseId()).isEqualTo(dep02Id);
        assertThat(loja02.saleNumber()).startsWith("V-LOJA-02-");
    }

    @Test
    void saleInOneWarehouseDoesNotAffectOtherStoreStock() {
        Product product = createProduct();
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), dep01Id, new BigDecimal("5"), "seed loja01", false));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), dep02Id, new BigDecimal("1"), "seed loja02", false));
        BigDecimal before01 = inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity();
        BigDecimal before02 = inventoryService.getBalance(product.getId(), dep02Id).physicalQuantity();

        SaleResponse sale = prepareConfirmedSale(loja01Id, dep01Id, product.getId(), new BigDecimal("2"));

        assertThat(inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity())
                .isEqualByComparingTo(before01.subtract(new BigDecimal("2")));
        assertThat(inventoryService.getBalance(product.getId(), dep02Id).physicalQuantity())
                .isEqualByComparingTo(before02);

        saleService.cancel(sale.id(), new SaleCancelRequest("teste isolamento"));
        assertThat(inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity())
                .isEqualByComparingTo(before01);
    }

    @Test
    void shouldRejectWarehouseFromAnotherStore() {
        assertThatThrownBy(() -> saleService.createDraft(draft(loja01Id, dep02Id, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não pertence");
    }

    @Test
    void shouldRejectSellerNotAuthorizedForStore() {
        UUID sellerLoja02 = createSellerForStore(loja02Id, "V2");
        assertThatThrownBy(() -> saleService.createDraft(draft(loja01Id, dep01Id, sellerLoja02)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não autorizado");
    }

    @Test
    void shouldDenySaleForStoreWithoutAccess() {
        User limited = createLimitedUser();
        UUID sellerId = sellerService.listByStore(loja01Id).getFirst().id();

        asUser(limited.getId(), "SALE_CREATE", "SALE_READ");
        assertThatThrownBy(() -> saleService.createDraft(draft(loja01Id, dep01Id, sellerId)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("acesso");
    }

    private SaleResponse prepareConfirmedSale(UUID storeId, UUID warehouseId, UUID productId, BigDecimal qty) {
        SaleResponse draft = saleService.createDraft(draft(storeId, warehouseId, null));
        saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        saleService.addItem(
                draft.id(), new SaleItemRequest(productId, qty, new BigDecimal("10.00"), BigDecimal.ZERO, null));
        return saleService.confirm(draft.id());
    }

    private UUID createSellerForStore(UUID storeId, String codeSuffix) {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var employee = employeeService.create(new EmployeeCreateRequest(
                defaultOrgId,
                "SE-" + suffix,
                "Seller " + suffix,
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

    private User createLimitedUser() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toLowerCase();
        User user = new User();
        user.setName("Limited " + suffix);
        user.setEmail("lim-" + suffix + "@test.local");
        user.setLogin("lim" + suffix);
        user.setPasswordHash(passwordEncoder.encode("Admin@123"));
        user.setStatus(User.UserStatus.ACTIVE);
        user = userRepository.save(user);
        storeAccessService.grant(new UserStoreAccessGrantRequest(
                user.getId(),
                loja02Id,
                LocalDate.now(),
                null,
                true,
                UserStoreAccess.AccessType.PERMANENT,
                "acesso só loja 02"));
        return user;
    }

    private SaleCreateRequest draft(UUID storeId, UUID warehouseId, UUID sellerProfileId) {
        return new SaleCreateRequest(storeId, warehouseId, null, sellerProfileId, null, "multistore erp");
    }

    private Product createProduct() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("MS-" + suffix);
        product.setSku("SKU-MS-" + suffix);
        product.setName("Produto multistore " + suffix);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(new BigDecimal("10.00"));
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(loja01Id, saved.getId()));
        storeProductService.enable(new StoreProductEnableRequest(loja02Id, saved.getId()));
        return saved;
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

    private void asAdmin() {
        UUID adminId = userRepository.findByLoginIgnoreCase("admin").orElseThrow().getId();
        asUser(adminId, "SALE_CREATE", "SALE_CONFIRM", "SALE_CANCEL", "SALE_READ", "GLOBAL_STORE_ACCESS");
    }

    private void asUser(UUID userId, String... authorities) {
        var auth = new UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                java.util.Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .toList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
