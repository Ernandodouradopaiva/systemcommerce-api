package br.com.systemcommerce.user.service;

import br.com.systemcommerce.access.entity.UserGroupAssignment;
import br.com.systemcommerce.access.repository.GroupPermissionAssignmentRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.user.entity.Permission;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Calcula permissões efetivas = união ALLOW dos grupos ativos e vínculos vigentes.
 * Preferência: assignments (V281+); fallback: ManyToMany legado.
 */
@Component
@RequiredArgsConstructor
public class PermissionResolver {

    private final UserGroupAssignmentRepository userGroupAssignmentRepository;
    private final GroupPermissionAssignmentRepository groupPermissionAssignmentRepository;

    public List<String> resolvePermissionCodes(User user) {
        Instant now = Instant.now();
        List<UserGroupAssignment> assignments =
                userGroupAssignmentRepository.findEffectiveAssignments(user.getId(), now);
        if (!assignments.isEmpty()) {
            List<UUID> groupIds = assignments.stream()
                    .map(a -> a.getGroup().getId())
                    .distinct()
                    .toList();
            return groupPermissionAssignmentRepository.findEffectivePermissionCodes(groupIds, now);
        }
        return resolveFromLegacyRoles(user);
    }

    public List<String> resolveRoleCodes(User user) {
        Instant now = Instant.now();
        List<UserGroupAssignment> assignments =
                userGroupAssignmentRepository.findEffectiveAssignments(user.getId(), now);
        if (!assignments.isEmpty()) {
            return assignments.stream()
                    .map(a -> a.getGroup().getCode())
                    .distinct()
                    .toList();
        }
        Set<String> codes = new LinkedHashSet<>();
        if (user.getRoles() != null) {
            for (Role role : user.getRoles()) {
                if (role != null && Boolean.TRUE.equals(role.getActive())) {
                    codes.add(role.getCode());
                }
            }
        }
        return List.copyOf(codes);
    }

    private List<String> resolveFromLegacyRoles(User user) {
        Set<String> codes = new LinkedHashSet<>();
        if (user.getRoles() == null) {
            return List.of();
        }
        for (Role role : user.getRoles()) {
            if (role == null || !Boolean.TRUE.equals(role.getActive()) || role.getPermissions() == null) {
                continue;
            }
            for (Permission permission : role.getPermissions()) {
                if (permission != null && Boolean.TRUE.equals(permission.getActive())) {
                    codes.add(permission.getCode());
                }
            }
        }
        return List.copyOf(codes);
    }
}
