package br.com.systemcommerce.multistore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.employee.dto.EmployeeCreateRequest;
import br.com.systemcommerce.employee.entity.Employee;
import br.com.systemcommerce.employee.service.EmployeeService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.dto.StoreCreateRequest;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.seller.dto.SellerEnableRequest;
import br.com.systemcommerce.seller.dto.SellerStoreAuthorizeRequest;
import br.com.systemcommerce.seller.service.SellerService;
import br.com.systemcommerce.shared.exception.BusinessException;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.storeaccess.dto.UserStoreAccessGrantRequest;
import br.com.systemcommerce.storeaccess.entity.UserStoreAccess;
import br.com.systemcommerce.storeaccess.service.StoreAccessService;
import br.com.systemcommerce.storeaccess.service.StoreAuthorizationEvaluator;
import br.com.systemcommerce.storecontext.CurrentStoreContext;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SellerStoreAccessContextModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_seller_access_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
    private StoreAccessService storeAccessService;

    @Autowired
    private StoreAuthorizationEvaluator storeAuthorizationEvaluator;

    @Autowired
    private SaleService saleService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID loja01Id;
    private UUID dep01Id;
    private UUID defaultOrgId;
    private UUID adminUserId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
        defaultOrgId = organizationService.getDefault().id();
        loja01Id = storeService
                .list(null, "LOJA-01", null, null, null, null, null, null, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .getFirst()
                .id();
        dep01Id = warehouseService.list(loja01Id, null, null, null, Pageable.unpaged()).stream()
                .filter(w -> "DEP-01".equals(w.code()))
                .findFirst()
                .orElseThrow()
                .id();
        adminUserId = userRepository.findByLoginIgnoreCase("admin").orElseThrow().getId();
        asAdmin();
    }

    @Test
    void shouldSeedSellerAndBlockSaleOutsideAuthorizedStore() {
        var sellers = sellerService.listByStore(loja01Id);
        assertThat(sellers).anyMatch(s -> "VEND-0001".equals(s.sellerCode()));

        UUID sellerId = sellers.stream()
                .filter(s -> "VEND-0001".equals(s.sellerCode()))
                .findFirst()
                .orElseThrow()
                .id();

        var otherStore = storeService.create(new StoreCreateRequest(
                defaultOrgId,
                "LJ-S-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                "Loja Sem Auth",
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                true,
                true,
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

        assertThatThrownBy(() -> sellerService.requireAuthorizedForSale(sellerId, otherStore.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não autorizado");

        var sale = saleService.createDraft(
                new SaleCreateRequest(loja01Id, dep01Id, null, sellerId, null, "com vendedor"));
        assertThat(sale.sellerProfileId()).isEqualTo(sellerId);
        assertThat(sale.sellerCode()).isEqualTo("VEND-0001");
    }

    @Test
    void shouldPreserveSellerOnSaleAndDisableBlocksNewSale() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var employee = employeeService.create(new EmployeeCreateRequest(
                defaultOrgId,
                "SV-" + suffix,
                "Seller Emp " + suffix,
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
                employee.id(),
                "VX-" + suffix,
                new BigDecimal("5.00"),
                false,
                false,
                null, null, null, null));
        sellerService.authorizeStore(
                seller.id(),
                new SellerStoreAuthorizeRequest(
                        loja01Id, LocalDate.now(), null, true, false, true, null, null, null));

        var draft = saleService.createDraft(
                new SaleCreateRequest(loja01Id, dep01Id, null, seller.id(), null, null));
        assertThat(draft.sellerProfileId()).isEqualTo(seller.id());

        sellerService.disable(seller.id());
        assertThatThrownBy(() -> sellerService.requireAuthorizedForSale(seller.id(), loja01Id))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");

        // venda anterior preserva o perfil
        assertThat(saleService.getById(draft.id()).sellerProfileId()).isEqualTo(seller.id());
    }

    @Test
    void shouldIsolateUserStoreAccessAndValidateHeaderContext() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        User limited = new User();
        limited.setName("Limited " + suffix);
        limited.setEmail("lim-" + suffix.toLowerCase() + "@test.local");
        limited.setLogin("lim" + suffix.toLowerCase());
        limited.setPasswordHash(passwordEncoder.encode("Admin@123"));
        limited.setStatus(User.UserStatus.ACTIVE);
        limited = userRepository.save(limited);
        final UUID limitedUserId = limited.getId();

        var otherStore = storeService.create(new StoreCreateRequest(
                defaultOrgId,
                "LJ-A-" + suffix,
                "Loja Access " + suffix,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                true,
                true,
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

        storeAccessService.grant(new UserStoreAccessGrantRequest(
                limitedUserId,
                loja01Id,
                LocalDate.now(),
                null,
                true,
                UserStoreAccess.AccessType.PERMANENT,
                "teste"));

        assertThat(storeAuthorizationEvaluator.canAccessStore(limitedUserId, loja01Id)).isTrue();
        assertThat(storeAuthorizationEvaluator.canAccessStore(limitedUserId, otherStore.id())).isFalse();

        assertThatThrownBy(() -> storeAuthorizationEvaluator.assertCanAccess(limitedUserId, otherStore.id()))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STORE_ACCESS_DENIED);

        mockMvc.perform(get("/api/v1/store-access/accessible-stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("X-Store-Id", loja01Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.storeCode == 'LOJA-01')]").exists());

        mockMvc.perform(get("/api/v1/store-access/accessible-stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("X-Store-Id", UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectInvalidStoreHeader() throws Exception {
        mockMvc.perform(get("/api/v1/store-access/context/current")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .header("X-Store-Id", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("STORE_CONTEXT_INVALID"));
    }

    @Test
    void shouldResolveCurrentStoreContextFromDefault() {
        asAdmin();
        CurrentStoreContext def = storeAuthorizationEvaluator.resolveDefaultContext(adminUserId);
        // admin tem GLOBAL; default context pode ser empty
        assertThat(storeAuthorizationEvaluator.hasGlobalAccess()).isTrue();
        assertThat(storeAccessService.listAccessibleStores(null)).isNotEmpty();
    }

    private void asAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        "n/a",
                        java.util.List.of(
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"),
                                new SimpleGrantedAuthority("SELLER_READ"),
                                new SimpleGrantedAuthority("SELLER_CREATE"),
                                new SimpleGrantedAuthority("SELLER_UPDATE"),
                                new SimpleGrantedAuthority("SELLER_ASSIGN_STORE"),
                                new SimpleGrantedAuthority("SELLER_AUTHORIZE_OTHER_STORE"),
                                new SimpleGrantedAuthority("USER_STORE_ACCESS_MANAGE"),
                                new SimpleGrantedAuthority("STORE_CONTEXT_SWITCH"),
                                new SimpleGrantedAuthority("SALE_CREATE"))));
    }
}
