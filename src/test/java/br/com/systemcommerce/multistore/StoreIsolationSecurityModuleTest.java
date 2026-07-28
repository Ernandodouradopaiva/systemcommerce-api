package br.com.systemcommerce.multistore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.storeaccess.dto.UserStoreAccessGrantRequest;
import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import br.com.systemcommerce.storeaccess.service.StoreAccessService;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.support.IntegrationTestUsers;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prompt 81 — isolamento de segurança entre lojas (MockMvc + services).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class StoreIsolationSecurityModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_store_isolation_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SaleService saleService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StoreAccessService storeAccessService;

    @Autowired
    private SellerService sellerService;

    @Autowired
    private EmployeeService employeeService;

    @Autowired
    private StoreProductService storeProductService;

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

    private String adminToken;
    private UUID adminUserId;
    private UUID loja01Id;
    private UUID loja02Id;
    private UUID dep01Id;
    private UUID dep02Id;
    private UUID customerId;
    private UUID defaultOrgId;
    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        adminToken = login("admin", "Admin@123").path("accessToken").asText();
        adminUserId = userRepository.findByLoginIgnoreCase("admin").orElseThrow().getId();
        asAdmin();

        loja01Id = findStoreId("LOJA-01");
        loja02Id = findStoreId("LOJA-02");
        dep01Id = findWarehouseId(loja01Id, "DEP-01");
        dep02Id = findWarehouseId(loja02Id, "DEP-02");
        defaultOrgId = organizationService.getDefault().id();
        customerId = customerRepository.findByDocument("52998224725").orElseThrow().getId();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
    }

    @Test
    void userOfStoreACannotGetSaleOfStoreBByUuid() throws Exception {
        asAdmin();
        SaleResponse saleB = saleService.createDraft(draft(loja02Id, dep02Id, null));

        User limited = createLimitedSellerOnStore(loja01Id);
        String limitedToken = login(limited.getLogin(), "Test@1234").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/sales/{id}", saleB.id())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + limitedToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STORE_ACCESS_DENIED"));

        asUser(limited.getId(), "SALE_READ");
        assertThatThrownBy(() -> saleService.getById(saleB.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_ACCESS_DENIED);
    }

    @Test
    void userOfStoreACannotListOrGetInventoryOfStoreB() throws Exception {
        asAdmin();
        Product product = createProduct();
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), dep02Id, new BigDecimal("3"), "seed B", false));

        User limited = createLimitedSellerOnStore(loja01Id);
        String limitedToken = login(limited.getLogin(), "Test@1234").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/inventory")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + limitedToken)
                        .param("storeId", loja02Id.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STORE_ACCESS_DENIED"));

        mockMvc.perform(get("/api/v1/inventory/by-store/{storeId}", loja02Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + limitedToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STORE_ACCESS_DENIED"));

        asUser(limited.getId(), "INVENTORY_READ");
        assertThatThrownBy(() -> inventoryService.list(null, loja02Id, null, null, null, Pageable.unpaged()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_ACCESS_DENIED);

        assertThatThrownBy(() -> inventoryService.getBalance(product.getId(), dep02Id))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_ACCESS_DENIED);
    }

    @Test
    void invalidStoreIdHeaderReturnsStoreContextInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/store-access/context/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("X-Store-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STORE_CONTEXT_INVALID"));
    }

    @Test
    void draftSaleWithWarehouseOfAnotherStoreIsBlocked() {
        asAdmin();
        assertThatThrownBy(() -> saleService.createDraft(draft(loja01Id, dep02Id, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não pertence");
    }

    @Test
    void sellerNotAuthorizedForStoreIsBlocked() {
        asAdmin();
        UUID sellerOnlyLoja02 = createSellerForStore(loja02Id, "SB");
        assertThatThrownBy(() -> saleService.createDraft(draft(loja01Id, dep01Id, sellerOnlyLoja02)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não autorizado");
    }

    @Test
    void storeManagerWithoutConsolidatedPermissionCannotListConsolidatedSales() throws Exception {
        User managerLike = createLimitedSellerOnStore(loja01Id);
        String token = login(managerLike.getLogin(), "Test@1234").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/sales/consolidated")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());

        asUser(managerLike.getId(), "SALE_READ");
        assertThatThrownBy(() -> saleService.listConsolidated(
                        null, null, null, null, null, null, null, null, Pageable.unpaged()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("consolidada");
    }

    @Test
    void adminWithGlobalAccessCanListConsolidatedSales() throws Exception {
        mockMvc.perform(get("/api/v1/sales/consolidated")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        asAdmin();
        assertThat(saleService
                        .listConsolidated(null, null, null, null, null, null, null, null, Pageable.ofSize(5))
                        .getContent())
                .isNotNull();
    }

    @Test
    void saleInStoreADoesNotChangeStockOfStoreBAndCancelRestoresOnlyOrigin() {
        asAdmin();
        Product product = createProduct();
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), dep01Id, new BigDecimal("10"), "seed A", false));
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), dep02Id, new BigDecimal("7"), "seed B", false));

        BigDecimal beforeA = inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity();
        BigDecimal beforeB = inventoryService.getBalance(product.getId(), dep02Id).physicalQuantity();

        SaleResponse sale = prepareConfirmedSale(loja01Id, dep01Id, product.getId(), new BigDecimal("3"));

        assertThat(inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity())
                .isEqualByComparingTo(beforeA.subtract(new BigDecimal("3")));
        assertThat(inventoryService.getBalance(product.getId(), dep02Id).physicalQuantity())
                .isEqualByComparingTo(beforeB);

        saleService.cancel(sale.id(), new SaleCancelRequest("isolamento cancel"));
        assertThat(inventoryService.getBalance(product.getId(), dep01Id).physicalQuantity())
                .isEqualByComparingTo(beforeA);
        assertThat(inventoryService.getBalance(product.getId(), dep02Id).physicalQuantity())
                .isEqualByComparingTo(beforeB);
    }

    @Test
    void directUuidLookupRespectsStoreAccess() {
        asAdmin();
        SaleResponse saleA = saleService.createDraft(draft(loja01Id, dep01Id, null));
        SaleResponse saleB = saleService.createDraft(draft(loja02Id, dep02Id, null));

        User limited = createLimitedSellerOnStore(loja01Id);
        asUser(limited.getId(), "SALE_READ");

        assertThat(saleService.getById(saleA.id()).storeId()).isEqualTo(loja01Id);
        assertThatThrownBy(() -> saleService.getById(saleB.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_ACCESS_DENIED);
    }

    private SaleResponse prepareConfirmedSale(UUID storeId, UUID warehouseId, UUID productId, BigDecimal qty) {
        SaleResponse draft = saleService.createDraft(draft(storeId, warehouseId, null));
        saleService.setCustomer(draft.id(), new SaleCustomerRequest(customerId));
        saleService.addItem(
                draft.id(), new SaleItemRequest(productId, qty, new BigDecimal("10.00"), BigDecimal.ZERO, null));
        return saleService.confirm(draft.id());
    }

    private SaleCreateRequest draft(UUID storeId, UUID warehouseId, UUID sellerProfileId) {
        return new SaleCreateRequest(storeId, warehouseId, null, sellerProfileId, null, "isolation");
    }

    private User createLimitedSellerOnStore(UUID storeId) {
        User user = IntegrationTestUsers.createUser(userRepository, roleRepository, passwordEncoder, "SELLER");
        storeAccessService.grant(new UserStoreAccessGrantRequest(
                user.getId(),
                storeId,
                LocalDate.now(),
                null,
                true,
                UserStoreAccess.AccessType.PERMANENT,
                "acesso limitado loja"));
        return user;
    }

    private UUID createSellerForStore(UUID storeId, String codeSuffix) {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var employee = employeeService.create(new EmployeeCreateRequest(
                defaultOrgId,
                "SI-" + suffix,
                "Seller Iso " + suffix,
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

    private Product createProduct() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("ISO-" + suffix);
        product.setSku("SKU-ISO-" + suffix);
        product.setName("Produto isolamento " + suffix);
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

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"%s","password":"%s"}
                                """
                                        .formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
    }

    private UUID findStoreId(String code) {
        return storeService
                .list(null, code, null, null, null, null, null, null, Pageable.unpaged())
                .getContent()
                .getFirst()
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
        asUser(
                adminUserId,
                "SALE_CREATE",
                "SALE_CONFIRM",
                "SALE_CANCEL",
                "SALE_READ",
                "INVENTORY_READ",
                "INVENTORY_MOVE",
                "GLOBAL_STORE_ACCESS",
                "STORE_CONSOLIDATED_READ",
                "USER_STORE_ACCESS_MANAGE",
                "SELLER_CREATE",
                "SELLER_UPDATE",
                "SELLER_ASSIGN_STORE",
                "SELLER_AUTHORIZE_OTHER_STORE");
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
