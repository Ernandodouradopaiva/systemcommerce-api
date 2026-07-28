package br.com.systemcommerce.finance;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
class FinanceMasterDataModuleTest {

    private static final UUID ORG = UUID.fromString("b1000000-0000-4000-8000-000000000001");

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_finance_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

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
    }

    @Test
    void financialAccountTreeAndCycleProtection() throws Exception {
        mockMvc.perform(get("/api/v1/financial-accounts/tree")
                        .param("organizationId", ORG.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/v1/financial-accounts/postable")
                        .param("organizationId", ORG.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void costCentersSeeded() throws Exception {
        mockMvc.perform(get("/api/v1/cost-centers")
                        .param("organizationId", ORG.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void bankAccountOpeningBalanceAndPaymentConditionPercentages() throws Exception {
        MvcResult banks = mockMvc.perform(get("/api/v1/banks")
                        .param("organizationId", ORG.toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        String bankId = objectMapper
                .readTree(banks.getResponse().getContentAsString())
                .path("data")
                .get(0)
                .path("id")
                .asText();

        String suffix = String.valueOf(System.currentTimeMillis());
        MvcResult created = mockMvc.perform(post("/api/v1/bank-accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "organizationId":"%s",
                                  "code":"CC-%s",
                                  "name":"Conta teste",
                                  "bankId":"%s",
                                  "agency":"1234",
                                  "accountNumber":"56789",
                                  "accountDigit":"0",
                                  "accountKind":"CHECKING",
                                  "holderName":"SystemCommerce",
                                  "openingBalance":100.00,
                                  "openingBalanceDate":"2026-01-01",
                                  "allowsPayments":true,
                                  "allowsReceipts":true,
                                  "allowsReconciliation":true
                                }
                                """
                                        .formatted(ORG, suffix, bankId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.currentBalance").value(100.0))
                .andReturn();

        String holderId = objectMapper
                .readTree(created.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText();

        mockMvc.perform(get("/api/v1/bank-accounts/" + holderId + "/balance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(100.0));

        mockMvc.perform(post("/api/v1/payment-conditions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "organizationId":"%s",
                                  "code":"BAD-%s",
                                  "name":"Percentuais inválidos",
                                  "conditionType":"INSTALLMENTS",
                                  "installmentCount":2,
                                  "installments":[
                                    {"sequenceNo":1,"daysOffset":30,"percentage":40},
                                    {"sequenceNo":2,"daysOffset":60,"percentage":40}
                                  ]
                                }
                                """
                                        .formatted(ORG, suffix)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/payment-conditions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "organizationId":"%s",
                                  "code":"OK-%s",
                                  "name":"30/60 ok",
                                  "conditionType":"INSTALLMENTS",
                                  "installmentCount":2,
                                  "installments":[
                                    {"sequenceNo":1,"daysOffset":30,"percentage":50},
                                    {"sequenceNo":2,"daysOffset":60,"percentage":50}
                                  ]
                                }
                                """
                                        .formatted(ORG, suffix)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.installments.length()").value(2));
    }
}
