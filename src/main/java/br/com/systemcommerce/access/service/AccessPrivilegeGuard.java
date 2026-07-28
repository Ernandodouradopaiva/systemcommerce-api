package br.com.systemcommerce.access.service;

import br.com.systemcommerce.shared.exception.AccessDeniedBusinessException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import br.com.systemcommerce.user.service.PermissionResolver;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Impede escalada: administrador só concede permissões que possui,
 * salvo grupo ADMIN (superadministrador).
 */
@Component
@RequiredArgsConstructor
public class AccessPrivilegeGuard {

    public static final String SUPER_ADMIN_GROUP_CODE = "ADMIN";

    private final UserRepository userRepository;
    private final PermissionResolver permissionResolver;

    public User requireActor() {
        UUID id = CurrentUser.requireId();
        return userRepository
                .findWithRolesById(id)
                .orElseThrow(() -> new AccessDeniedBusinessException("Usuário autenticado inválido"));
    }

    public boolean isSuperAdmin(User actor) {
        return permissionResolver.resolveRoleCodes(actor).contains(SUPER_ADMIN_GROUP_CODE);
    }

    public boolean resolveHasAuthority(User actor, String permissionCode) {
        return permissionResolver.resolvePermissionCodes(actor).contains(permissionCode);
    }

    public void assertCanGrantPermissions(User actor, Collection<String> permissionCodes) {
        if (isSuperAdmin(actor)) {
            return;
        }
        Set<String> owned = new HashSet<>(permissionResolver.resolvePermissionCodes(actor));
        for (String code : permissionCodes) {
            if (!owned.contains(code)) {
                throw new AccessDeniedBusinessException(
                        "Não é permitido conceder permissão superior às suas: " + code);
            }
        }
    }

    public void assertNotSelfGroupChange(UUID targetUserId) {
        UUID actorId = CurrentUser.requireId();
        if (actorId.equals(targetUserId)) {
            throw new AccessDeniedBusinessException("Usuário não pode alterar os próprios grupos");
        }
    }
}
