package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.entity.SessionRevocation;
import br.com.systemcommerce.access.entity.UserSession;
import br.com.systemcommerce.access.repository.SessionRevocationRepository;
import br.com.systemcommerce.access.repository.UserSessionRepository;
import br.com.systemcommerce.auth.repository.RefreshTokenRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final SessionRevocationRepository sessionRevocationRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public UserSession openSession(User user, UUID storeId, String ip, String userAgent, UUID refreshTokenId) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setOrganizationId(OrganizationService.DEFAULT_ID);
        session.setStoreId(storeId);
        session.setAccessVersion(user.getAccessVersion() != null ? user.getAccessVersion() : 0L);
        session.setRefreshTokenId(refreshTokenId);
        session.setIpAddress(ip);
        session.setUserAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent);
        session.setStatus(UserSession.Status.ACTIVE);
        session.setStartedAt(Instant.now());
        session.setLastSeenAt(Instant.now());
        session.setActive(true);
        return userSessionRepository.save(session);
    }

    @Transactional
    public void logoutSession(UUID sessionId, String reason) {
        UserSession session = userSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Sessão", sessionId));
        session.setStatus(UserSession.Status.LOGGED_OUT);
        session.setEndedAt(Instant.now());
        session.setActive(false);
        userSessionRepository.save(session);
        SessionRevocation rev = new SessionRevocation();
        rev.setSessionId(sessionId);
        rev.setRevokedBy(CurrentUser.id().orElse(null));
        rev.setReason(reason != null ? reason : "logout");
        sessionRevocationRepository.save(rev);
    }

    @Transactional
    public void revokeAllForUser(UUID userId, String reason) {
        userSessionRepository.revokeAllActiveByUserId(userId);
        refreshTokenRepository.revokeAllActiveByUserId(userId, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<UserSession> listByUser(UUID userId) {
        return userSessionRepository.findByUserIdOrderByStartedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public boolean isSessionActive(UUID sessionId) {
        return userSessionRepository
                .findById(sessionId)
                .map(s -> s.getStatus() == UserSession.Status.ACTIVE && Boolean.TRUE.equals(s.getActive()))
                .orElse(false);
    }
}
