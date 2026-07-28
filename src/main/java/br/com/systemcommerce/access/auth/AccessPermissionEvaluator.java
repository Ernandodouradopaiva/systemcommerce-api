package br.com.systemcommerce.access.auth;

import java.io.Serializable;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Integração opcional com hasPermission do Spring Security.
 */
@Component
@RequiredArgsConstructor
public class AccessPermissionEvaluator implements PermissionEvaluator {

    private final AuthorizationService authorizationService;

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (permission == null) {
            return false;
        }
        return authorizationService.hasPermission(String.valueOf(permission));
    }

    @Override
    public boolean hasPermission(
            Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (permission == null || targetType == null || targetId == null) {
            return false;
        }
        UUID id = targetId instanceof UUID uuid ? uuid : UUID.fromString(String.valueOf(targetId));
        return authorizationService.canAccessResource(String.valueOf(permission), targetType, id);
    }
}
