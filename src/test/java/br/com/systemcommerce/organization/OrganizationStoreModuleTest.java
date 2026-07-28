package br.com.systemcommerce.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.organization.dto.OrganizationCreateRequest;
import br.com.systemcommerce.organization.dto.OrganizationUpdateRequest;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.dto.StoreCreateRequest;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
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
class OrganizationStoreModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_org_store_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private StoreService storeService;

    private String adminToken;
    private UUID defaultOrgId;

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
        adminToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("accessToken")
                .asText();
        defaultOrgId = organizationService.getDefault().id();
    }

    @Test
    void shouldSeedDefaultOrganizationLinkedToLoja01() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/default")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("ORG-DEFAULT"));

        mockMvc.perform(get("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("code", "LOJA-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].organizationCode").value("ORG-DEFAULT"))
                .andExpect(jsonPath("$.data[0].headquarters").value(true))
                .andExpect(jsonPath("$.data[0].establishmentType").value("HEADQUARTERS"))
                .andExpect(jsonPath("$.data[0].allowsSales").value(true))
                .andExpect(jsonPath("$.data[0].allowsPos").value(true));
    }

    @Test
    void shouldCreateAndUpdateOrganizationViaApi() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        MvcResult created = mockMvc.perform(post("/api/v1/organizations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrganizationCreateRequest(
                                "ORG-" + suffix,
                                "Empresa " + suffix + " LTDA",
                                "Fantasia " + suffix,
                                null,
                                null,
                                null,
                                "contato@" + suffix.toLowerCase() + ".test",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                "Curitiba",
                                "PR",
                                "America/Sao_Paulo",
                                "BRL"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("ORG-" + suffix))
                .andReturn();

        UUID orgId = UUID.fromString(objectMapper
                .readTree(created.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        mockMvc.perform(put("/api/v1/organizations/" + orgId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrganizationUpdateRequest(
                                "ORG-" + suffix,
                                "Empresa " + suffix + " LTDA Atualizada",
                                "Fantasia Nova",
                                null,
                                null,
                                null,
                                "novo@" + suffix.toLowerCase() + ".test",
                                null,
                                "https://example.com",
                                null,
                                null,
                                null,
                                null,
                                null,
                                "Curitiba",
                                "PR",
                                "America/Sao_Paulo",
                                "BRL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.legalName").value("Empresa " + suffix + " LTDA Atualizada"))
                .andExpect(jsonPath("$.data.website").value("https://example.com"));
    }

    @Test
    void shouldSupportMultipleStoresAndUniqueCodePerOrganization() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var branch = storeService.create(storeRequest(null, "FIL-" + suffix, "Filial " + suffix, false));
        assertThat(branch.organizationId()).isEqualTo(defaultOrgId);
        assertThat(branch.headquarters()).isFalse();

        assertThatThrownBy(() -> storeService.create(storeRequest(null, "FIL-" + suffix, "Duplicada", false)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Código");
    }

    @Test
    void shouldRejectSecondHeadquartersUnlessConfigured() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var org = organizationService.create(new OrganizationCreateRequest(
                "ORG-HQ-" + suffix,
                "Org HQ " + suffix,
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
                "America/Sao_Paulo",
                "BRL"));
        storeService.create(storeRequest(org.id(), "HQ1-" + suffix, "Matriz " + suffix, true));
        assertThatThrownBy(() -> storeService.create(storeRequest(org.id(), "HQ2-" + suffix, "Outra Matriz", true)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("matriz");
    }

    @Test
    void shouldDefineHeadquartersAndExposeOperationalAndSummary() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var branch = storeService.create(storeRequest(null, "OP-" + suffix, "Operacional " + suffix, false));

        mockMvc.perform(patch("/api/v1/stores/" + branch.id() + "/headquarters")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.headquarters").value(true))
                .andExpect(jsonPath("$.data.establishmentType").value("HEADQUARTERS"));

        var formerHq = storeService
                .list(null, "LOJA-01", null, null, null, null, null, null, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .getFirst();
        assertThat(formerHq.headquarters()).isFalse();

        mockMvc.perform(get("/api/v1/stores/operational")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.code == 'OP-" + suffix + "')]").exists());

        mockMvc.perform(get("/api/v1/stores/" + branch.id() + "/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("OP-" + suffix))
                .andExpect(jsonPath("$.data.openCashSessionCount").value(0));

        // restaura LOJA-01 como matriz para não impactar outros testes no mesmo container
        storeService.defineHeadquarters(formerHq.id());
    }

    @Test
    void shouldBlockInactivationWhenAllowsSalesDisabledStillBlocksOpenCashViaRequire() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        var store = storeService.create(storeRequest(null, "IN-" + suffix, "Inativar " + suffix, false));
        storeService.inactivate(store.id());

        assertThatThrownBy(() -> storeService.requireUsable(store.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativa");

        assertThatThrownBy(() -> storeService.requireAllowsPos(store.id()))
                .isInstanceOf(BusinessRuleException.class);

        storeService.activate(store.id());
        var updated = storeService.update(
                store.id(),
                new br.com.systemcommerce.pos.store.dto.StoreUpdateRequest(
                        store.code(),
                        store.name(),
                        store.tradeName(),
                        store.document(),
                        store.stateRegistration(),
                        store.municipalRegistration(),
                        Store.EstablishmentType.BRANCH,
                        false,
                        store.openingDate(),
                        false,
                        false,
                        store.email(),
                        store.phone(),
                        store.zipCode(),
                        store.street(),
                        store.number(),
                        store.complement(),
                        store.district(),
                        store.city(),
                        store.state(),
                        store.timezone()));

        assertThat(updated.allowsSales()).isFalse();
        assertThatThrownBy(() -> storeService.requireAllowsSales(store.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vendas");
        assertThatThrownBy(() -> storeService.requireAllowsPos(store.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PDV");
    }

    @Test
    void shouldRejectDuplicateDocument() {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String cnpj = "112223330001" + (Math.abs(suffix.hashCode()) % 90 + 10);
        storeService.create(new StoreCreateRequest(
                null,
                "DOC-" + suffix,
                "Com CNPJ " + suffix,
                null,
                cnpj,
                null,
                null,
                Store.EstablishmentType.BRANCH,
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

        assertThatThrownBy(() -> storeService.create(new StoreCreateRequest(
                        null,
                        "DOC2-" + suffix,
                        "Outro CNPJ",
                        null,
                        cnpj,
                        null,
                        null,
                        Store.EstablishmentType.BRANCH,
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
                        null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CNPJ");
    }

    @Test
    void shouldProtectOrganizationEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/organizations/default")).andExpect(status().isUnauthorized());
    }

    private static StoreCreateRequest storeRequest(UUID orgId, String code, String name, boolean headquarters) {
        return new StoreCreateRequest(
                orgId,
                code,
                name,
                null,
                null,
                null,
                null,
                headquarters ? Store.EstablishmentType.HEADQUARTERS : Store.EstablishmentType.BRANCH,
                headquarters,
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
                "America/Sao_Paulo");
    }
}
