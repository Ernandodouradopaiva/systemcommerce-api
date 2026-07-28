package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.dto.UserGroupDtos.AssignGroupRequest;
import br.com.systemcommerce.access.dto.UserGroupDtos.AssignMultipleGroupsRequest;
import br.com.systemcommerce.access.dto.UserGroupDtos.EffectivePermissionsResponse;
import br.com.systemcommerce.access.dto.UserGroupDtos.SetPrimaryGroupRequest;
import br.com.systemcommerce.access.dto.UserGroupDtos.SetValidityRequest;
import br.com.systemcommerce.access.dto.UserGroupDtos.UserGroupAssignmentResponse;
import br.com.systemcommerce.access.entity.AccessChangeHistory;
import br.com.systemcommerce.access.entity.UserGroupAssignment;
import br.com.systemcommerce.access.repository.AccessChangeHistoryRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.organization.repository.OrganizationRepository;
import br.com.systemcommerce.pos.store.repository.StoreRepository;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import br.com.systemcommerce.user.service.PermissionResolver;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserGroupAssignmentService {

    private final UserRepository userRepository;
    private final AccessGroupService accessGroupService;
    private final UserGroupAssignmentRepository assignmentRepository;
    private final AccessChangeHistoryRepository historyRepository;
    private final OrganizationRepository organizationRepository;
    private final StoreRepository storeRepository;
    private final AccessAuditService accessAuditService;
    private final AccessPrivilegeGuard privilegeGuard;
    private final PermissionResolver permissionResolver;

    @Transactional(readOnly = true)
    public List<UserGroupAssignmentResponse> listByUser(UUID userId) {
        requireUser(userId);
        return assignmentRepository.findByUserIdAndActiveTrue(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserGroupAssignmentResponse> listByGroup(UUID groupId) {
        accessGroupService.requireGroup(groupId);
        return assignmentRepository.findByGroupIdAndActiveTrue(groupId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EffectivePermissionsResponse effectivePermissions(UUID userId) {
        User user = requireUser(userId);
        return new EffectivePermissionsResponse(
                userId, permissionResolver.resolveRoleCodes(user), permissionResolver.resolvePermissionCodes(user));
    }

    @Transactional(readOnly = true)
    public List<AccessChangeHistory> history(UUID userId) {
        requireUser(userId);
        return historyRepository.findByTargetUserIdOrderByOccurredAtDesc(userId);
    }

    @Transactional
    public UserGroupAssignmentResponse assign(UUID userId, AssignGroupRequest request) {
        privilegeGuard.assertNotSelfGroupChange(userId);
        User actor = privilegeGuard.requireActor();
        User user = requireUser(userId);
        Role group = accessGroupService.requireGroup(request.groupId());
        if (!Boolean.TRUE.equals(group.getActive())) {
            throw new BusinessRuleException("Não é permitido vincular usuário a grupo inativo");
        }

        Optional<UserGroupAssignment> existing =
                assignmentRepository.findByUserAndGroupAndStore(userId, group.getId(), request.storeId());
        if (existing.isPresent()
                && Boolean.TRUE.equals(existing.get().getActive())
                && existing.get().getStatus() == UserGroupAssignment.Status.ACTIVE) {
            throw new ConflictException("Vínculo usuário/grupo já existe neste escopo");
        }

        UserGroupAssignment assignment = existing.orElseGet(UserGroupAssignment::new);
        assignment.setUser(user);
        assignment.setGroup(group);
        if (request.organizationId() != null) {
            assignment.setOrganization(organizationRepository
                    .findById(request.organizationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Organização", request.organizationId())));
        } else {
            assignment.setOrganization(group.getOrganization());
        }
        if (request.storeId() != null) {
            assignment.setStore(storeRepository
                    .findById(request.storeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Loja", request.storeId())));
        }
        assignment.setValidFrom(request.validFrom() != null ? request.validFrom() : Instant.now());
        assignment.setValidTo(request.validTo());
        assignment.setStatus(UserGroupAssignment.Status.ACTIVE);
        assignment.setActive(true);
        assignment.setAssignedBy(actor.getId());
        assignment.setReason(request.reason());
        boolean primary = Boolean.TRUE.equals(request.primaryGroup());
        if (primary) {
            clearPrimary(userId, request.storeId());
        }
        assignment.setPrimaryGroup(primary);

        UserGroupAssignment saved = assignmentRepository.save(assignment);
        syncLegacyUserRoles(user);
        accessAuditService.record(
                orgId(assignment),
                actor.getId(),
                userId,
                group.getId(),
                null,
                "USER_GROUP_ASSIGN",
                group.getCode());
        accessAuditService.bumpAccessVersion(List.of(userId));
        return toResponse(saved);
    }

    @Transactional
    public List<UserGroupAssignmentResponse> assignMultiple(UUID userId, AssignMultipleGroupsRequest request) {
        List<UserGroupAssignmentResponse> result = new ArrayList<>();
        for (AssignGroupRequest item : request.assignments()) {
            result.add(assign(userId, item));
        }
        return result;
    }

    @Transactional
    public void remove(UUID assignmentId, String reason) {
        UserGroupAssignment assignment = assignmentRepository
                .findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de grupo", assignmentId));
        privilegeGuard.assertNotSelfGroupChange(assignment.getUser().getId());
        User actor = privilegeGuard.requireActor();

        if (Boolean.TRUE.equals(assignment.getGroup().getAllowsAdministration())
                || AccessPrivilegeGuard.SUPER_ADMIN_GROUP_CODE.equals(assignment.getGroup().getCode())) {
            ensureNotLastAdminMembership(assignment);
        }

        assignment.setStatus(UserGroupAssignment.Status.INACTIVE);
        assignment.setActive(false);
        assignment.setReason(reason);
        assignmentRepository.save(assignment);
        syncLegacyUserRoles(assignment.getUser());
        accessAuditService.record(
                orgId(assignment),
                actor.getId(),
                assignment.getUser().getId(),
                assignment.getGroup().getId(),
                null,
                "USER_GROUP_REMOVE",
                reason != null ? reason : assignment.getGroup().getCode());
        accessAuditService.bumpAccessVersion(List.of(assignment.getUser().getId()));
    }

    @Transactional
    public UserGroupAssignmentResponse setPrimary(UUID userId, SetPrimaryGroupRequest request) {
        privilegeGuard.assertNotSelfGroupChange(userId);
        requireUser(userId);
        clearPrimary(userId, request.storeId());
        UserGroupAssignment assignment = assignmentRepository
                .findByUserAndGroupAndStore(userId, request.groupId(), request.storeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de grupo", request.groupId()));
        assignment.setPrimaryGroup(true);
        UserGroupAssignment saved = assignmentRepository.save(assignment);
        accessAuditService.record(
                orgId(saved),
                CurrentUser.id().orElse(null),
                userId,
                request.groupId(),
                null,
                "USER_GROUP_PRIMARY",
                saved.getGroup().getCode());
        return toResponse(saved);
    }

    @Transactional
    public UserGroupAssignmentResponse setValidity(UUID assignmentId, SetValidityRequest request) {
        UserGroupAssignment assignment = assignmentRepository
                .findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de grupo", assignmentId));
        privilegeGuard.assertNotSelfGroupChange(assignment.getUser().getId());
        if (request.validFrom() != null) {
            assignment.setValidFrom(request.validFrom());
        }
        assignment.setValidTo(request.validTo());
        if (request.reason() != null) {
            assignment.setReason(request.reason());
        }
        UserGroupAssignment saved = assignmentRepository.save(assignment);
        accessAuditService.bumpAccessVersion(List.of(assignment.getUser().getId()));
        accessAuditService.record(
                orgId(saved),
                CurrentUser.id().orElse(null),
                assignment.getUser().getId(),
                assignment.getGroup().getId(),
                null,
                "USER_GROUP_VALIDITY",
                request.reason());
        return toResponse(saved);
    }

    @Transactional
    public UserGroupAssignmentResponse activate(UUID assignmentId) {
        UserGroupAssignment assignment = assignmentRepository
                .findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de grupo", assignmentId));
        privilegeGuard.assertNotSelfGroupChange(assignment.getUser().getId());
        assignment.setStatus(UserGroupAssignment.Status.ACTIVE);
        assignment.setActive(true);
        UserGroupAssignment saved = assignmentRepository.save(assignment);
        syncLegacyUserRoles(assignment.getUser());
        accessAuditService.bumpAccessVersion(List.of(assignment.getUser().getId()));
        return toResponse(saved);
    }

    @Transactional
    public UserGroupAssignmentResponse deactivate(UUID assignmentId) {
        UserGroupAssignment assignment = assignmentRepository
                .findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo de grupo", assignmentId));
        privilegeGuard.assertNotSelfGroupChange(assignment.getUser().getId());
        if (Boolean.TRUE.equals(assignment.getGroup().getAllowsAdministration())
                || AccessPrivilegeGuard.SUPER_ADMIN_GROUP_CODE.equals(assignment.getGroup().getCode())) {
            ensureNotLastAdminMembership(assignment);
        }
        assignment.setStatus(UserGroupAssignment.Status.INACTIVE);
        assignment.setActive(false);
        UserGroupAssignment saved = assignmentRepository.save(assignment);
        syncLegacyUserRoles(assignment.getUser());
        accessAuditService.bumpAccessVersion(List.of(assignment.getUser().getId()));
        return toResponse(saved);
    }

    private void ensureNotLastAdminMembership(UserGroupAssignment removing) {
        long otherAdmins = assignmentRepository.countOtherAdminUsers(
                Instant.now(), AccessPrivilegeGuard.SUPER_ADMIN_GROUP_CODE, removing.getId());
        if (otherAdmins == 0) {
            throw new BusinessRuleException("Não é permitido remover o último vínculo administrativo do sistema");
        }
    }

    private void clearPrimary(UUID userId, UUID storeId) {
        for (UserGroupAssignment a : assignmentRepository.findByUserIdAndActiveTrue(userId)) {
            UUID aStore = a.getStore() != null ? a.getStore().getId() : null;
            boolean sameStore = (storeId == null && aStore == null) || (storeId != null && storeId.equals(aStore));
            if (sameStore && Boolean.TRUE.equals(a.getPrimaryGroup())) {
                a.setPrimaryGroup(false);
                assignmentRepository.save(a);
            }
        }
    }

    private void syncLegacyUserRoles(User user) {
        User managed = userRepository
                .findWithRolesById(user.getId())
                .orElse(user);
        Set<Role> roles = new HashSet<>();
        Instant now = Instant.now();
        for (UserGroupAssignment a : assignmentRepository.findEffectiveAssignments(user.getId(), now)) {
            roles.add(a.getGroup());
        }
        managed.getRoles().clear();
        managed.getRoles().addAll(roles);
        userRepository.save(managed);
    }

    private User requireUser(UUID userId) {
        return userRepository
                .findWithRolesById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário", userId));
    }

    private UUID orgId(UserGroupAssignment a) {
        return a.getOrganization() != null ? a.getOrganization().getId() : null;
    }

    private UserGroupAssignmentResponse toResponse(UserGroupAssignment a) {
        return new UserGroupAssignmentResponse(
                a.getId(),
                a.getUser().getId(),
                a.getGroup().getId(),
                a.getGroup().getCode(),
                a.getGroup().getName(),
                a.getOrganization() != null ? a.getOrganization().getId() : null,
                a.getStore() != null ? a.getStore().getId() : null,
                a.getValidFrom(),
                a.getValidTo(),
                a.getStatus(),
                a.getPrimaryGroup(),
                a.getReason(),
                a.getActive());
    }
}
