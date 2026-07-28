package br.com.systemcommerce.security;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Fail-fast de configurações inseguras em produção. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductionSecurityValidator {

    static final String INSECURE_DEFAULT_JWT =
            "change-me-systemcommerce-jwt-secret-key-min-256-bits-long";

    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final Environment environment;

    @PostConstruct
    void validate() {
        validateSecretStrength(jwtProperties.getSecret());

        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            return;
        }

        if (INSECURE_DEFAULT_JWT.equals(jwtProperties.getSecret())) {
            throw new IllegalStateException(
                    "JWT_SECRET inseguro em produção. Defina um segredo aleatório com pelo menos 32 bytes.");
        }

        if (corsProperties.getAllowedOrigins() == null || corsProperties.getAllowedOrigins().isEmpty()) {
            throw new IllegalStateException(
                    "CORS_ALLOWED_ORIGINS obrigatório em produção (lista explícita de origens).");
        }

        if (corsProperties.getAllowedOrigins().stream().anyMatch(o -> "*".equals(o) || o.contains("*"))) {
            throw new IllegalStateException("CORS com wildcard (*) não é permitido em produção.");
        }

        boolean adminSeed =
                environment.getProperty("app.seed.admin.enabled", Boolean.class, false);
        String adminPassword = environment.getProperty("app.seed.admin.password", "");
        if (adminSeed && ("Admin@123".equals(adminPassword) || !StringUtils.hasText(adminPassword))) {
            throw new IllegalStateException(
                    "ADMIN_SEED_ENABLED com senha padrão/frágil não é permitido em produção.");
        }

        log.info(
                "Validação de segurança de produção OK (profiles={})",
                Arrays.toString(environment.getActiveProfiles()));
    }

    static void validateSecretStrength(String secret) {
        if (!StringUtils.hasText(secret)) {
            throw new IllegalStateException("JWT secret não configurado");
        }
        int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 32) {
            throw new IllegalStateException(
                    "JWT secret deve ter pelo menos 32 bytes (atual=" + bytes + ")");
        }
    }
}
