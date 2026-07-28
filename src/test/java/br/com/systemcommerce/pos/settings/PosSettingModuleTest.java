package br.com.systemcommerce.pos.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.pos.settings.dto.PosSettingUpsertRequest;
import br.com.systemcommerce.pos.settings.dto.PosSettingValidateRequest;
import br.com.systemcommerce.pos.settings.entity.PosSettingKeys;
import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import br.com.systemcommerce.pos.settings.service.PosSettingService;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
class PosSettingModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_pos_settings_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PosSettingService posSettingService;

    @Autowired
    private PosTerminalService posTerminalService;

    private String adminToken;
    private UUID adminUserId;
    private UUID storeId;
    private UUID terminalId;

    @BeforeEach
    void setUp() throws Exception {
        MvcResult login = mockMvc.perform(postLogin())
                .andExpect(status().isOk())
                .andReturn();
        var data = objectMapper.readTree(login.getResponse().getContentAsString()).path("data");
        adminToken = data.path("accessToken").asText();
        adminUserId = UUID.fromString(data.path("user").path("id").asText());

        withSecurity(() -> {
            var available = posTerminalService.listAvailable(null, Pageable.unpaged());
            var terminal = available.getContent().stream()
                    .filter(t -> "TERM-01".equals(t.code()))
                    .findFirst()
                    .orElseGet(() -> available.getContent().getFirst());
            terminalId = terminal.id();
            storeId = terminal.storeId();
        });
    }

    @Test
    void shouldResolveHierarchyTerminalOverStoreOverGlobal() {
        withSecurity(() -> {
            posSettingService.upsert(new PosSettingUpsertRequest(
                    PosSettingKeys.PRINT_COPIES, PosSettingScope.GLOBAL, null, null, "1", "global", null));
            posSettingService.upsert(new PosSettingUpsertRequest(
                    PosSettingKeys.PRINT_COPIES, PosSettingScope.STORE, storeId, null, "2", "loja", null));
            posSettingService.upsert(new PosSettingUpsertRequest(
                    PosSettingKeys.PRINT_COPIES,
                    PosSettingScope.TERMINAL,
                    storeId,
                    terminalId,
                    "3",
                    "terminal",
                    null));

            var effective = posSettingService.resolveEffective(storeId, terminalId);
            var copies = effective.settings().stream()
                    .filter(s -> PosSettingKeys.PRINT_COPIES.equals(s.settingKey()))
                    .findFirst()
                    .orElseThrow();
            assertThat(copies.value()).isEqualTo("3");
            assertThat(copies.resolvedFrom()).isEqualTo(PosSettingScope.TERMINAL);

            var storeOnly = posSettingService.resolveEffective(storeId, null);
            var storeCopies = storeOnly.settings().stream()
                    .filter(s -> PosSettingKeys.PRINT_COPIES.equals(s.settingKey()))
                    .findFirst()
                    .orElseThrow();
            assertThat(storeCopies.value()).isEqualTo("2");
            assertThat(storeCopies.resolvedFrom()).isEqualTo(PosSettingScope.STORE);
        });
    }

    @Test
    void shouldRejectInvalidValue() {
        withSecurity(() -> {
            var result = posSettingService.validate(new PosSettingValidateRequest(
                    PosSettingKeys.PRINT_COPIES, PosSettingScope.GLOBAL, null, null, "99"));
            assertThat(result.valid()).isFalse();

            assertThatThrownBy(() -> posSettingService.upsert(new PosSettingUpsertRequest(
                            PosSettingKeys.PRINTER_WIDTH,
                            PosSettingScope.GLOBAL,
                            null,
                            null,
                            "70",
                            null,
                            null)))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("não permitido");
        });
    }

    @Test
    void shouldAuditAndListHistoryViaApi() throws Exception {
        withSecurity(() -> posSettingService.upsert(new PosSettingUpsertRequest(
                PosSettingKeys.RECEIPT_FOOTER_MESSAGE,
                PosSettingScope.STORE,
                storeId,
                null,
                "Volte sempre!",
                "msg loja",
                null)));

        mockMvc.perform(get("/api/v1/pos/settings/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("settingKey", PosSettingKeys.RECEIPT_FOOTER_MESSAGE)
                        .param("storeId", storeId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/v1/pos/settings/effective")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("storeId", storeId.toString())
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settings").isArray());
    }

    @Test
    void shouldUpsertViaApi() throws Exception {
        var body = new PosSettingUpsertRequest(
                PosSettingKeys.SOUNDS_ENABLED, PosSettingScope.GLOBAL, null, null, "false", "desligar sons", null);
        mockMvc.perform(put("/api/v1/pos/settings")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.value").value("false"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder postLogin() {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                        {"username":"admin","password":"Admin@123"}
                        """);
    }

    private void withSecurity(Runnable action) {
        withSecurity(() -> {
            action.run();
            return null;
        });
    }

    private <T> T withSecurity(java.util.concurrent.Callable<T> action) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            adminUserId.toString(),
                            null,
                            List.of(
                                    new SimpleGrantedAuthority("POS_SETTINGS_READ"),
                                    new SimpleGrantedAuthority("POS_SETTINGS_MANAGE"),
                                    new SimpleGrantedAuthority("POS_TERMINAL_MANAGE"),
                                    new SimpleGrantedAuthority("POS_TERMINAL_READ"),
                                    new SimpleGrantedAuthority("GLOBAL_STORE_ACCESS"))));
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
