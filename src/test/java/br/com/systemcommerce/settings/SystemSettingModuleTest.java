package br.com.systemcommerce.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.settings.dto.SystemSettingUpsertRequest;
import br.com.systemcommerce.settings.entity.SystemSettingKeys;
import br.com.systemcommerce.settings.entity.SystemSettingScope;
import br.com.systemcommerce.settings.service.SystemSettingService;
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
class SystemSettingModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_system_settings_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SystemSettingService systemSettingService;

    @Autowired
    private StoreService storeService;

    @Autowired
    private OrganizationService organizationService;

    private String adminToken;
    private UUID orgId;
    private UUID loja01Id;
    private UUID loja02Id;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult login = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
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

        orgId = organizationService.getDefault().id();
        loja01Id = findStoreId("LOJA-01");
        loja02Id = findStoreId("LOJA-02");
    }

    @Test
    void shouldResolvePriorityTerminalOverStoreOverStoreGroupOverOrganization() {
        withSecurity(() -> {
            systemSettingService.upsert(new SystemSettingUpsertRequest(
                    SystemSettingKeys.REQUIRE_SELLER,
                    SystemSettingScope.ORGANIZATION,
                    orgId,
                    null,
                    null,
                    null,
                    null,
                    "false",
                    null));
            systemSettingService.upsert(new SystemSettingUpsertRequest(
                    SystemSettingKeys.REQUIRE_SELLER,
                    SystemSettingScope.STORE,
                    null,
                    null,
                    loja01Id,
                    null,
                    null,
                    "true",
                    null));

            var storeEffective = systemSettingService.resolveEffective(
                    SystemSettingKeys.REQUIRE_SELLER, orgId, null, loja01Id, null, null);
            assertThat(storeEffective.value()).isEqualTo("true");
            assertThat(storeEffective.resolvedFrom()).isEqualTo(SystemSettingScope.STORE);

            var orgEffective = systemSettingService.resolveEffective(
                    SystemSettingKeys.REQUIRE_SELLER, orgId, null, loja02Id, null, null);
            assertThat(orgEffective.value()).isEqualTo("false");
            assertThat(orgEffective.resolvedFrom()).isEqualTo(SystemSettingScope.ORGANIZATION);
        });
    }

    @Test
    void shouldRejectUserScopeForCommercialKey() {
        withSecurity(() -> assertThatThrownBy(() -> systemSettingService.upsert(new SystemSettingUpsertRequest(
                        SystemSettingKeys.REQUIRE_SELLER,
                        SystemSettingScope.USER,
                        null,
                        null,
                        null,
                        null,
                        UUID.randomUUID(),
                        "true",
                        null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("não permite escopo USER"));
    }

    @Test
    void shouldRestoreStoreInheritanceViaApi() throws Exception {
        withSecurity(() -> systemSettingService.upsert(new SystemSettingUpsertRequest(
                SystemSettingKeys.ALLOW_NEGATIVE_STOCK,
                SystemSettingScope.STORE,
                null,
                null,
                loja01Id,
                null,
                null,
                "true",
                null)));

        mockMvc.perform(delete("/api/v1/system-settings/inheritance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("storeId", loja01Id.toString())
                        .param("settingKey", SystemSettingKeys.ALLOW_NEGATIVE_STOCK))
                .andExpect(status().isNoContent());

        withSecurity(() -> {
            var effective = systemSettingService.resolveEffective(
                    SystemSettingKeys.ALLOW_NEGATIVE_STOCK, orgId, null, loja01Id, null, null);
            assertThat(effective.resolvedFrom()).isEqualTo(SystemSettingScope.ORGANIZATION);
        });
    }

    @Test
    void shouldExposeEffectiveViaHttp() throws Exception {
        mockMvc.perform(get("/api/v1/system-settings/effective")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("settingKey", SystemSettingKeys.REQUIRE_SELLER)
                        .param("organizationId", orgId.toString())
                        .param("storeId", loja02Id.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingKey").value(SystemSettingKeys.REQUIRE_SELLER));
    }

    private UUID findStoreId(String code) {
        return storeService
                .list(null, code, null, null, null, null, null, null, Pageable.unpaged())
                .getContent()
                .getFirst()
                .id();
    }

    private void withSecurity(Runnable action) {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(
                        "admin",
                        "n/a",
                        java.util.List.of(
                                new SimpleGrantedAuthority("SYSTEM_SETTING_READ"),
                                new SimpleGrantedAuthority("SYSTEM_SETTING_MANAGE"),
                                new SimpleGrantedAuthority("ROLE_ADMIN"),
                                new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
