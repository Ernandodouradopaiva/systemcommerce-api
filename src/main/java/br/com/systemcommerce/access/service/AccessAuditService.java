package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.entity.AccessChangeHistory;
import br.com.systemcommerce.access.repository.AccessChangeHistoryRepository;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessAuditService {

    private final AccessChangeHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final EffectivePermissionService effectivePermissionService;

    @Transactional
    public void record(
            UUID organizationId,
            UUID actorUserId,
            UUID targetUserId,
            UUID groupId,
            UUID permissionId,
            String changeType,
            String details) {
        AccessChangeHistory h = new AccessChangeHistory();
        h.setOrganizationId(organizationId);
        h.setActorUserId(actorUserId);
        h.setTargetUserId(targetUserId);
        h.setGroupId(groupId);
        h.setPermissionId(permissionId);
        h.setChangeType(changeType);
        h.setDetails(details);
        historyRepository.save(h);
    }

    /** Incrementa access_version dos usuários afetados (invalida JWT com claim av desatualizado). */
    @Transactional
    public void bumpAccessVersion(Collection<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        Set<UUID> unique = new HashSet<>(userIds);
        List<User> users = userRepository.findAllById(unique);
        for (User user : users) {
            long current = user.getAccessVersion() == null ? 0L : user.getAccessVersion();
            user.setAccessVersion(current + 1);
            effectivePermissionService.invalidateUser(user.getId());
        }
        userRepository.saveAll(users);
    }

    @Transactional
    public void bumpAccessVersionForGroupMembers(UUID groupId, List<UUID> memberUserIds) {
        bumpAccessVersion(memberUserIds);
    }
}
