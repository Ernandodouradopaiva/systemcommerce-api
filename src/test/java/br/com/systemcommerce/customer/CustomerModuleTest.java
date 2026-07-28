package br.com.systemcommerce.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.repository.CustomerRepository;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.sale.dto.SaleCreateRequest;
import br.com.systemcommerce.sale.service.SaleService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.jdbc.core.JdbcTemplate;
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
class CustomerModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_customer_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private SaleService saleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    private String adminToken;
    private UUID adminUserId;
    private UUID loja01Id;
    private UUID dep01Id;

    @BeforeEach
    void loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        var loginJson = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        adminToken = loginJson.path("accessToken").asText();
        adminUserId = UUID.fromString(loginJson.path("user").path("id").asText());
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
    void shouldCreatePfCustomerAndEnforceUniqueness() throws Exception {
        String payload =
                """
                {
                  "type":"PF",
                  "name":"Ana Nova",
                  "document":"11144477735",
                  "email":"ana.nova@example.com",
                  "phone":"11999991111",
                  "mobile":"11988882222",
                  "birthDate":"1992-01-15",
                  "notes":"Cliente teste",
                  "zipCode":"01310-100",
                  "street":"Av. Paulista",
                  "number":"100",
                  "district":"Bela Vista",
                  "city":"São Paulo",
                  "state":"sp"
                }
                """;

        MvcResult created = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.document").value("11144477735"))
                .andExpect(jsonPath("$.data.state").value("SP"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andReturn();

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        UUID id = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        Integer audits = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM audit_logs WHERE entity_name = 'Customer' AND entity_id = ?",
                Integer.class,
                id);
        assertThat(audits).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldRejectIncompatibleDocumentTypeAndInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type":"PF",
                                  "name":"Inválido",
                                  "document":"11222333000181",
                                  "email":"ok@example.com"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"));

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type":"PF",
                                  "name":"E-mail ruim",
                                  "document":"15350946056",
                                  "email":"sem-arroba"
                                }
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("E-mail inválido"));
    }

    @Test
    void shouldFilterByNameDocumentAndStatus() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                        .param("name", "Maria")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Maria Silva"));

        mockMvc.perform(get("/api/v1/customers")
                        .param("document", "11222333000181")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("PJ"));

        mockMvc.perform(get("/api/v1/customers")
                        .param("status", "INACTIVE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("INACTIVE"));
    }

    @Test
    void shouldActivateInactivateAndBlockInactiveForSale() throws Exception {
        Customer maria = customerRepository.findByDocument("52998224725").orElseThrow();

        mockMvc.perform(patch("/api/v1/customers/" + maria.getId() + "/inactivate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("INACTIVE"));

        assertThatThrownBy(() -> customerService.requireUsableForSale(maria.getId()))
                .isInstanceOf(BusinessRuleException.class);

        mockMvc.perform(patch("/api/v1/customers/" + maria.getId() + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        assertThat(customerService.requireUsableForSale(maria.getId()).isUsableForSale()).isTrue();
    }

    @Test
    void shouldLogicallyDeleteWhenLinkedToSale() throws Exception {
        Customer customer = customerRepository.findByDocument("11222333000181").orElseThrow();
        customer.markActive();
        customerRepository.saveAndFlush(customer);

        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        adminUserId.toString(),
                        null,
                        java.util.List.of(
                                new SimpleGrantedAuthority("SALE_CREATE"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        saleService.createDraft(new SaleCreateRequest(
                loja01Id, dep01Id, customer.getId(), null, null, "vínculo soft delete"));
        SecurityContextHolder.clearContext();

        mockMvc.perform(delete("/api/v1/customers/" + customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        Customer reloaded = customerRepository.findById(customer.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(Customer.CustomerStatus.INACTIVE);
        assertThat(reloaded.getActive()).isFalse();
    }

    @Test
    void shouldUpdateCustomer() throws Exception {
        Customer customer = customerRepository.findByDocument("34028316000103").orElseThrow();

        mockMvc.perform(put("/api/v1/customers/" + customer.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "type":"PJ",
                                  "name":"Comércio Mineiro Atualizado SA",
                                  "tradeName":"Mineiro Store+",
                                  "document":"34028316000103",
                                  "stateRegistration":"ISENTO",
                                  "email":"novo@mineiro.example.com",
                                  "phone":"3132221100",
                                  "mobile":"31999991111",
                                  "notes":"Atualizado",
                                  "zipCode":"30130000",
                                  "street":"Av. Afonso Pena",
                                  "number":"1500",
                                  "district":"Centro",
                                  "city":"Belo Horizonte",
                                  "state":"MG"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Comércio Mineiro Atualizado SA"))
                .andExpect(jsonPath("$.data.email").value("novo@mineiro.example.com"));
    }
}
