package br.com.systemcommerce.pos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.pos.store.dto.StoreCreateRequest;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalCreateRequest;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalLinkWarehouseRequest;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseCreateRequest;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
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
class PosLocationModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_location_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StoreService storeService;

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private PosTerminalService posTerminalService;

    private String adminToken;

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
    }

    @Test
    void shouldSeedDefaultStoreWarehouseAndTerminal() throws Exception {
        mockMvc.perform(get("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("code", "LOJA-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("LOJA-01"));

        mockMvc.perform(get("/api/v1/warehouses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("search", "DEP-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("DEP-01"))
                .andExpect(jsonPath("$.data[0].allowsSale").value(true));

        mockMvc.perform(get("/api/v1/pos-terminals/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("storeId", "c1000000-0000-4000-8000-000000000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].code").value("TERM-01"))
                .andExpect(jsonPath("$.data[0].eligibleToOpenCashSession").value(true));
    }

    @Test
    void shouldCreateStoreWarehouseAndTerminalViaApi() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        MvcResult storeResult = mockMvc.perform(post("/api/v1/stores")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalStoreCreate(
                                "LJ-" + suffix, "Loja " + suffix, "America/Sao_Paulo"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.code").value("LJ-" + suffix))
                .andReturn();

        UUID storeId = UUID.fromString(objectMapper
                .readTree(storeResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        MvcResult whResult = mockMvc.perform(post("/api/v1/warehouses")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new WarehouseCreateRequest(storeId, "DEP-" + suffix, "Dep " + suffix, true))))
                .andExpect(status().isCreated())
                .andReturn();

        UUID warehouseId = UUID.fromString(objectMapper
                .readTree(whResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        mockMvc.perform(post("/api/v1/pos-terminals")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PosTerminalCreateRequest(
                                storeId,
                                warehouseId,
                                "T-" + suffix,
                                "Terminal " + suffix,
                                99,
                                "ST-" + suffix,
                                null,
                                PosTerminal.PrintModel.THERMAL_80))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.terminalNumber").value(99))
                .andExpect(jsonPath("$.data.eligibleToOpenCashSession").value(true));
    }

    @Test
    void shouldRejectDuplicateStoreCode() {
        assertThatThrownBy(() -> storeService.create(minimalStoreCreate("LOJA-01", "Duplicada", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldRejectInactiveTerminalOpeningCash() {
        var available = posTerminalService.listAvailable(null, org.springframework.data.domain.Pageable.unpaged());
        assertThat(available.getContent()).isNotEmpty();
        UUID terminalId = available.getContent().getFirst().id();

        posTerminalService.inactivate(terminalId);

        assertThatThrownBy(() -> posTerminalService.requireEligibleToOpenCashSession(terminalId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("inativo");

        posTerminalService.activate(terminalId);
    }

    @Test
    void shouldRejectWarehouseFromAnotherStore() {
        var defaultStore = storeService
                .list(null, "LOJA-01", null, null, null, null, null, null, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .getFirst();

        var otherStore = storeService.create(minimalStoreCreate(
                "LJ-OTHER-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(), "Outra Loja", null));

        var otherWh = warehouseService.create(new WarehouseCreateRequest(
                otherStore.id(), "DEP-X", "Dep X", true));

        assertThatThrownBy(() -> posTerminalService.create(new PosTerminalCreateRequest(
                        defaultStore.id(),
                        otherWh.id(),
                        "T-X-" + UUID.randomUUID().toString().substring(0, 4),
                        "Bad link",
                        77,
                        null,
                        null,
                        null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mesma loja");
    }

    @Test
    void shouldLinkTerminalToWarehouse() {
        var terminals = posTerminalService.listAvailable(null, org.springframework.data.domain.Pageable.unpaged());
        var terminal = terminals.getContent().getFirst();

        var warehouses = warehouseService.list(
                terminal.storeId(), null, true, null, org.springframework.data.domain.Pageable.unpaged());
        assertThat(warehouses.getContent()).isNotEmpty();

        var linked = posTerminalService.linkWarehouse(
                terminal.id(), new PosTerminalLinkWarehouseRequest(warehouses.getContent().getFirst().id()));
        assertThat(linked.warehouseId()).isEqualTo(warehouses.getContent().getFirst().id());
    }

    @Test
    void shouldProtectEndpointsWithoutPermission() throws Exception {
        mockMvc.perform(get("/api/v1/stores")).andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/v1/stores/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inventoryShouldUseDefaultWarehouseAfterMigration() throws Exception {
        mockMvc.perform(get("/api/v1/inventory")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.warehouseCode == 'DEP-01')]").isNotEmpty());
    }

    @Test
    void inactiveTerminalMustNotAppearInAvailable() throws Exception {
        var available = posTerminalService.listAvailable(null, org.springframework.data.domain.Pageable.unpaged());
        UUID id = available.getContent().getFirst().id();
        posTerminalService.inactivate(id);

        mockMvc.perform(get("/api/v1/pos-terminals/available")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id == '" + id + "')]").doesNotExist());

        posTerminalService.activate(id);
    }

    private static StoreCreateRequest minimalStoreCreate(String code, String name, String timezone) {
        return new StoreCreateRequest(
                null,
                code,
                name,
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
                timezone);
    }
}
