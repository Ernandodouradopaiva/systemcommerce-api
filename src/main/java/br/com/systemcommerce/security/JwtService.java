package br.com.systemcommerce.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    public static final String CLAIM_TYPE = "type";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_AUTHORITIES = "authorities";
    public static final String CLAIM_ACCESS_VERSION = "av";
    public static final String CLAIM_SESSION_ID = "sid";
    public static final String CLAIM_ORG_ID = "orgId";
    public static final String CLAIM_STORE_ID = "storeId";
    public static final String CLAIM_API = "api";
    public static final String CLAIM_ORG = "org";
    public static final String CLAIM_SCOPES = "scopes";
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    public static final String TYPE_PUBLIC_ACCESS = "public_access";
    public static final String API_PUBLIC = "public";

    private final JwtProperties properties;
    private final SecretKey secretKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.security.jwt.secret não configurado. Defina JWT_SECRET ou a propriedade correspondente.");
        }
        this.secretKey = buildKey(secret);
    }

    public String generateAccessToken(UUID userId, String email, List<String> authorities) {
        return generateAccessToken(userId, email, authorities, 0L, null, null, null);
    }

    public String generateAccessToken(UUID userId, String email, List<String> authorities, long accessVersion) {
        return generateAccessToken(userId, email, authorities, accessVersion, null, null, null);
    }

    /**
     * Token enxuto: identidade + versão de acesso + sessão/contexto.
     * Authorities no claim são dica de UX; a fonte da verdade é DB + {@code av}.
     */
    public String generateAccessToken(
            UUID userId,
            String email,
            List<String> authorities,
            long accessVersion,
            UUID sessionId,
            UUID organizationId,
            UUID storeId) {
        return buildToken(
                userId,
                email,
                authorities,
                accessVersion,
                sessionId,
                organizationId,
                storeId,
                TYPE_ACCESS,
                properties.getAccessExpirationMs());
    }

    public String generateRefreshToken(UUID userId, String email) {
        return buildToken(
                userId, email, List.of(), 0L, null, null, null, TYPE_REFRESH, properties.getRefreshExpirationMs());
    }

    /** Access token da API pública (Prompt 81) — subject = clientId. */
    public String generatePublicAccessToken(String clientId, UUID organizationId, String scopes, long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(clientId)
                .claim(CLAIM_TYPE, TYPE_PUBLIC_ACCESS)
                .claim(CLAIM_API, API_PUBLIC)
                .claim(CLAIM_ORG, organizationId.toString())
                .claim(CLAIM_SCOPES, scopes)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public boolean isPublicAccessToken(Claims claims) {
        return TYPE_PUBLIC_ACCESS.equals(claims.get(CLAIM_TYPE, String.class))
                && API_PUBLIC.equals(claims.get(CLAIM_API, String.class));
    }

    /** Compatibilidade com testes/código legado. */
    public String generateToken(UUID userId, String email, List<String> authorities) {
        return generateAccessToken(userId, email, authorities);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean isAccessToken(Claims claims) {
        return TYPE_ACCESS.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isRefreshToken(Claims claims) {
        return TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class));
    }

    public boolean isExpired(String token) {
        try {
            parseClaims(token);
            return false;
        } catch (ExpiredJwtException ex) {
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    public long getAccessExpirationMs() {
        return properties.getAccessExpirationMs();
    }

    public long getRefreshExpirationMs() {
        return properties.getRefreshExpirationMs();
    }

    private String buildToken(
            UUID userId,
            String email,
            List<String> authorities,
            long accessVersion,
            UUID sessionId,
            UUID organizationId,
            UUID storeId,
            String type,
            long expirationMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId.toString())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_AUTHORITIES, authorities)
                .claim(CLAIM_ACCESS_VERSION, accessVersion)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry);
        if (sessionId != null) {
            builder.claim(CLAIM_SESSION_ID, sessionId.toString());
        }
        if (organizationId != null) {
            builder.claim(CLAIM_ORG_ID, organizationId.toString());
        }
        if (storeId != null) {
            builder.claim(CLAIM_STORE_ID, storeId.toString());
        }
        return builder.signWith(secretKey).compact();
    }

    private SecretKey buildKey(String secret) {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (RuntimeException ex) {
            // Segredo em texto puro (não Base64) — comum em testes e .env de desenvolvimento.
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(padIfNecessary(keyBytes));
    }

    private byte[] padIfNecessary(byte[] keyBytes) {
        if (keyBytes.length >= 32) {
            return keyBytes;
        }
        byte[] padded = new byte[32];
        System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
        return padded;
    }
}
