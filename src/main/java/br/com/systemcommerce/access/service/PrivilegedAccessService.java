package br.com.systemcommerce.access.service;

import br.com.systemcommerce.access.entity.AccessAuditEvent;
import br.com.systemcommerce.access.entity.GroupPermissionAssignment;
import br.com.systemcommerce.access.entity.PrivilegedAccessApproval;
import br.com.systemcommerce.access.entity.PrivilegedAccessRequest;
import br.com.systemcommerce.access.repository.GroupPermissionAssignmentRepository;
import br.com.systemcommerce.access.repository.PrivilegedAccessApprovalRepository;
import br.com.systemcommerce.access.repository.PrivilegedAccessRequestRepository;
import br.com.systemcommerce.access.repository.UserGroupAssignmentRepository;
import br.com.systemcommerce.shared.exception.AccessDeniedBusinessException;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.user.entity.Permission;
import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.PermissionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PrivilegedAccessService {

    private static final Logger log = LoggerFactory.getLogger(PrivilegedAccessService.class);

    private final PrivilegedAccessRequestRepository requestRepository;
    private final PrivilegedAccessApprovalRepository approvalRepository;
    private final PermissionRepository permissionRepository;
    private final GroupPermissionAssignmentRepository groupPermissionAssignmentRepository;
    private final UserGroupAssignmentRepository userGroupAssignmentRepository;
    private final AccessPrivilegeGuard privilegeGuard;
    private final AccessAuditService accessAuditService;
    private final AccessAuditEventService accessAuditEventService;
    private final AccessGroupService accessGroupService;

    public boolean requiresApproval(Permission permission) {
        return Boolean.TRUE.equals(permission.getSensitive())
                || "CRITICAL".equalsIgnoreCase(permission.getRiskLevel())
                || Boolean.TRUE.equals(permission.getRequiresDualApproval());
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public PrivilegedAccessRequest requestGrant(
            UUID groupId, UUID permissionId, String justification, Instant validTo) {
        User requester = privilegeGuard.requireActor();
        if (justification == null || justification.isBlank()) {
            throw new BusinessRuleException("Justificativa obrigatória para permissão sensível");
        }
        Role group = accessGroupService.requireGroup(groupId);
        Permission permission = permissionRepository
                .findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Permissão", permissionId));
        privilegeGuard.assertCanGrantPermissions(requester, List.of(permission.getCode()));

        PrivilegedAccessRequest req = new PrivilegedAccessRequest();
        req.setRequester(requester);
        req.setTargetGroup(group);
        req.setPermission(permission);
        req.setJustification(justification.trim());
        req.setValidTo(validTo);
        req.setStatus(PrivilegedAccessRequest.Status.PENDING);
        req.setActive(true);
        PrivilegedAccessRequest saved = requestRepository.save(req);
        accessAuditEventService.record(
                "PRIVILEGED_ACCESS_REQUEST",
                AccessAuditEvent.Result.SUCCESS,
                requester.getId(),
                null,
                groupId,
                permissionId,
                permission.getCode(),
                null,
                justification,
                null,
                null,
                null,
                null);
        log.warn(
                "PRIVILEGED_ACCESS_REQUESTED user={} permission={} group={}",
                requester.getId(),
                permission.getCode(),
                group.getCode());
        return saved;
    }

    @Transactional
    public PrivilegedAccessRequest decide(UUID requestId, boolean approve, String reason) {
        User approver = privilegeGuard.requireActor();
        if (!privilegeGuard.resolveHasAuthority(approver, "PRIVILEGED_ACCESS_APPROVE")
                && !privilegeGuard.isSuperAdmin(approver)) {
            throw new AccessDeniedBusinessException("Sem permissão para aprovar acesso privilegiado");
        }
        PrivilegedAccessRequest req = requestRepository
                .findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação", requestId));
        if (req.getStatus() != PrivilegedAccessRequest.Status.PENDING) {
            throw new BusinessRuleException("Solicitação não está pendente");
        }
        if (approver.getId().equals(req.getRequester().getId())) {
            throw new AccessDeniedBusinessException("Autoaprovação não permitida");
        }

        PrivilegedAccessApproval approval = new PrivilegedAccessApproval();
        approval.setRequest(req);
        approval.setApprover(approver);
        approval.setDecision(
                approve ? PrivilegedAccessApproval.Decision.APPROVED : PrivilegedAccessApproval.Decision.REJECTED);
        approval.setReason(reason);
        approval.setDecidedAt(Instant.now());
        approval.setActive(true);
        approvalRepository.save(approval);

        if (!approve) {
            req.setStatus(PrivilegedAccessRequest.Status.REJECTED);
            req.setDecidedAt(Instant.now());
            req.setDecidedBy(approver.getId());
            req.setDecisionReason(reason);
            accessAuditEventService.record(
                    "PRIVILEGED_ACCESS_REJECTED",
                    AccessAuditEvent.Result.DENIED,
                    approver.getId(),
                    req.getRequester().getId(),
                    req.getTargetGroup().getId(),
                    req.getPermission().getId(),
                    req.getPermission().getCode(),
                    null,
                    reason,
                    null,
                    null,
                    null,
                    null);
            return requestRepository.save(req);
        }

        boolean dual = Boolean.TRUE.equals(req.getPermission().getRequiresDualApproval());
        long approvals = approvalRepository.countByRequestIdAndDecision(
                requestId, PrivilegedAccessApproval.Decision.APPROVED);
        if (dual && approvals < 2 && !privilegeGuard.isSuperAdmin(approver)) {
            accessAuditEventService.record(
                    "PRIVILEGED_ACCESS_PARTIAL_APPROVAL",
                    AccessAuditEvent.Result.SUCCESS,
                    approver.getId(),
                    req.getRequester().getId(),
                    req.getTargetGroup().getId(),
                    req.getPermission().getId(),
                    req.getPermission().getCode(),
                    null,
                    "Aguardando segundo aprovador",
                    null,
                    null,
                    null,
                    null);
            return req;
        }

        applyGrant(req, approver, reason);
        req.setStatus(PrivilegedAccessRequest.Status.APPROVED);
        req.setDecidedAt(Instant.now());
        req.setDecidedBy(approver.getId());
        req.setDecisionReason(reason);
        return requestRepository.save(req);
    }

    private void applyGrant(PrivilegedAccessRequest req, User approver, String reason) {
        Role group = req.getTargetGroup();
        Permission permission = req.getPermission();
        var existing =
                groupPermissionAssignmentRepository.findByGroupIdAndPermissionId(group.getId(), permission.getId());
        GroupPermissionAssignment gpa = existing.orElseGet(GroupPermissionAssignment::new);
        gpa.setGroup(group);
        gpa.setPermission(permission);
        gpa.setGrantType(GroupPermissionAssignment.GrantType.ALLOW);
        gpa.setScope(GroupPermissionAssignment.Scope.ORGANIZATION);
        gpa.setStatus(GroupPermissionAssignment.Status.ACTIVE);
        gpa.setActive(true);
        gpa.setGrantedBy(approver.getId());
        gpa.setReason(reason != null ? reason : req.getJustification());
        gpa.setValidFrom(Instant.now());
        gpa.setValidTo(req.getValidTo());
        groupPermissionAssignmentRepository.save(gpa);

        List<UUID> memberIds = userGroupAssignmentRepository.findByGroupIdAndActiveTrue(group.getId()).stream()
                .map(a -> a.getUser().getId())
                .distinct()
                .toList();
        accessAuditService.bumpAccessVersion(memberIds);

        accessAuditEventService.record(
                "PRIVILEGED_ACCESS_APPROVED",
                AccessAuditEvent.Result.SUCCESS,
                approver.getId(),
                req.getRequester().getId(),
                group.getId(),
                permission.getId(),
                permission.getCode(),
                "ORGANIZATION",
                reason,
                null,
                permission.getCode(),
                null,
                null);
    }

    @Transactional(readOnly = true)
    public List<PrivilegedAccessRequest> listPending() {
        return requestRepository.findByStatusOrderByCreatedAtDesc(PrivilegedAccessRequest.Status.PENDING);
    }
}
