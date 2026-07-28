package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.dto.EffectivePermissionDtos.EffectivePermissionItem;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.EffectivePermissionsResponse;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.GrantedByGroup;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.PermissionExplainResponse;
import br.com.systemcommerce.access.dto.EffectivePermissionDtos.ScopeItem;
import br.com.systemcommerce.access.entity.GroupPermissionAssignment;
import br.com.systemcommerce.access.entity.UserGroupAssignment;
import br.com.systemcommerce.access.repository.GroupPermissionAssignmentRepository;
import br.com.systemcommerce.access.repository.GroupStoreAssignmentRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.access.scope.PermissionScopeType;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.exception.AccessDeniedBusinessException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("effectivePermissionService")
@RequiredArgsConstructor
public class EffectivePermissionService {

    private final UserRepository userRepository;
    private final UserGroupAssignmentRepository userGroupAssignmentRepository;
    private final GroupPermissionAssignmentRepository groupPermissionAssignmentRepository;
    private final GroupStoreAssignmentRepository groupStoreAssignmentRepository;
    private final AccessPrivilegeGuard privilegeGuard;

    /** Cache: userId → (accessVersion, response). */
    private final ConcurrentHashMap<UUID, CachedEntry> cache = new ConcurrentHashMap<>();

    private record CachedEntry(long accessVersion, EffectivePermissionsResponse response) {}

    @Transactional(readOnly = true)
    public EffectivePermissionsResponse mine(boolean includeExplanation) {
        return forUser(CurrentUser.requireId(), includeExplanation);
    }

    @Transactional(readOnly = true)
    public EffectivePermissionsResponse forUser(UUID userId, boolean includeOrigins) {
        User user = userRepository
                .findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
        assertCanView(userId);

        long av = user.getAccessVersion() == null ? 0L : user.getAccessVersion();
        CachedEntry cached = cache.get(userId);
        if (cached != null && cached.accessVersion() == av && !includeOrigins) {
            return cached.response();
        }

        EffectivePermissionsResponse computed = compute(user, av, includeOrigins);
        if (!includeOrigins) {
            cache.put(userId, new CachedEntry(av, computed));
        }
        return computed;
    }

    public void invalidateUser(UUID userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    public void invalidateAll() {
        cache.clear();
    }

    @Transactional(readOnly = true)
    public PermissionExplainResponse explain(UUID userId, String permissionCode) {
        assertCanView(userId);
        User actor = privilegeGuard.requireActor();
        boolean canExplain = privilegeGuard.isSuperAdmin(actor)
                || privilegeGuard.resolveHasAuthority(actor, "EFFECTIVE_PERMISSION_READ");
        EffectivePermissionsResponse eff = forUser(userId, true);
        EffectivePermissionItem item = eff.permissions().stream()
                .filter(p -> p.code().equals(permissionCode))
                .findFirst()
                .orElse(null);
        if (item == null) {
            return new PermissionExplainResponse(
                    userId, permissionCode, false, List.of(), List.of(), "Permissão não concedida pelos grupos ativos");
        }
        String explanation = canExplain
                ? buildExplanation(item)
                : "Permissão concedida (detalhes de origem restritos)";
        return new PermissionExplainResponse(
                userId,
                permissionCode,
                true,
                canExplain ? item.scopes() : List.of(),
                canExplain ? item.grantedByGroups() : List.of(),
                explanation);
    }

    private void assertCanView(UUID targetUserId) {
        UUID actorId = CurrentUser.requireId();
        if (actorId.equals(targetUserId)) {
            return;
        }
        User actor = privilegeGuard.requireActor();
        if (privilegeGuard.isSuperAdmin(actor)) {
            return;
        }
        if (!privilegeGuard.resolveHasAuthority(actor, "EFFECTIVE_PERMISSION_READ")
                && !privilegeGuard.resolveHasAuthority(actor, "ACCESS_GROUP_MEMBER_MANAGE")
                && !privilegeGuard.resolveHasAuthority(actor, "USER_READ")) {
            throw new AccessDeniedBusinessException("Sem permissão para consultar permissões efetivas de outro usuário");
        }
    }

    private EffectivePermissionsResponse compute(User user, long accessVersion, boolean includeOrigins) {
        Instant now = Instant.now();
        List<UserGroupAssignment> assignments =
                userGroupAssignmentRepository.findEffectiveAssignments(user.getId(), now);
        Map<String, MutablePerm> byCode = new LinkedHashMap<>();

        if (assignments.isEmpty() && user.getRoles() != null) {
            // legacy fallback without rich scopes
            for (var role : user.getRoles()) {
                if (role == null || !Boolean.TRUE.equals(role.getActive()) || role.getPermissions() == null) {
                    continue;
                }
                for (var perm : role.getPermissions()) {
                    if (perm == null || !Boolean.TRUE.equals(perm.getActive())) {
                        continue;
                    }
                    MutablePerm mp = byCode.computeIfAbsent(perm.getCode(), MutablePerm::new);
                    mp.addScope(PermissionScopeType.ORGANIZATION, null, null);
                    if (includeOrigins) {
                        mp.addGroup(role.getId(), role.getName(), role.getCode());
                    }
                }
            }
        } else {
            List<UUID> groupIds = assignments.stream().map(a -> a.getGroup().getId()).distinct().toList();
            List<GroupPermissionAssignment> grants =
                    groupPermissionAssignmentRepository.findEffectiveAssignments(groupIds, now);
            List<UUID> storeGroupStores = groupStoreAssignmentRepository.findEffectiveStoreIds(groupIds, now);

            for (GroupPermissionAssignment gpa : grants) {
                String code = gpa.getPermission().getCode();
                MutablePerm mp = byCode.computeIfAbsent(code, MutablePerm::new);
                PermissionScopeType scope = mapScope(gpa.getScope());
                UUID storeId = gpa.getStore() != null ? gpa.getStore().getId() : null;
                UUID orgId = gpa.getOrganization() != null
                        ? gpa.getOrganization().getId()
                        : OrganizationService.DEFAULT_ID;
                if (scope == PermissionScopeType.STORE_GROUP) {
                    for (UUID sid : storeGroupStores) {
                        mp.addScope(PermissionScopeType.STORE, sid, orgId);
                    }
                    mp.addScope(PermissionScopeType.STORE_GROUP, null, orgId);
                } else {
                    mp.addScope(scope, storeId, orgId);
                }
                if (includeOrigins) {
                    mp.addGroup(gpa.getGroup().getId(), gpa.getGroup().getName(), gpa.getGroup().getCode());
                }
            }
        }

        List<EffectivePermissionItem> items = byCode.values().stream()
                .map(MutablePerm::toItem)
                .sorted(Comparator.comparing(EffectivePermissionItem::code))
                .toList();

        return new EffectivePermissionsResponse(user.getId(), OrganizationService.DEFAULT_ID, accessVersion, items);
    }

    private static PermissionScopeType mapScope(GroupPermissionAssignment.Scope scope) {
        if (scope == null) {
            return PermissionScopeType.ORGANIZATION;
        }
        return PermissionScopeType.valueOf(scope.name());
    }

    private static String buildExplanation(EffectivePermissionItem item) {
        String groups = item.grantedByGroups().stream()
                .map(GrantedByGroup::groupName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("—");
        String scopes = item.scopes().stream()
                .map(s -> s.type().name() + (s.storeId() != null ? ":" + s.storeId() : ""))
                .reduce((a, b) -> a + "; " + b)
                .orElse("—");
        return "Concedida por: " + groups + ". Escopos: " + scopes;
    }

    private static final class MutablePerm {
        private final String code;
        private final Map<String, ScopeItem> scopes = new LinkedHashMap<>();
        private final Map<UUID, GrantedByGroup> groups = new LinkedHashMap<>();

        MutablePerm(String code) {
            this.code = code;
        }

        void addScope(PermissionScopeType type, UUID storeId, UUID organizationId) {
            String key = type.name() + "|" + Objects.toString(storeId, "") + "|" + Objects.toString(organizationId, "");
            scopes.putIfAbsent(key, new ScopeItem(type, storeId, organizationId));
            consolidate();
        }

        void addGroup(UUID id, String name, String groupCode) {
            groups.putIfAbsent(id, new GrantedByGroup(id, name, groupCode));
        }

        /** Escopo mais amplo absorve o mais restrito quando aplicável. */
        private void consolidate() {
            List<ScopeItem> list = new ArrayList<>(scopes.values());
            boolean hasGlobal = list.stream().anyMatch(s -> s.type() == PermissionScopeType.GLOBAL_SYSTEM);
            boolean hasOrg = list.stream().anyMatch(s -> s.type() == PermissionScopeType.ORGANIZATION);
            if (hasGlobal || hasOrg) {
                scopes.entrySet().removeIf(e -> {
                    PermissionScopeType t = e.getValue().type();
                    return t == PermissionScopeType.STORE
                            || t == PermissionScopeType.STORE_GROUP
                            || (hasGlobal && t == PermissionScopeType.ORGANIZATION);
                });
            }
        }

        EffectivePermissionItem toItem() {
            List<ScopeItem> scopeList = scopes.values().stream()
                    .sorted(Comparator.comparingInt((ScopeItem s) -> -s.type().breadthRank()))
                    .toList();
            return new EffectivePermissionItem(code, scopeList, List.copyOf(groups.values()));
        }
    }
}
