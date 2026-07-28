package br.com.systemcommerce.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

import io.jsonwebtoken.Claims;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtProperties properties;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        lenient()
                .when(properties.getSecret())
                .thenReturn("unit-test-systemcommerce-jwt-secret-key-256bits");
        lenient().when(properties.getAccessExpirationMs()).thenReturn(3_600_000L);
        lenient().when(properties.getRefreshExpirationMs()).thenReturn(86_400_000L);
        lenient().when(properties.getExpirationMs()).thenReturn(3_600_000L);
        jwtService = new JwtService(properties);
    }

    @Test
    void shouldGenerateTokenWithSubjectAndAuthorities() {
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, "user@test.com", List.of("USER_READ"));
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@test.com");
        assertThat(claims.get("type", String.class)).isEqualTo("access");
        assertThat(jwtService.isAccessToken(claims)).isTrue();
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void shouldGenerateRefreshToken() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateRefreshToken(userId, "user@test.com");
        Claims claims = jwtService.parseClaims(token);

        assertThat(jwtService.isRefreshToken(claims)).isTrue();
        assertThat(jwtService.isAccessToken(claims)).isFalse();
    }

    @Test
    void shouldRejectInvalidToken() {
        assertThat(jwtService.isValid("invalid.token.value")).isFalse();
    }
}
