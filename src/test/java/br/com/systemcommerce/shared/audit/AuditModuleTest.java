package br.com.systemcommerce.shared.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.dto.CustomerCreateRequest;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.inventory.dto.InventoryEntryRequest;
import br.com.systemcommerce.inventory.service.InventoryService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Category;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.CategoryRepository;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.storeproduct.dto.StoreProductEnableRequest;
import br.com.systemcommerce.storeproduct.service.StoreProductService;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.dto.SaleItemRequest;
import br.com.systemcommerce.sale.dto.SaleResponse;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class AuditModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_audit_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private SaleService saleService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private StoreProductService storeProductService;

    private String adminToken;
    private UUID adminUserId;
    private UUID customerId;
    private UUID loja01Id;
    private UUID dep01Id;
    private Category category;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode login = login("admin", "Admin@123");
        adminToken = login.path("accessToken").asText();
        adminUserId = UUID.fromString(login.path("user").path("id").asText());
        authenticateAs(adminUserId);

        Customer maria = customerRepository.findByDocument("52998224725").orElseThrow();
        maria.markActive();
        customerRepository.saveAndFlush(maria);
        customerId = maria.getId();
        category = categoryRepository.findByNameIgnoreCase("Informática").orElseThrow();
        loja01Id = storeService
                .list(null, "LOJA-01", null, null, null, null, null, null, Pageable.unpaged())
                .getContent()
                .getFirst()
                .id();
        dep01Id = warehouseService.list(loja01Id, null, null, null, Pageable.unpaged()).stream()
                .filter(w -> "DEP-01".equals(w.code()))
                .findFirst()
                .orElseThrow()
                .id();
    }

    @Test
    void shouldAuditLoginAndLoginFailureWithoutSensitiveData() throws Exception {
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditLog.AuditAction.LOGIN
                        && "AUTH".equals(log.getModule())
                        && adminUserId.equals(log.getEntityId())
                        && log.getCorrelationId() != null
                        && log.getPerformedBy() != null
                        && log.getIpAddress() != null);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"WrongPassword1"}
                                """))
                .andExpect(status().isUnauthorized());

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> log.getAction() == AuditLog.AuditAction.LOGIN_FAILURE
                        && "AUTH".equals(log.getModule())
                        && log.getNewValues() != null
                        && !log.getNewValues().contains("WrongPassword")
                        && !log.getNewValues().toLowerCase().contains("\"password\""));
    }

    @Test
    void shouldAuditDomainEventsAndSupportFilteredPagedQuery() {
        customerService.create(new CustomerCreateRequest(
                Customer.CustomerType.PF,
                "Cliente Auditoria",
                null,
                "11144477735",
                null,
                "audit+" + UUID.randomUUID() + "@example.com",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null));

        Product product = createProduct();
        inventoryService.registerEntry(
                new InventoryEntryRequest(product.getId(), null, new BigDecimal("5"), "entrada audit", false));

        SaleResponse draft = saleService.createDraft(
                new SaleCreateRequest(loja01Id, dep01Id, customerId, null, null, "venda audit"));
        saleService.addItem(
                draft.id(), new SaleItemRequest(product.getId(), BigDecimal.ONE, null, BigDecimal.ZERO, null));
        saleService.confirm(draft.id());

        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "CUSTOMER".equals(log.getModule()) && log.getAction() == AuditLog.AuditAction.CREATE);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "INVENTORY".equals(log.getModule())
                        && log.getAction() == AuditLog.AuditAction.STOCK_MOVEMENT);
        assertThat(auditLogRepository.findAll())
                .anyMatch(log -> "SALE".equals(log.getModule())
                        && (log.getAction() == AuditLog.AuditAction.CREATE
                                || log.getAction() == AuditLog.AuditAction.UPDATE));
    }

    @Test
    void shouldQueryAuditLogsWithFiltersAndPagination() throws Exception {
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("module", "AUTH")
                        .param("action", "LOGIN")
                        .param("userId", adminUserId.toString())
                        .param("entity", "Auth")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$.data[0].module").value("AUTH"))
                .andExpect(jsonPath("$.data[0].action").value("LOGIN"))
                .andExpect(jsonPath("$.data[0].correlationId").isNotEmpty())
                .andExpect(jsonPath("$.data[0].performedById").value(adminUserId.toString()));
    }

    @Test
    void shouldDenyAuditQueryWithoutPermission() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User seller = createUser("seller_" + suffix, "seller_" + suffix + "@test.com", "Seller@123", "SELLER");
        String accessToken = login(seller.getLogin(), "Seller@123").path("accessToken").asText();

        mockMvc.perform(get("/api/v1/audit-logs").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    private Product createProduct() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Product product = new Product();
        product.setInternalCode("AUD-" + suffix);
        product.setSku("SKU-AUD-" + suffix);
        product.setName("Produto audit " + suffix);
        product.setCategory(category);
        product.setUnitOfMeasure("UN");
        product.setSalePrice(new BigDecimal("50.00"));
        product.setCostPrice(BigDecimal.ONE);
        product.setMinStock(BigDecimal.ZERO);
        product.setAllowNegativeStock(false);
        product.markActive();
        Product saved = productRepository.saveAndFlush(product);
        storeProductService.enable(new StoreProductEnableRequest(loja01Id, saved.getId()));
        return saved;
    }

    private User createUser(String login, String email, String password, String roleCode) {
        User user = new User();
        user.setName(login);
        user.setLogin(login);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setActive(true);
        user.setFailedLoginAttempts(0);
        user.getRoles().add(roleRepository.findByCode(roleCode).orElseThrow());
        return userRepository.saveAndFlush(user);
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

    private void authenticateAs(UUID userId) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        userId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("AUDIT_READ"),
                                new SimpleGrantedAuthority("SALE_CREATE"),
                                new SimpleGrantedAuthority("SALE_CONFIRM"),
                                new SimpleGrantedAuthority("INVENTORY_MOVE"),
                                new SimpleGrantedAuthority("CUSTOMER_CREATE"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
    }
}
