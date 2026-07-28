package br.com.systemcommerce.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.systemcommerce.security.AuthProperties;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
class AuthModuleTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("systemcommerce_auth_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthProperties authProperties;

    @BeforeEach
    void resetAdminLock() {
        userRepository.findByEmailIgnoreCase("admin@systemcommerce.local").ifPresent(admin -> {
            admin.setStatus(User.UserStatus.ACTIVE);
            admin.setActive(true);
            admin.setFailedLoginAttempts(0);
            admin.setLockedUntil(null);
            admin.setPasswordHash(passwordEncoder.encode("Admin@123"));
            userRepository.save(admin);
        });
    }

    @Test
    void shouldLoginWithValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.login").value("admin"))
                .andExpect(jsonPath("$.data.user.permissions").isArray());
    }

    @Test
    void shouldRejectInvalidLogin() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void shouldRejectBlockedUser() throws Exception {
        User admin = userRepository.findByEmailIgnoreCase("admin@systemcommerce.local").orElseThrow();
        admin.setStatus(User.UserStatus.BLOCKED);
        userRepository.save(admin);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {"username":"admin","password":"Admin@123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciais inválidas"));
    }

    @Test
    void shouldRefreshAndRotateToken() throws Exception {
        JsonNode login = loginAsAdmin();
        String refreshToken = login.get("refreshToken").asText();

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String newRefresh = objectMapper
                .readTree(refreshResult.getResponse().getContentAsString())
                .path("data")
                .path("refreshToken")
                .asText();
        assertThat(newRefresh).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void shouldRevokeRefreshTokenOnLogout() throws Exception {
        JsonNode login = loginAsAdmin();
        String refreshToken = login.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowAccessWithPermission() throws Exception {
        String accessToken = loginAsAdmin().get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void shouldDenyAccessWithoutPermission() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User seller = createUser("seller_" + suffix, "seller_" + suffix + "@test.com", "Seller@123", "SELLER");
        String accessToken = login(seller.getLogin(), "Seller@123").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/users").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void shouldRejectExpiredAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + buildExpiredAccessToken()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
    }

    @Test
    void shouldCreateUserAndBlockUnblock() throws Exception {
        String adminToken = loginAsAdmin().get("accessToken").asText();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String login = "gerente_" + suffix;

        MvcResult createResult = mockMvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(
                                """
                                {
                                  "name":"Gerente Teste",
                                  "email":"%s@test.com",
                                  "login":"%s",
                                  "password":"Gerente@123",
                                  "roleCodes":["MANAGER"]
                                }
                                """
                                        .formatted(login, login)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.login").value(login))
                .andReturn();

        UUID userId = UUID.fromString(objectMapper
                .readTree(createResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asText());

        mockMvc.perform(patch("/api/v1/users/" + userId + "/block")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("BLOCKED"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + login + "\",\"password\":\"Gerente@123\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(patch("/api/v1/users/" + userId + "/unblock")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }

    private JsonNode loginAsAdmin() throws Exception {
        return login("admin", "Admin@123");
    }

    private JsonNode login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
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
        return userRepository.save(user);
    }

    private String buildExpiredAccessToken() {
        br.com.systemcommerce.security.JwtProperties props = new br.com.systemcommerce.security.JwtProperties();
        props.setSecret("test-systemcommerce-jwt-secret-key-min-256-bits-long");
        // build manually with past expiry using same secret padding as JwtService
        byte[] keyBytes = props.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        javax.crypto.SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
        java.util.Date now = new java.util.Date();
        return io.jsonwebtoken.Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("email", "x@test.com")
                .claim("authorities", java.util.List.of("USER_READ"))
                .claim("type", "access")
                .issuedAt(new java.util.Date(now.getTime() - 120_000))
                .expiration(new java.util.Date(now.getTime() - 60_000))
                .signWith(key)
                .compact();
    }
}
