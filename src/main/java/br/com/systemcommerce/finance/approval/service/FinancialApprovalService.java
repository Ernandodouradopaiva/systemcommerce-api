package br.com.systemcommerce.finance.approval.service;

import br.com.systemcommerce.finance.approval.dto.ApprovalDtos.*;
import br.com.systemcommerce.finance.approval.entity.FinancialApprovalPolicy;
import br.com.systemcommerce.finance.approval.entity.FinancialApprovalRequest;
import br.com.systemcommerce.finance.approval.repository.FinancialApprovalPolicyRepository;
import br.com.systemcommerce.finance.approval.repository.FinancialApprovalRequestRepository;
import br.com.systemcommerce.finance.security.FinanceAuditEvents;
import br.com.systemcommerce.finance.security.FinanceAuditService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FinancialApprovalService {

    private final FinancialApprovalPolicyRepository policyRepository;
    private final FinancialApprovalRequestRepository requestRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final FinanceAuditService financeAuditService;

    @Transactional
    public PolicyResponse getOrCreatePolicy(UUID organizationId) {
        return toPolicy(requirePolicy(organizationId));
    }

    @Transactional
    public PolicyResponse updatePolicy(UUID organizationId, PolicyUpdateRequest request) {
        FinancialApprovalPolicy policy = requirePolicy(organizationId);
        if (request.requirePaymentApproval() != null) {
            policy.setRequirePaymentApproval(request.requirePaymentApproval());
        }
        if (request.paymentApprovalThreshold() != null) {
            policy.setPaymentApprovalThreshold(nz(request.paymentApprovalThreshold()));
        }
        if (request.requireReversalApproval() != null) {
            policy.setRequireReversalApproval(request.requireReversalApproval());
        }
        if (request.requireDiscountApproval() != null) {
            policy.setRequireDiscountApproval(request.requireDiscountApproval());
        }
        if (request.discountApprovalThreshold() != null) {
            policy.setDiscountApprovalThreshold(nz(request.discountApprovalThreshold()));
        }
        if (request.requireTransferApproval() != null) {
            policy.setRequireTransferApproval(request.requireTransferApproval());
        }
        if (request.transferApprovalThreshold() != null) {
            policy.setTransferApprovalThreshold(nz(request.transferApprovalThreshold()));
        }
        if (request.requirePeriodReopenApproval() != null) {
            policy.setRequirePeriodReopenApproval(request.requirePeriodReopenApproval());
        }
        if (request.requireManualEntryApproval() != null) {
            policy.setRequireManualEntryApproval(request.requireManualEntryApproval());
        }
        if (request.manualEntryApprovalThreshold() != null) {
            policy.setManualEntryApprovalThreshold(nz(request.manualEntryApprovalThreshold()));
        }
        return toPolicy(policyRepository.save(policy));
    }

    /**
     * @return null se a operação pode seguir sem aprovação; caso contrário a solicitação PENDING/APPROVED.
     */
    @Transactional
    public FinancialApprovalRequest requireOrCreateIfNeeded(
            UUID organizationId,
            UUID storeId,
            FinancialApprovalRequest.OperationType type,
            String sourceEntityType,
            UUID sourceEntityId,
            BigDecimal amount,
            String reason,
            String payloadJson,
            String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = requestRepository.findByOrganizationIdAndIdempotencyKey(organizationId, idempotencyKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        if (!needsApproval(organizationId, type, amount)) {
            return null;
        }
        return createInternal(
                organizationId, storeId, type, sourceEntityType, sourceEntityId, amount, reason, payloadJson, idempotencyKey);
    }

    @Transactional
    public ApprovalResponse create(CreateApprovalRequest request) {
        FinancialApprovalRequest saved = createInternal(
                request.organizationId(),
                request.storeId(),
                request.operationType(),
                request.sourceEntityType(),
                request.sourceEntityId(),
                request.amount(),
                request.reason(),
                request.payloadJson(),
                request.idempotencyKey());
        return toResponse(saved);
    }

    @Transactional
    public ApprovalResponse decide(UUID id, DecideRequest request) {
        FinancialApprovalRequest approval = requestRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de aprovação não encontrada"));
        if (approval.getStatus() != FinancialApprovalRequest.Status.PENDING) {
            throw new BusinessRuleException("Somente solicitações PENDING podem ser decididas");
        }
        UUID actor = CurrentUser.requireId();
        if (actor.equals(approval.getRequestedBy())) {
            throw new BusinessRuleException("Aprovação em duas etapas: o solicitante não pode decidir a própria solicitação");
        }
        String decision = request.decision().trim().toUpperCase();
        if ("APPROVE".equals(decision) || "APPROVED".equals(decision)) {
            approval.setStatus(FinancialApprovalRequest.Status.APPROVED);
        } else if ("REJECT".equals(decision) || "REJECTED".equals(decision)) {
            approval.setStatus(FinancialApprovalRequest.Status.REJECTED);
        } else {
            throw new BusinessRuleException("Decisão inválida. Use APPROVE ou REJECT");
        }
        approval.setDecisionNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        approval.setDecidedBy(actor);
        approval.setDecidedAt(Instant.now());
        FinancialApprovalRequest saved = requestRepository.save(approval);
        financeAuditService.success(
                FinanceAuditEvents.APPROVAL_DECIDE,
                "FinancialApprovalRequest",
                saved.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                "Decisão: " + saved.getStatus());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> listPending(UUID organizationId) {
        return requestRepository
                .findByOrganizationIdAndStatusOrderByRequestedAtDesc(
                        organizationId, FinancialApprovalRequest.Status.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ApprovalResponse get(UUID id) {
        return toResponse(requestRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de aprovação não encontrada")));
    }

    /** Valida se há aprovação APPROVED para executar a operação (ou lança). */
    @Transactional(readOnly = true)
    public void assertApprovedOrNotRequired(
            UUID organizationId,
            FinancialApprovalRequest.OperationType type,
            BigDecimal amount,
            UUID approvalRequestId) {
        if (!needsApproval(organizationId, type, amount)) {
            return;
        }
        if (approvalRequestId == null) {
            throw new BusinessRuleException(
                    "Operação exige aprovação em duas etapas. Crie uma solicitação e informe approvalRequestId.");
        }
        FinancialApprovalRequest approval = requestRepository
                .findById(approvalRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de aprovação não encontrada"));
        if (approval.getOrganization().getId() != null
                && !approval.getOrganization().getId().equals(organizationId)) {
            throw new BusinessRuleException("Aprovação não pertence à organização");
        }
        if (approval.getOperationType() != type) {
            throw new BusinessRuleException("Tipo de aprovação incompatível com a operação");
        }
        if (approval.getStatus() != FinancialApprovalRequest.Status.APPROVED
                && approval.getStatus() != FinancialApprovalRequest.Status.EXECUTED) {
            throw new BusinessRuleException("Operação aguarda aprovação (status=" + approval.getStatus() + ")");
        }
    }

    @Transactional
    public void markExecuted(UUID approvalRequestId) {
        if (approvalRequestId == null) {
            return;
        }
        requestRepository.findById(approvalRequestId).ifPresent(a -> {
            if (a.getStatus() == FinancialApprovalRequest.Status.APPROVED) {
                a.setStatus(FinancialApprovalRequest.Status.EXECUTED);
                a.setExecutedAt(Instant.now());
                requestRepository.save(a);
            }
        });
    }

    public boolean needsApproval(UUID organizationId, FinancialApprovalRequest.OperationType type, BigDecimal amount) {
        FinancialApprovalPolicy policy = requirePolicy(organizationId);
        BigDecimal value = nz(amount);
        return switch (type) {
            case HIGH_PAYMENT -> Boolean.TRUE.equals(policy.getRequirePaymentApproval())
                    && value.compareTo(nz(policy.getPaymentApprovalThreshold())) >= 0;
            case REVERSAL -> Boolean.TRUE.equals(policy.getRequireReversalApproval());
            case DISCOUNT -> Boolean.TRUE.equals(policy.getRequireDiscountApproval())
                    && value.compareTo(nz(policy.getDiscountApprovalThreshold())) >= 0;
            case TRANSFER -> Boolean.TRUE.equals(policy.getRequireTransferApproval())
                    && value.compareTo(nz(policy.getTransferApprovalThreshold())) >= 0;
            case PERIOD_REOPEN -> Boolean.TRUE.equals(policy.getRequirePeriodReopenApproval());
            case MANUAL_ENTRY -> Boolean.TRUE.equals(policy.getRequireManualEntryApproval())
                    && value.compareTo(nz(policy.getManualEntryApprovalThreshold())) >= 0;
        };
    }

    private FinancialApprovalRequest createInternal(
            UUID organizationId,
            UUID storeId,
            FinancialApprovalRequest.OperationType type,
            String sourceEntityType,
            UUID sourceEntityId,
            BigDecimal amount,
            String reason,
            String payloadJson,
            String idempotencyKey) {
        FinancialApprovalRequest req = new FinancialApprovalRequest();
        req.setOrganization(organizationService.requireUsable(organizationId));
        if (storeId != null) {
            req.setStore(storeService.requireUsable(storeId));
        }
        req.setOperationType(type);
        req.setStatus(FinancialApprovalRequest.Status.PENDING);
        req.setSourceEntityType(MoneyAndQuantityUtils.requireText(sourceEntityType, "Tipo de origem"));
        req.setSourceEntityId(sourceEntityId);
        req.setAmount(amount != null ? amount.setScale(2, RoundingMode.HALF_UP) : null);
        req.setReason(MoneyAndQuantityUtils.blankToNull(reason));
        req.setPayloadJson(MoneyAndQuantityUtils.blankToNull(payloadJson));
        req.setIdempotencyKey(idempotencyKey);
        req.setRequestedAt(Instant.now());
        CurrentUser.id().ifPresent(req::setRequestedBy);
        FinancialApprovalRequest saved = requestRepository.save(req);
        financeAuditService.success(
                FinanceAuditEvents.APPROVAL_REQUEST,
                "FinancialApprovalRequest",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                "Solicitação " + type + " criada");
        return saved;
    }

    private FinancialApprovalPolicy requirePolicy(UUID organizationId) {
        return policyRepository
                .findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    FinancialApprovalPolicy p = new FinancialApprovalPolicy();
                    p.setOrganization(organizationService.requireUsable(organizationId));
                    return policyRepository.save(p);
                });
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private PolicyResponse toPolicy(FinancialApprovalPolicy p) {
        return new PolicyResponse(
                p.getId(),
                p.getOrganization().getId(),
                p.getRequirePaymentApproval(),
                p.getPaymentApprovalThreshold(),
                p.getRequireReversalApproval(),
                p.getRequireDiscountApproval(),
                p.getDiscountApprovalThreshold(),
                p.getRequireTransferApproval(),
                p.getTransferApprovalThreshold(),
                p.getRequirePeriodReopenApproval(),
                p.getRequireManualEntryApproval(),
                p.getManualEntryApprovalThreshold());
    }

    private ApprovalResponse toResponse(FinancialApprovalRequest r) {
        return new ApprovalResponse(
                r.getId(),
                r.getOrganization().getId(),
                r.getOperationType().name(),
                r.getStatus().name(),
                r.getSourceEntityType(),
                r.getSourceEntityId(),
                r.getAmount(),
                r.getReason(),
                r.getRequestedAt(),
                r.getRequestedBy(),
                r.getDecidedAt(),
                r.getDecidedBy(),
                r.getDecisionNotes());
    }
}
