package br.com.systemcommerce.auth.service;

import br.com.systemcommerce.access.service.UserSessionService;
import br.com.systemcommerce.auth.dto.AuthTokenResponse;
import br.com.systemcommerce.auth.dto.ChangePasswordRequest;
import br.com.systemcommerce.auth.dto.ForgotPasswordResponse;
import br.com.systemcommerce.auth.dto.LoginRequest;
import br.com.systemcommerce.auth.dto.MessageResponse;
import br.com.systemcommerce.auth.dto.ResetPasswordRequest;
import br.com.systemcommerce.auth.entity.PasswordResetToken;
import br.com.systemcommerce.auth.entity.RefreshToken;
import br.com.systemcommerce.auth.repository.PasswordResetTokenRepository;
import br.com.systemcommerce.auth.repository.RefreshTokenRepository;
import br.com.systemcommerce.security.AuthProperties;
import br.com.systemcommerce.security.JwtService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.AuditRequestContext;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.InvalidTokenException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.exception.TokenExpiredException;
import br.com.systemcommerce.shared.exception.UnauthorizedException;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import br.com.systemcommerce.user.service.PermissionResolver;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /**
     * Hash BCrypt válido usado apenas para equalizar custo computacional quando o usuário não existe
     * ou está bloqueado (mitigação de timing/enumeração). Não corresponde a nenhuma senha de produção.
     */
    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenHasher tokenHasher;
    private final PermissionResolver permissionResolver;
    private final AuthProperties authProperties;
    private final Environment environment;
    private final DomainAuditService domainAuditService;
    private final UserSessionService userSessionService;

    @Transactional
    public AuthTokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        if (ipAddress != null && !ipAddress.isBlank()) {
            AuditRequestContext.setIpAddress(ipAddress);
        }

        String username = request.username().trim();
        User user = userRepository.findByEmailOrLoginWithRoles(username).orElse(null);
        if (user == null) {
            // Equaliza tempo de resposta (mitiga enumeração por timing) e não revela existência.
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            auditLoginFailure(null, username, "Usuário não encontrado");
            throw new UnauthorizedException("Credenciais inválidas");
        }

        if (user.getStatus() == User.UserStatus.BLOCKED
                || user.getStatus() == User.UserStatus.INACTIVE
                || !Boolean.TRUE.equals(user.getActive())) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            auditLoginFailure(user, username, "Usuário bloqueado ou inativo");
            throw new UnauthorizedException("Credenciais inválidas");
        }

        if (user.isTemporarilyLocked(now)) {
            passwordEncoder.matches(request.password(), DUMMY_PASSWORD_HASH);
            auditLoginFailure(user, username, "Usuário temporariamente bloqueado");
            throw new UnauthorizedException("Credenciais inválidas");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user, now);
            auditLoginFailure(user, username, "Senha inválida");
            throw new UnauthorizedException("Credenciais inválidas");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(now);
        userRepository.save(user);

        AuthTokenResponse tokens = issueTokens(user, ipAddress, userAgent);
        domainAuditService.recordAuth(
                "Auth",
                user.getId(),
                AuditLog.AuditAction.LOGIN,
                null,
                Map.of("login", user.getLogin(), "email", user.getEmail()),
                "Login bem-sucedido",
                user);
        return tokens;
    }

    @Transactional
    public AuthTokenResponse refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        Instant now = Instant.now();
        Claims claims = parseRefreshClaims(rawRefreshToken);
        String hash = tokenHasher.hash(rawRefreshToken);

        RefreshToken stored = refreshTokenRepository
                .findByTokenHashWithUser(hash)
                .orElseThrow(() -> new InvalidTokenException("Refresh token inválido"));

        if (!stored.isActive(now)) {
            throw new InvalidTokenException("Refresh token revogado ou expirado");
        }

        UUID subject = UUID.fromString(claims.getSubject());
        if (!subject.equals(stored.getUser().getId())) {
            throw new InvalidTokenException("Refresh token inválido");
        }

        User user = userRepository
                .findWithRolesById(stored.getUser().getId())
                .orElseThrow(() -> new InvalidTokenException("Refresh token inválido"));

        if (!user.canAuthenticate(now)) {
            stored.setRevokedAt(now);
            refreshTokenRepository.save(stored);
            throw new UnauthorizedException("Usuário bloqueado ou inativo");
        }

        List<String> permissions = permissionResolver.resolvePermissionCodes(user);
        List<String> roles = permissionResolver.resolveRoleCodes(user);
        long av = user.getAccessVersion() != null ? user.getAccessVersion() : 0L;
        String rawRefresh = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        RefreshToken replacement = new RefreshToken();
        replacement.setUser(user);
        replacement.setTokenHash(tokenHasher.hash(rawRefresh));
        replacement.setExpiresAt(now.plusMillis(jwtService.getRefreshExpirationMs()));
        replacement.setIpAddress(truncate(ipAddress, 45));
        replacement.setUserAgent(truncate(userAgent, 500));
        refreshTokenRepository.save(replacement);

        stored.setRevokedAt(now);
        stored.setReplacedBy(replacement);
        refreshTokenRepository.save(stored);

        var session = userSessionService.openSession(user, null, ipAddress, userAgent, replacement.getId());
        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                permissions,
                av,
                session.getId(),
                br.com.systemcommerce.organization.service.OrganizationService.DEFAULT_ID,
                null);

        return new AuthTokenResponse(
                accessToken,
                rawRefresh,
                "Bearer",
                jwtService.getAccessExpirationMs() / 1000,
                jwtService.getRefreshExpirationMs() / 1000,
                new AuthTokenResponse.AuthenticatedUserResponse(
                        user.getId(), user.getName(), user.getEmail(), user.getLogin(), roles, permissions));
    }

    @Transactional
    public MessageResponse logout(String rawRefreshToken) {
        Instant now = Instant.now();
        String hash = tokenHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHashWithUser(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(now);
                refreshTokenRepository.save(token);
                domainAuditService.recordAuth(
                        "Auth",
                        token.getUser().getId(),
                        AuditLog.AuditAction.LOGOUT,
                        null,
                        Map.of("login", token.getUser().getLogin()),
                        "Logout realizado",
                        token.getUser());
            }
        });
        return new MessageResponse("Logout realizado com sucesso");
    }

    @Transactional
    public MessageResponse logoutAll(UUID userId) {
        userSessionService.revokeAllForUser(userId, "logout_all");
        return new MessageResponse("Sessões encerradas com sucesso");
    }

    @Transactional
    public MessageResponse changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Senha atual inválida");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
        domainAuditService.record(
                "USER",
                "User",
                userId,
                AuditLog.AuditAction.UPDATE,
                null,
                Map.of("passwordChanged", true),
                "Senha alterada");
        return new MessageResponse("Senha alterada com sucesso");
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(String email) {
        String genericMessage =
                "Se o e-mail existir, um token de recuperação será processado";
        Instant now = Instant.now();

        return userRepository
                .findByEmailIgnoreCase(email.trim())
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .map(user -> {
                    String rawToken = tokenHasher.generateRawToken();
                    PasswordResetToken resetToken = new PasswordResetToken();
                    resetToken.setUser(user);
                    resetToken.setTokenHash(tokenHasher.hash(rawToken));
                    resetToken.setExpiresAt(
                            now.plus(authProperties.getPasswordResetExpirationMinutes(), ChronoUnit.MINUTES));
                    passwordResetTokenRepository.save(resetToken);
                    log.info("Password reset gerado para userId={}", user.getId());

                    boolean exposeToken = environment.acceptsProfiles(Profiles.of("test", "dev"));
                    return new ForgotPasswordResponse(genericMessage, exposeToken ? rawToken : null);
                })
                .orElseGet(() -> new ForgotPasswordResponse(genericMessage, null));
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        Instant now = Instant.now();
        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHashWithUser(tokenHasher.hash(request.token()))
                .orElseThrow(() -> new InvalidTokenException("Token de recuperação inválido"));

        if (!token.isUsable(now)) {
            throw new TokenExpiredException("Token de recuperação expirado ou já utilizado");
        }

        User user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        token.setUsedAt(now);
        passwordResetTokenRepository.save(token);
        refreshTokenRepository.revokeAllActiveByUserId(user.getId(), now);
        domainAuditService.record(
                "USER",
                "User",
                user.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                Map.of("passwordReset", true),
                "Senha redefinida",
                user);
        return new MessageResponse("Senha redefinida com sucesso");
    }

    private void auditLoginFailure(User user, String username, String reason) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("reason", reason);
        domainAuditService.recordAuth(
                "Auth",
                user != null ? user.getId() : null,
                AuditLog.AuditAction.LOGIN_FAILURE,
                null,
                payload,
                "Falha de login: " + reason,
                user);
    }

    private void registerFailedAttempt(User user, Instant now) {
        int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
        attempts++;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= authProperties.getMaxFailedAttempts()) {
            user.setLockedUntil(now.plus(authProperties.getLockDurationMinutes(), ChronoUnit.MINUTES));
            user.setFailedLoginAttempts(0);
            log.warn("Usuario {} bloqueado temporariamente apos tentativas invalidas", user.getId());
        }
        userRepository.save(user);
    }

    private AuthTokenResponse issueTokens(User user, String ipAddress, String userAgent) {
        List<String> permissions = permissionResolver.resolvePermissionCodes(user);
        List<String> roles = permissionResolver.resolveRoleCodes(user);

        long av = user.getAccessVersion() != null ? user.getAccessVersion() : 0L;
        String rawRefresh = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(tokenHasher.hash(rawRefresh));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
        refreshToken.setIpAddress(truncate(ipAddress, 45));
        refreshToken.setUserAgent(truncate(userAgent, 500));
        refreshTokenRepository.save(refreshToken);

        var session = userSessionService.openSession(user, null, ipAddress, userAgent, refreshToken.getId());
        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getEmail(),
                permissions,
                av,
                session.getId(),
                br.com.systemcommerce.organization.service.OrganizationService.DEFAULT_ID,
                null);

        return new AuthTokenResponse(
                accessToken,
                rawRefresh,
                "Bearer",
                jwtService.getAccessExpirationMs() / 1000,
                jwtService.getRefreshExpirationMs() / 1000,
                new AuthTokenResponse.AuthenticatedUserResponse(
                        user.getId(), user.getName(), user.getEmail(), user.getLogin(), roles, permissions));
    }

    private Claims parseRefreshClaims(String rawRefreshToken) {
        try {
            Claims claims = jwtService.parseClaims(rawRefreshToken);
            if (!jwtService.isRefreshToken(claims)) {
                throw new InvalidTokenException("Token informado não é um refresh token");
            }
            return claims;
        } catch (ExpiredJwtException ex) {
            throw new TokenExpiredException("Refresh token expirado");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new InvalidTokenException("Refresh token inválido");
        }
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
