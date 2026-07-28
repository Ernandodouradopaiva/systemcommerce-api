package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.dto.GroupPermissionDtos.BatchPermissionsRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.CopyPermissionsRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.EffectivePermissionItem;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.GrantPermissionRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.GroupCompareResponse;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.GroupPermissionResponse;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.ReplacePermissionsRequest;
import br.com.systemcommerce.access.dto.GroupPermissionDtos.ReplacePermissionsResult;
import br.com.systemcommerce.access.entity.GroupPermissionAssignment;
import br.com.systemcommerce.access.entity.SystemModule;
import br.com.systemcommerce.access.entity.SystemResource;
import br.com.systemcommerce.access.repository.GroupPermissionAssignmentRepository;
import br.com.systemcommerce.access.repository.SystemModuleRepository;
import br.com.systemcommerce.access.repository.SystemResourceRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.user.entity.Permission;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.PermissionRepository;
import br.com.systemcommerce.user.repository.RoleRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupPermissionService {

    private final AccessGroupService accessGroupService;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final GroupPermissionAssignmentRepository assignmentRepository;
    private final UserGroupAssignmentRepository userGroupAssignmentRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final SystemModuleRepository moduleRepository;
    private final SystemResourceRepository resourceRepository;
    private final AccessAuditService accessAuditService;
    private final AccessPrivilegeGuard privilegeGuard;
    private final PrivilegedAccessService privilegedAccessService;

    @Transactional(readOnly = true)
    public List<GroupPermissionResponse> listDirect(UUID groupId) {
        accessGroupService.requireGroup(groupId);
        return assignmentRepository.findByGroupIdAndActiveTrue(groupId).stream()
                .filter(a -> a.getStatus() == GroupPermissionAssignment.Status.ACTIVE)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GroupPermissionResponse add(UUID groupId, GrantPermissionRequest request) {
        Role group = accessGroupService.requireGroup(groupId);
        assertGroupActive(group);
        User actor = privilegeGuard.requireActor();
        Permission permission = requireActivePermission(request.permissionId());
        privilegeGuard.assertCanGrantPermissions(actor, List.of(permission.getCode()));

        if (privilegedAccessService.requiresApproval(permission)) {
            if (request.reason() == null || request.reason().isBlank()) {
                throw new br.com.systemcommerce.shared.exception.BusinessRuleException(
                        "Permissão sensível exige justificativa e aprovação administrativa");
            }
            privilegedAccessService.requestGrant(groupId, permission.getId(), request.reason(), request.validTo());
            throw new br.com.systemcommerce.shared.exception.BusinessRuleException(
                    "Solicitação de acesso privilegiado criada — aguardando aprovação");
        }

        if (assignmentRepository.findByGroupIdAndPermissionId(groupId, permission.getId()).isPresent()) {
            GroupPermissionAssignment existing =
                    assignmentRepository.findByGroupIdAndPermissionId(groupId, permission.getId()).orElseThrow();
            if (Boolean.TRUE.equals(existing.getActive())
                    && existing.getStatus() == GroupPermissionAssignment.Status.ACTIVE) {
                throw new ConflictException("Permissão já atribuída a este grupo");
            }
            existing.setActive(true);
            existing.setStatus(GroupPermissionAssignment.Status.ACTIVE);
            existing.setGrantType(GroupPermissionAssignment.GrantType.ALLOW);
            existing.setScope(request.scope() != null ? request.scope() : GroupPermissionAssignment.Scope.ORGANIZATION);
            existing.setValidFrom(request.validFrom() != null ? request.validFrom() : Instant.now());
            existing.setValidTo(request.validTo());
            existing.setGrantedBy(actor.getId());
            existing.setReason(request.reason());
            GroupPermissionAssignment saved = assignmentRepository.save(existing);
            syncLegacyRolePermission(group, permission, true);
            accessAuditService.record(
                    orgId(group),
                    actor.getId(),
                    null,
                    groupId,
                    permission.getId(),
                    "GROUP_PERMISSION_ADD",
                    permission.getCode());
            bumpMembers(groupId);
            return toResponse(saved);
        }

        GroupPermissionAssignment gpa = new GroupPermissionAssignment();
        gpa.setGroup(group);
        gpa.setPermission(permission);
        gpa.setGrantType(GroupPermissionAssignment.GrantType.ALLOW);
        gpa.setScope(request.scope() != null ? request.scope() : GroupPermissionAssignment.Scope.ORGANIZATION);
        if (request.organizationId() != null) {
            gpa.setOrganization(organizationRepository
                    .findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organização", request.organizationId())));
        }
        if (request.storeId() != null) {
            gpa.setStore(storeRepository
                    .findById(request.storeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loja", request.storeId())));
        }
        gpa.setValidFrom(request.validFrom() != null ? request.validFrom() : Instant.now());
        gpa.setValidTo(request.validTo());
        gpa.setGrantedBy(actor.getId());
        gpa.setReason(request.reason());
        gpa.setStatus(GroupPermissionAssignment.Status.ACTIVE);
        gpa.setActive(true);

        GroupPermissionAssignment saved = assignmentRepository.save(gpa);
        syncLegacyRolePermission(group, permission, true);
        accessAuditService.record(
                orgId(group),
                actor.getId(),
                null,
                groupId,
                permission.getId(),
                "GROUP_PERMISSION_ADD",
                permission.getCode());
        bumpMembers(groupId);
        return toResponse(saved);
    }

    @Transactional
    public void remove(UUID groupId, UUID permissionId, String reason) {
        Role group = accessGroupService.requireGroup(groupId);
        User actor = privilegeGuard.requireActor();
        GroupPermissionAssignment gpa = assignmentRepository
                .findByGroupIdAndPermissionId(groupId, permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Atribuição de permissão", permissionId));
        gpa.setStatus(GroupPermissionAssignment.Status.REVOKED);
        gpa.setActive(false);
        gpa.setReason(reason);
        assignmentRepository.save(gpa);
        Permission permission = gpa.getPermission();
        syncLegacyRolePermission(group, permission, false);
        accessAuditService.record(
                orgId(group),
                actor.getId(),
                null,
                groupId,
                permissionId,
                "GROUP_PERMISSION_REMOVE",
                reason != null ? reason : permission.getCode());
        bumpMembers(groupId);
    }

    @Transactional
    public ReplacePermissionsResult replace(UUID groupId, ReplacePermissionsRequest request) {
        Role group = accessGroupService.requireGroup(groupId);
        assertGroupActive(group);
        assertVersion(group, request.expectedVersion());
        User actor = privilegeGuard.requireActor();

        List<Permission> desired = resolveDesiredPermissions(request);
        privilegeGuard.assertCanGrantPermissions(
                actor, desired.stream().map(Permission::getCode).toList());
        assertCriticalJustification(desired, request.reason());

        GroupPermissionAssignment.Scope scope =
                request.scope() != null ? request.scope() : GroupPermissionAssignment.Scope.ORGANIZATION;
        Instant now = Instant.now();

        Set<String> beforeCodes = codesOf(groupId);
        Set<UUID> desiredIds = desired.stream().map(Permission::getId).collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, Permission> desiredById =
                desired.stream().collect(Collectors.toMap(Permission::getId, p -> p, (a, b) -> a, HashMap::new));

        List<GroupPermissionAssignment> allExisting = assignmentRepository.findAllByGroupId(groupId);
        Set<UUID> handled = new HashSet<>();

        for (GroupPermissionAssignment gpa : allExisting) {
            UUID permissionId = gpa.getPermission().getId();
            if (desiredIds.contains(permissionId)) {
                gpa.setActive(true);
                gpa.setStatus(GroupPermissionAssignment.Status.ACTIVE);
                gpa.setGrantType(GroupPermissionAssignment.GrantType.ALLOW);
                gpa.setScope(scope);
                gpa.setValidFrom(gpa.getValidFrom() != null ? gpa.getValidFrom() : now);
                gpa.setValidTo(null);
                gpa.setGrantedBy(actor.getId());
                gpa.setReason(request.reason());
                assignmentRepository.save(gpa);
                handled.add(permissionId);
            } else if (Boolean.TRUE.equals(gpa.getActive())
                    && gpa.getStatus() == GroupPermissionAssignment.Status.ACTIVE) {
                gpa.setStatus(GroupPermissionAssignment.Status.REVOKED);
                gpa.setActive(false);
                gpa.setReason(request.reason() != null ? request.reason() : "Removido na substituição");
                assignmentRepository.save(gpa);
            }
        }

        for (UUID permissionId : desiredIds) {
            if (handled.contains(permissionId)) {
                continue;
            }
            Permission permission = desiredById.get(permissionId);
            GroupPermissionAssignment gpa = new GroupPermissionAssignment();
            gpa.setGroup(group);
            gpa.setPermission(permission);
            gpa.setGrantType(GroupPermissionAssignment.GrantType.ALLOW);
            gpa.setScope(scope);
            gpa.setValidFrom(now);
            gpa.setGrantedBy(actor.getId());
            gpa.setReason(request.reason());
            gpa.setStatus(GroupPermissionAssignment.Status.ACTIVE);
            gpa.setActive(true);
            assignmentRepository.save(gpa);
        }

        group.getPermissions().clear();
        group.getPermissions().addAll(desired);
        Role saved = roleRepository.save(group);

        Set<String> afterCodes =
                desired.stream().map(Permission::getCode).collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> added = afterCodes.stream().filter(c -> !beforeCodes.contains(c)).sorted().toList();
        List<String> removed = beforeCodes.stream().filter(c -> !afterCodes.contains(c)).sorted().toList();

        accessAuditService.record(
                orgId(group),
                actor.getId(),
                null,
                groupId,
                null,
                "GROUP_PERMISSION_REPLACE",
                "version="
                        + request.expectedVersion()
                        + "->"
                        + saved.getVersion()
                        + "; added="
                        + String.join(",", added)
                        + "; removed="
                        + String.join(",", removed)
                        + "; reason="
                        + (request.reason() != null ? request.reason() : ""));
        bumpMembers(groupId);

        List<EffectivePermissionItem> effective = toEffectiveItems(desired, scope.name());
        return new ReplacePermissionsResult(
                saved.getId(),
                saved.getName(),
                saved.getVersion(),
                effective.size(),
                added,
                removed,
                effective);
    }

    @Transactional
    public List<GroupPermissionResponse> applyBatch(UUID groupId, BatchPermissionsRequest request) {
        Role group = accessGroupService.requireGroup(groupId);
        assertGroupActive(group);
        assertVersion(group, request.expectedVersion());
        User actor = privilegeGuard.requireActor();
        List<Permission> permissions = loadActivePermissions(request.permissionIds());
        privilegeGuard.assertCanGrantPermissions(
                actor, permissions.stream().map(Permission::getCode).toList());

        List<GroupPermissionResponse> added = new ArrayList<>();
        for (Permission permission : permissions) {
            var existing = assignmentRepository.findByGroupIdAndPermissionId(groupId, permission.getId());
            if (existing.isPresent() && Boolean.TRUE.equals(existing.get().getActive())
                    && existing.get().getStatus() == GroupPermissionAssignment.Status.ACTIVE) {
                continue;
            }
            if (existing.isPresent()) {
                GroupPermissionAssignment gpa = existing.get();
                gpa.setActive(true);
                gpa.setStatus(GroupPermissionAssignment.Status.ACTIVE);
                gpa.setGrantedBy(actor.getId());
                gpa.setReason(request.reason());
                gpa.setValidFrom(Instant.now());
                added.add(toResponse(assignmentRepository.save(gpa)));
            } else {
                GrantPermissionRequest grant = new GrantPermissionRequest(
                        permission.getId(), GroupPermissionAssignment.Scope.ORGANIZATION, null, null, null, null, request.reason());
                added.add(add(groupId, grant));
            }
            syncLegacyRolePermission(group, permission, true);
        }
        accessAuditService.record(
                orgId(group), actor.getId(), null, groupId, null, "GROUP_PERMISSION_BATCH", "added=" + added.size());
        bumpMembers(groupId);
        return added;
    }

    @Transactional
    public List<GroupPermissionResponse> copyFrom(UUID targetGroupId, CopyPermissionsRequest request) {
        Role target = accessGroupService.requireGroup(targetGroupId);
        assertGroupActive(target);
        assertVersion(target, request.expectedVersion());
        List<UUID> permissionIds = assignmentRepository.findByGroupIdAndActiveTrue(request.sourceGroupId()).stream()
                .filter(a -> a.getStatus() == GroupPermissionAssignment.Status.ACTIVE)
                .map(a -> a.getPermission().getId())
                .toList();
        replace(
                targetGroupId,
                new ReplacePermissionsRequest(
                        permissionIds,
                        null,
                        GroupPermissionAssignment.Scope.ORGANIZATION,
                        request.reason() != null ? request.reason() : "Cópia de permissões",
                        request.expectedVersion()));
        return listDirect(targetGroupId);
    }

    @Transactional(readOnly = true)
    public GroupCompareResponse compare(UUID groupAId, UUID groupBId) {
        accessGroupService.requireGroup(groupAId);
        accessGroupService.requireGroup(groupBId);
        Set<String> a = codesOf(groupAId);
        Set<String> b = codesOf(groupBId);
        Set<String> shared = new LinkedHashSet<>(a);
        shared.retainAll(b);
        Set<String> onlyA = new LinkedHashSet<>(a);
        onlyA.removeAll(b);
        Set<String> onlyB = new LinkedHashSet<>(b);
        onlyB.removeAll(a);
        return new GroupCompareResponse(groupAId, groupBId, List.copyOf(onlyA), List.copyOf(onlyB), List.copyOf(shared));
    }

    private Set<String> codesOf(UUID groupId) {
        return assignmentRepository.findByGroupIdAndActiveTrue(groupId).stream()
                .filter(a -> a.getStatus() == GroupPermissionAssignment.Status.ACTIVE)
                .map(a -> a.getPermission().getCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private List<Permission> resolveDesiredPermissions(ReplacePermissionsRequest request) {
        LinkedHashSet<UUID> ids = new LinkedHashSet<>();
        if (request.permissionIds() != null) {
            ids.addAll(request.permissionIds());
        }
        if (request.permissionCodes() != null) {
            for (String code : request.permissionCodes()) {
                if (code == null || code.isBlank()) {
                    continue;
                }
                Permission permission = permissionRepository
                        .findByCode(code.trim().toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Permissão código " + code));
                ids.add(permission.getId());
            }
        }
        if (ids.isEmpty()) {
            return List.of();
        }
        return loadActivePermissions(List.copyOf(ids));
    }

    private void assertCriticalJustification(List<Permission> permissions, String reason) {
        boolean needsReason = permissions.stream()
                .anyMatch(p -> "CRITICAL".equalsIgnoreCase(p.getRiskLevel())
                        || Boolean.TRUE.equals(p.getRequiresJustification())
                        || Boolean.TRUE.equals(p.getSensitive()));
        if (needsReason && (reason == null || reason.isBlank())) {
            throw new BusinessRuleException(
                    "Justificativa obrigatória ao conceder permissões críticas ou sensíveis");
        }
    }

    private List<EffectivePermissionItem> toEffectiveItems(List<Permission> permissions, String scope) {
        Map<UUID, String> moduleNames = moduleRepository.findAll().stream()
                .collect(Collectors.toMap(SystemModule::getId, SystemModule::getName, (a, b) -> a));
        Map<UUID, String> resourceNames = resourceRepository.findAll().stream()
                .collect(Collectors.toMap(SystemResource::getId, SystemResource::getName, (a, b) -> a));
        List<EffectivePermissionItem> items = new ArrayList<>();
        for (Permission p : permissions) {
            items.add(new EffectivePermissionItem(
                    p.getId(),
                    p.getCode(),
                    p.getName(),
                    p.getModuleId() != null ? moduleNames.getOrDefault(p.getModuleId(), p.getModule()) : p.getModule(),
                    p.getResourceId() != null ? resourceNames.getOrDefault(p.getResourceId(), "") : "",
                    scope,
                    p.getRiskLevel() != null ? p.getRiskLevel() : "MEDIUM"));
        }
        return items;
    }

    private List<Permission> loadActivePermissions(List<UUID> ids) {
        List<Permission> permissions = permissionRepository.findAllById(ids);
        if (permissions.size() != new HashSet<>(ids).size()) {
            throw new ResourceNotFoundException("Uma ou mais permissões não foram encontradas");
        }
        for (Permission p : permissions) {
            if (!Boolean.TRUE.equals(p.getActive())) {
                throw new BusinessRuleException("Permissão inativa não pode ser concedida: " + p.getCode());
            }
        }
        return permissions;
    }

    private Permission requireActivePermission(UUID id) {
        Permission permission = permissionRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão", id));
        if (!Boolean.TRUE.equals(permission.getActive())) {
            throw new BusinessRuleException("Permissão inativa não pode ser concedida");
        }
        return permission;
    }

    private void assertGroupActive(Role group) {
        if (!Boolean.TRUE.equals(group.getActive())) {
            throw new BusinessRuleException("Grupo inativo não pode receber novas permissões");
        }
    }

    private void assertVersion(Role group, Long expected) {
        if (expected == null || !expected.equals(group.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Role.class, group.getId());
        }
    }

    private void syncLegacyRolePermission(Role group, Permission permission, boolean add) {
        Role managed = roleRepository
                .findWithPermissionsByCode(group.getCode())
                .orElse(group);
        if (add) {
            managed.getPermissions().add(permission);
        } else {
            managed.getPermissions().removeIf(p -> p.getId().equals(permission.getId()));
        }
        roleRepository.save(managed);
    }

    private void bumpMembers(UUID groupId) {
        List<UUID> userIds = userGroupAssignmentRepository.findByGroupIdAndActiveTrue(groupId).stream()
                .map(a -> a.getUser().getId())
                .distinct()
                .toList();
        accessAuditService.bumpAccessVersion(userIds);
    }

    private UUID orgId(Role group) {
        return group.getOrganization() != null ? group.getOrganization().getId() : null;
    }

    private GroupPermissionResponse toResponse(GroupPermissionAssignment gpa) {
        Permission p = gpa.getPermission();
        return new GroupPermissionResponse(
                gpa.getId(),
                p.getId(),
                p.getCode(),
                p.getName(),
                gpa.getGrantType(),
                gpa.getScope(),
                gpa.getOrganization() != null ? gpa.getOrganization().getId() : null,
                gpa.getStore() != null ? gpa.getStore().getId() : null,
                gpa.getValidFrom(),
                gpa.getValidTo(),
                gpa.getStatus(),
                gpa.getReason(),
                gpa.getVersion());
    }
}
