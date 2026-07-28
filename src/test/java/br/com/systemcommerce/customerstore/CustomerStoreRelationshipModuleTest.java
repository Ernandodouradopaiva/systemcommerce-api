package br.com.systemcommerce.customerstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.customer.dto.CustomerCreateRequest;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipCreateRequest;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipNotesRequest;
import br.com.systemcommerce.customerstore.service.CustomerStoreRelationshipService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
class CustomerStoreRelationshipModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_customer_store_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CustomerStoreRelationshipService relationshipService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private OrganizationService organizationService;

    private String adminToken;
    private UUID loja01Id;
    private UUID loja02Id;

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
        adminToken = objectMapper
                .readTree(login.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();

        loja01Id = findStoreId("LOJA-01");
        loja02Id = findStoreId("LOJA-02");
    }

    @Test
    void shouldCreateCustomerWithStoreRelationshipAndOriginStore() throws Exception {
        String document = randomValidCpf();

        mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomerCreateRequest(
                                Customer.CustomerType.PF,
                                "Cliente Multiloja Teste",
                                null,
                                document,
                                null,
                                "cliente@test.com",
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
                                loja01Id))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.document").value(document));

        UUID customerId = relationshipService
                .listByStore(loja01Id, null, Pageable.unpaged())
                .getContent()
                .stream()
                .filter(r -> document.equals(r.customerDocument()))
                .findFirst()
                .orElseThrow()
                .customerId();

        var origin = relationshipService.getOriginStore(customerId);
        assertThat(origin.originStoreId()).isEqualTo(loja01Id);

        mockMvc.perform(get("/api/v1/customers/{id}/origin-store", customerId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.originStoreId").value(loja01Id.toString()));
    }

    @Test
    void shouldCreateSecondStoreRelationshipAndUpdateLocalNotes() throws Exception {
        String document = randomValidCpf();
        MvcResult created = mockMvc.perform(post("/api/v1/customers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomerCreateRequest(
                                Customer.CustomerType.PF,
                                "Cliente Duas Lojas",
                                null,
                                document,
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
                                null,
                                null,
                                loja01Id))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID customerId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        relationshipService.create(
                customerId, new CustomerStoreRelationshipCreateRequest(loja02Id, null, "primeiro contato"));

        mockMvc.perform(patch("/api/v1/customers/{id}/store-relationships/{storeId}/notes", customerId, loja02Id)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CustomerStoreRelationshipNotesRequest("VIP local", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.localNotes").value("VIP local"));

        assertThat(relationshipService.listByStore(loja02Id, null, Pageable.unpaged()).getTotalElements())
                .isGreaterThanOrEqualTo(1);
    }

    private UUID findStoreId(String code) {
        return storeService
                .list(null, code, null, null, null, null, null, null, Pageable.unpaged())
                .getContent()
                .getFirst()
                .id();
    }

    private static String randomValidCpf() {
        int[] d = new int[11];
        for (int i = 0; i < 9; i++) {
            d[i] = ThreadLocalRandom.current().nextInt(0, 10);
        }
        d[9] = cpfDigit(d, 9);
        d[10] = cpfDigit(d, 10);
        StringBuilder sb = new StringBuilder(11);
        for (int value : d) {
            sb.append(value);
        }
        return sb.toString();
    }

    private static int cpfDigit(int[] digits, int length) {
        int sum = 0;
        int weight = length + 1;
        for (int i = 0; i < length; i++) {
            sum += digits[i] * (weight - i);
        }
        int mod = sum % 11;
        return mod < 2 ? 0 : 11 - mod;
    }
}
