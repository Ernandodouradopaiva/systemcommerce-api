package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.dto.AccessGroupDtos.AccessGroupCreateRequest;
import br.com.systemcommerce.access.dto.AccessGroupDtos.AccessGroupDuplicateRequest;
import br.com.systemcommerce.access.dto.AccessGroupDtos.AccessGroupResponse;
import br.com.systemcommerce.access.dto.AccessGroupDtos.AccessGroupUpdateRequest;
import br.com.systemcommerce.access.entity.AccessChangeHistory;
import br.com.systemcommerce.access.entity.GroupPermissionAssignment;
import br.com.systemcommerce.access.entity.UserGroupAssignment;
import br.com.systemcommerce.access.repository.AccessChangeHistoryRepository;
import br.com.systemcommerce.access.repository.GroupPermissionAssignmentRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.RoleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccessGroupService {

    private final RoleRepository roleRepository;
    private final OrganizationRepository organizationRepository;
    private final GroupPermissionAssignmentRepository groupPermissionAssignmentRepository;
    private final UserGroupAssignmentRepository userGroupAssignmentRepository;
    private final AccessChangeHistoryRepository historyRepository;
    private final AccessAuditService accessAuditService;
    private final AccessPrivilegeGuard privilegeGuard;

    @Transactional(readOnly = true)
    public List<AccessGroupResponse> list(String search, boolean activeOnly) {
        return roleRepository.search(search, activeOnly).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AccessGroupResponse getById(UUID id) {
        return toResponse(requireGroup(id));
    }

    @Transactional
    public AccessGroupResponse create(AccessGroupCreateRequest request) {
        privilegeGuard.requireActor();
        String code = request.code().trim().toUpperCase();
        if (roleRepository.existsByCodeIgnoreCase(code)) {
            throw new ConflictException("Código de grupo já está em uso");
        }
        Role group = new Role();
        group.setCode(code);
        group.setName(request.name().trim());
        group.setDescription(request.description());
        group.setGroupType(request.groupType() != null ? request.groupType() : Role.GroupType.CUSTOM);
        group.setDefaultScope(request.defaultScope() != null ? request.defaultScope() : Role.DefaultScope.ORGANIZATION);
        group.setDefaultGroup(Boolean.TRUE.equals(request.defaultGroup()));
        group.setAllowsAdministration(Boolean.TRUE.equals(request.allowsAdministration()));
        group.setVisualPriority(request.visualPriority() != null ? request.visualPriority() : 100);
        group.setSystemGroup(false);
        group.setActive(true);
        if (request.organizationId() != null) {
            Organization org = organizationRepository
                    .findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organização", request.organizationId()));
            group.setOrganization(org);
        }
        Role saved = roleRepository.save(group);
        accessAuditService.record(
                orgId(saved),
                CurrentUser.id().orElse(null),
                null,
                saved.getId(),
                null,
                "GROUP_CREATE",
                "Grupo criado: " + saved.getCode());
        return toResponse(saved);
    }

    @Transactional
    public AccessGroupResponse update(UUID id, AccessGroupUpdateRequest request) {
        privilegeGuard.requireActor();
        Role group = requireGroup(id);
        if (request.version() != null && !request.version().equals(group.getVersion())) {
            throw new ObjectOptimisticLockingFailureException(Role.class, id);
        }
        group.setName(request.name().trim());
        group.setDescription(request.description());
        if (request.groupType() != null && !Boolean.TRUE.equals(group.getSystemGroup())) {
            group.setGroupType(request.groupType());
        }
        if (request.defaultScope() != null) {
            group.setDefaultScope(request.defaultScope());
        }
        if (request.defaultGroup() != null) {
            group.setDefaultGroup(request.defaultGroup());
        }
        if (request.allowsAdministration() != null) {
            group.setAllowsAdministration(request.allowsAdministration());
        }
        if (request.visualPriority() != null) {
            group.setVisualPriority(request.visualPriority());
        }
        Role saved = roleRepository.save(group);
        accessAuditService.record(
                orgId(saved),
                CurrentUser.id().orElse(null),
                null,
                saved.getId(),
                null,
                "GROUP_UPDATE",
                "Grupo atualizado: " + saved.getCode());
        bumpMembers(saved.getId());
        return toResponse(saved);
    }

    @Transactional
    public AccessGroupResponse activate(UUID id) {
        Role group = requireGroup(id);
        group.setActive(true);
        Role saved = roleRepository.save(group);
        accessAuditService.record(
                orgId(saved), CurrentUser.id().orElse(null), null, id, null, "GROUP_ACTIVATE", "Grupo ativado");
        bumpMembers(id);
        return toResponse(saved);
    }

    @Transactional
    public AccessGroupResponse deactivate(UUID id) {
        Role group = requireGroup(id);
        if (Boolean.TRUE.equals(group.getSystemGroup()) && AccessPrivilegeGuard.SUPER_ADMIN_GROUP_CODE.equals(group.getCode())) {
            ensureNotLastSuperAdminGroup();
        }
        group.setActive(false);
        Role saved = roleRepository.save(group);
        accessAuditService.record(
                orgId(saved), CurrentUser.id().orElse(null), null, id, null, "GROUP_DISABLE", "Grupo inativado");
        bumpMembers(id);
        return toResponse(saved);
    }

    @Transactional
    public AccessGroupResponse duplicate(UUID id, AccessGroupDuplicateRequest request) {
        privilegeGuard.requireActor();
        Role source = requireGroup(id);
        String newCode = request.newCode().trim().toUpperCase();
        if (roleRepository.existsByCodeIgnoreCase(newCode)) {
            throw new ConflictException("Código de grupo já está em uso");
        }
        Role copy = new Role();
        copy.setCode(newCode);
        copy.setName(request.newName().trim());
        copy.setDescription(source.getDescription());
        copy.setGroupType(Role.GroupType.CUSTOM);
        copy.setDefaultScope(source.getDefaultScope());
        copy.setDefaultGroup(false);
        copy.setSystemGroup(false);
        copy.setAllowsAdministration(source.getAllowsAdministration());
        copy.setVisualPriority(source.getVisualPriority());
        copy.setOrganization(source.getOrganization());
        copy.setActive(true);
        Role saved = roleRepository.save(copy);

        List<GroupPermissionAssignment> sourcePerms =
                groupPermissionAssignmentRepository.findByGroupIdAndActiveTrue(source.getId());
        User actor = privilegeGuard.requireActor();
        List<String> codes = sourcePerms.stream().map(a -> a.getPermission().getCode()).toList();
        privilegeGuard.assertCanGrantPermissions(actor, codes);

        for (GroupPermissionAssignment src : sourcePerms) {
            if (src.getStatus() != GroupPermissionAssignment.Status.ACTIVE) {
                continue;
            }
            GroupPermissionAssignment gpa = new GroupPermissionAssignment();
            gpa.setGroup(saved);
            gpa.setPermission(src.getPermission());
            gpa.setGrantType(GroupPermissionAssignment.GrantType.ALLOW);
            gpa.setScope(src.getScope());
            gpa.setOrganization(src.getOrganization());
            gpa.setStore(src.getStore());
            gpa.setStatus(GroupPermissionAssignment.Status.ACTIVE);
            gpa.setGrantedBy(CurrentUser.id().orElse(null));
            gpa.setReason("Duplicado de " + source.getCode());
            gpa.setActive(true);
            groupPermissionAssignmentRepository.save(gpa);
            if (saved.getPermissions() != null) {
                saved.getPermissions().add(src.getPermission());
            }
        }
        roleRepository.save(saved);
        accessAuditService.record(
                orgId(saved),
                CurrentUser.id().orElse(null),
                null,
                saved.getId(),
                null,
                "GROUP_DUPLICATE",
                "Duplicado de " + source.getCode());
        return toResponse(saved);
    }

    @Transactional
    public void softDelete(UUID id) {
        Role group = requireGroup(id);
        if (Boolean.TRUE.equals(group.getSystemGroup())) {
            throw new BusinessRuleException("Grupo de sistema não pode ser excluído");
        }
        long members = userGroupAssignmentRepository.countByGroupIdAndStatusAndActiveTrue(
                id, UserGroupAssignment.Status.ACTIVE);
        if (members > 0) {
            throw new BusinessRuleException("Grupo com usuários ativos não pode ser excluído; inative-o");
        }
        group.setActive(false);
        roleRepository.save(group);
        accessAuditService.record(
                orgId(group), CurrentUser.id().orElse(null), null, id, null, "GROUP_SOFT_DELETE", "Exclusão lógica");
    }

    @Transactional(readOnly = true)
    public List<AccessChangeHistory> history(UUID groupId) {
        requireGroup(groupId);
        return historyRepository.findByGroupIdOrderByOccurredAtDesc(groupId);
    }

    public Role requireGroup(UUID id) {
        return roleRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Grupo", id));
    }

    private void ensureNotLastSuperAdminGroup() {
        long activeAdmins = roleRepository.countByCodeAndActiveTrue(AccessPrivilegeGuard.SUPER_ADMIN_GROUP_CODE);
        if (activeAdmins <= 1) {
            throw new BusinessRuleException("Não é permitido inativar o último grupo de superadministradores");
        }
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

    private AccessGroupResponse toResponse(Role group) {
        long users = userGroupAssignmentRepository.countByGroupIdAndStatusAndActiveTrue(
                group.getId(), br.com.systemcommerce.access.entity.UserGroupAssignment.Status.ACTIVE);
        long perms = groupPermissionAssignmentRepository.findByGroupIdAndActiveTrue(group.getId()).stream()
                .filter(a -> a.getStatus()
                        == br.com.systemcommerce.access.entity.GroupPermissionAssignment.Status.ACTIVE)
                .count();
        return new AccessGroupResponse(
                group.getId(),
                group.getCode(),
                group.getName(),
                group.getDescription(),
                group.getGroupType(),
                group.getDefaultScope(),
                group.getSystemGroup(),
                group.getDefaultGroup(),
                group.getAllowsAdministration(),
                group.getVisualPriority(),
                group.getOrganization() != null ? group.getOrganization().getId() : null,
                group.getActive(),
                group.getVersion(),
                users,
                perms,
                group.getUpdatedAt());
    }
}
