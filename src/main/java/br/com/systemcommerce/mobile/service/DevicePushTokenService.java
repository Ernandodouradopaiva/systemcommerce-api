package br.com.systemcommerce.mobile.service;

import br.com.systemcommerce.mobile.dto.DevicePushTokenRegisterRequest;
import br.com.systemcommerce.mobile.dto.DevicePushTokenResponse;
import br.com.systemcommerce.mobile.entity.DevicePushToken;
import br.com.systemcommerce.mobile.repository.DevicePushTokenRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.repository.UserRepository;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DevicePushTokenService {

    private static final Set<String> PLATFORMS = Set.of("ANDROID", "IOS", "WEB");

    private final DevicePushTokenRepository repository;
    private final OrganizationService organizationService;
    private final UserRepository userRepository;

    @Transactional
    public DevicePushTokenResponse register(DevicePushTokenRegisterRequest request) {
        String platform = request.platform().trim().toUpperCase();
        if (!PLATFORMS.contains(platform)) {
            throw new BusinessRuleException("Plataforma inválida");
        }
        var org = organizationService.resolveForStoreCreate(request.organizationId());
        var user = userRepository
                .findById(CurrentUser.requireId())
                .orElseThrow(() -> new BusinessRuleException("Usuário não encontrado"));

        DevicePushToken token = repository
                .findByToken(request.token().trim())
                .orElseGet(DevicePushToken::new);
        token.setOrganization(org);
        token.setUser(user);
        token.setPlatform(platform);
        token.setToken(request.token().trim());
        token.setDeviceName(request.deviceName());
        token.setAppVersion(request.appVersion());
        token.setLastSeenAt(Instant.now());
        token.setActive(Boolean.TRUE);
        DevicePushToken saved = repository.save(token);
        return new DevicePushTokenResponse(
                saved.getId(),
                user.getId(),
                saved.getPlatform(),
                saved.getDeviceName(),
                saved.getAppVersion(),
                saved.getLastSeenAt());
    }
}
