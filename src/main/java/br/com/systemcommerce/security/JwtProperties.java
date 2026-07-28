package br.com.systemcommerce.security;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.security.jwt")
public class JwtProperties {

    private static final String DEFAULT_SECRET =
            "change-me-systemcommerce-jwt-secret-key-min-256-bits-long";

    private String secret = DEFAULT_SECRET;
    private long accessExpirationMs = 900_000L;
    private long refreshExpirationMs = 604_800_000L;
    /** @deprecated use accessExpirationMs */
    private long expirationMs = 900_000L;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = StringUtils.hasText(secret) ? secret : DEFAULT_SECRET;
    }

    public long getAccessExpirationMs() {
        return accessExpirationMs > 0 ? accessExpirationMs : expirationMs;
    }

    public void setAccessExpirationMs(long accessExpirationMs) {
        if (accessExpirationMs > 0) {
            this.accessExpirationMs = accessExpirationMs;
            this.expirationMs = accessExpirationMs;
        }
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    public void setRefreshExpirationMs(long refreshExpirationMs) {
        if (refreshExpirationMs > 0) {
            this.refreshExpirationMs = refreshExpirationMs;
        }
    }

    public long getExpirationMs() {
        return getAccessExpirationMs();
    }

    public void setExpirationMs(long expirationMs) {
        if (expirationMs > 0) {
            this.expirationMs = expirationMs;
            this.accessExpirationMs = expirationMs;
        }
    }

    @PostConstruct
    void ensureSecret() {
        if (!StringUtils.hasText(secret)) {
            this.secret = DEFAULT_SECRET;
        }
        int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
        if (bytes < 32) {
            throw new IllegalStateException(
                    "app.security.jwt.secret deve ter pelo menos 32 bytes (atual=" + bytes + ")");
        }
    }
}
