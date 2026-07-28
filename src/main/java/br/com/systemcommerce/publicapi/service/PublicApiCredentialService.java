package br.com.systemcommerce.publicapi.service;

import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.publicapi.dto.PublicApiCredentialCreateRequest;
import br.com.systemcommerce.publicapi.dto.PublicApiCredentialCreatedResponse;
import br.com.systemcommerce.publicapi.dto.PublicApiCredentialResponse;
import br.com.systemcommerce.publicapi.dto.PublicApiTokenRequest;
import br.com.systemcommerce.publicapi.dto.PublicApiTokenResponse;
import br.com.systemcommerce.publicapi.entity.PublicApiAccessLog;
import br.com.systemcommerce.publicapi.entity.PublicApiCredential;
import br.com.systemcommerce.publicapi.repository.PublicApiAccessLogRepository;
import br.com.systemcommerce.publicapi.repository.PublicApiCredentialRepository;
import br.com.systemcommerce.security.JwtService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PublicApiCredentialService {

    private static final long TOKEN_TTL_MS = 3_600_000L;

    private final PublicApiCredentialRepository credentialRepository;
    private final PublicApiAccessLogRepository accessLogRepository;
    private final OrganizationService organizationService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final DomainAuditService domainAuditService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentHashMap<String, WindowCounter> rateWindows = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Page<PublicApiCredentialResponse> list(UUID organizationId, Pageable pageable) {
        return credentialRepository
                .findAll(
                        (root, q, cb) ->
                                organizationId == null
                                        ? cb.conjunction()
                                        : cb.equal(root.get("organization").get("id"), organizationId),
                        pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PublicApiCredentialCreatedResponse create(PublicApiCredentialCreateRequest request) {
        var org = organizationService.resolveForStoreCreate(request.organizationId());
        String clientId = "sc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        byte[] secretBytes = new byte[32];
        secureRandom.nextBytes(secretBytes);
        String clientSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(secretBytes);

        PublicApiCredential cred = new PublicApiCredential();
        cred.setOrganization(org);
        cred.setClientId(clientId);
        cred.setClientSecretHash(passwordEncoder.encode(clientSecret));
        cred.setName(request.name().trim());
        cred.setScopes(normalizeScopes(request.scopes()));
        cred.setRateLimitPerMinute(
                request.rateLimitPerMinute() != null && request.rateLimitPerMinute() > 0
                        ? request.rateLimitPerMinute()
                        : 60);
        PublicApiCredential saved = credentialRepository.save(cred);
        domainAuditService.record(
                "PublicApiCredential",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                Map.of("clientId", clientId),
                "Credencial de API pública criada");
        return new PublicApiCredentialCreatedResponse(
                saved.getId(),
                org.getId(),
                clientId,
                clientSecret,
                saved.getName(),
                saved.getScopes(),
                saved.getRateLimitPerMinute());
    }

    @Transactional
    public PublicApiCredentialResponse revoke(UUID id) {
        PublicApiCredential cred = credentialRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Credencial não encontrada"));
        cred.setRevokedAt(Instant.now());
        cred.setActive(Boolean.FALSE);
        return toResponse(credentialRepository.save(cred));
    }

    @Transactional
    public PublicApiTokenResponse issueToken(PublicApiTokenRequest request) {
        String grant = StringUtils.hasText(request.grantType()) ? request.grantType() : "client_credentials";
        if (!"client_credentials".equalsIgnoreCase(grant)) {
            throw new BusinessRuleException("grant_type suportado: client_credentials");
        }
        PublicApiCredential cred = credentialRepository
                .findByClientIdAndActiveTrue(request.clientId().trim())
                .orElseThrow(() -> new BusinessRuleException("Credenciais inválidas"));
        if (cred.getRevokedAt() != null) {
            throw new BusinessRuleException("Credencial revogada");
        }
        if (!passwordEncoder.matches(request.clientSecret(), cred.getClientSecretHash())) {
            throw new BusinessRuleException("Credenciais inválidas");
        }
        assertRateLimit(cred);
        cred.setLastUsedAt(Instant.now());
        credentialRepository.save(cred);
        String token = jwtService.generatePublicAccessToken(
                cred.getClientId(), cred.getOrganization().getId(), cred.getScopes(), TOKEN_TTL_MS);
        return new PublicApiTokenResponse(token, "Bearer", TOKEN_TTL_MS / 1000, cred.getScopes());
    }

    @Transactional
    public void logAccess(
            PublicApiCredential cred,
            String method,
            String path,
            int status,
            String scopesUsed,
            String correlationId,
            String idempotencyKey) {
        PublicApiAccessLog log = new PublicApiAccessLog();
        log.setOrganization(cred.getOrganization());
        log.setCredential(cred);
        log.setClientId(cred.getClientId());
        log.setMethod(method);
        log.setPath(path.length() > 500 ? path.substring(0, 500) : path);
        log.setStatusCode(status);
        log.setScopesUsed(scopesUsed);
        log.setCorrelationId(correlationId);
        log.setIdempotencyKey(idempotencyKey);
        accessLogRepository.save(log);
    }

    public PublicApiCredential requireActive(String clientId) {
        PublicApiCredential cred = credentialRepository
                .findByClientIdAndActiveTrue(clientId)
                .orElseThrow(() -> new BusinessRuleException("Credencial inválida"));
        if (cred.getRevokedAt() != null) {
            throw new BusinessRuleException("Credencial revogada");
        }
        return cred;
    }

    public void assertRateLimit(PublicApiCredential cred) {
        long window = Instant.now().getEpochSecond() / 60;
        String key = cred.getClientId() + ":" + window;
        WindowCounter counter = rateWindows.computeIfAbsent(key, k -> new WindowCounter());
        if (counter.incrementAndGet() > cred.getRateLimitPerMinute()) {
            throw new BusinessRuleException("Rate limit excedido para client_id");
        }
    }

    public boolean hasScope(String scopesCsv, String required) {
        if (!StringUtils.hasText(scopesCsv)) {
            return false;
        }
        for (String s : scopesCsv.split("[,\\s]+")) {
            if (required.equalsIgnoreCase(s.trim()) || "*".equals(s.trim())) {
                return true;
            }
        }
        return false;
    }

    private String normalizeScopes(String scopes) {
        return scopes.trim().replaceAll("\\s+", " ");
    }

    private PublicApiCredentialResponse toResponse(PublicApiCredential c) {
        return new PublicApiCredentialResponse(
                c.getId(),
                c.getOrganization().getId(),
                c.getClientId(),
                c.getName(),
                c.getScopes(),
                c.getRateLimitPerMinute(),
                c.getRevokedAt(),
                c.getLastUsedAt(),
                c.getActive());
    }

    private static final class WindowCounter {
        private final AtomicInteger count = new AtomicInteger();

        int incrementAndGet() {
            return count.incrementAndGet();
        }
    }
}
