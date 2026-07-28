package br.com.systemcommerce.finance.payable.service;

import br.com.systemcommerce.finance.account.service.FinancialCategoryService;
import br.com.systemcommerce.finance.costcenter.service.CostCenterService;
import br.com.systemcommerce.finance.payable.dto.PayableBalanceResponse;
import br.com.systemcommerce.finance.payable.dto.PayableCancelRequest;
import br.com.systemcommerce.finance.payable.dto.PayableCreateRequest;
import br.com.systemcommerce.finance.payable.dto.PayableFromPurchaseRequest;
import br.com.systemcommerce.finance.payable.dto.PayableInstallmentRequest;
import br.com.systemcommerce.finance.payable.dto.PayableInstallmentResponse;
import br.com.systemcommerce.finance.payable.dto.PayableOriginResponse;
import br.com.systemcommerce.finance.payable.dto.PayableResponse;
import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import br.com.systemcommerce.finance.payable.entity.Payable;
import br.com.systemcommerce.finance.payable.entity.PayableInstallment;
import br.com.systemcommerce.finance.payable.entity.PayableOrigin;
import br.com.systemcommerce.finance.payable.entity.PayableStatusHistory;
import br.com.systemcommerce.finance.payable.repository.FinanceGenerationSettingsRepository;
import br.com.systemcommerce.finance.payable.repository.PayableInstallmentRepository;
import br.com.systemcommerce.finance.payable.repository.PayableOriginRepository;
import br.com.systemcommerce.finance.payable.repository.PayableRepository;
import br.com.systemcommerce.finance.payable.repository.PayableStatusHistoryRepository;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import br.com.systemcommerce.finance.paymentcatalog.service.PaymentCatalogService;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.purchase.entity.PurchaseOrder;
import br.com.systemcommerce.purchase.entity.PurchaseReceipt;
import br.com.systemcommerce.purchase.repository.PurchaseReceiptRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.service.SupplierService;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PayableService {

    private final PayableRepository payableRepository;
    private final PayableInstallmentRepository installmentRepository;
    private final PayableOriginRepository originRepository;
    private final PayableStatusHistoryRepository statusHistoryRepository;
    private final FinanceGenerationSettingsRepository settingsRepository;
    private final PurchaseReceiptRepository purchaseReceiptRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final SupplierService supplierService;
    private final PaymentCatalogService paymentCatalogService;
    private final FinancialCategoryService financialCategoryService;
    private final CostCenterService costCenterService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<PayableResponse> list(
            UUID organizationId, UUID supplierId, Payable.Status status, String search, Pageable pageable) {
        refreshOverdueFlags(organizationId);
        Specification<Payable> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (supplierId != null) {
                preds.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(search)) {
                String like = "%" + search.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("documentNumber")), like),
                        cb.like(cb.lower(root.get("notes")), like)));
            }
            return cb.and(preds.toArray(Predicate[]::new));
        };
        return payableRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public PayableResponse getById(UUID id) {
        Payable payable = getDetailed(id);
        refreshPayableOverdue(payable);
        return toResponse(payable);
    }

    @Transactional(readOnly = true)
    public PayableBalanceResponse balance(UUID id) {
        Payable payable = getDetailed(id);
        refreshPayableOverdue(payable);
        return new PayableBalanceResponse(
                payable.getId(),
                payable.getTotalAmount(),
                payable.getPaidAmount(),
                payable.getBalanceAmount(),
                payable.getStatus());
    }

    @Transactional(readOnly = true)
    public List<PayableInstallmentResponse> installments(UUID payableId) {
        getDetailed(payableId);
        return installmentRepository.findByPayableIdOrderByInstallmentNumberAsc(payableId).stream()
                .map(this::toInstallmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayableInstallmentResponse> agenda(UUID organizationId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(30);
        return installmentRepository.findAgenda(organizationId, start, end).stream()
                .peek(i -> i.refreshOverdue(LocalDate.now()))
                .filter(i -> i.getStatus() != PayableInstallment.Status.PAID
                        && i.getStatus() != PayableInstallment.Status.CANCELLED)
                .map(this::toInstallmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PayableStatusHistory> history(UUID id) {
        getDetailed(id);
        return statusHistoryRepository.findByPayableIdOrderByChangedAtAsc(id);
    }

    @Transactional
    public PayableResponse createManual(PayableCreateRequest request) {
        return createInternal(request, PayableOrigin.OriginType.MANUAL_EXPENSE, null, null, true);
    }

    @Transactional
    public PayableResponse generateFromPurchaseReceipt(PayableFromPurchaseRequest request) {
        PurchaseReceipt receipt = purchaseReceiptRepository
                .findDetailedById(request.purchaseReceiptId())
                .orElseThrow(() -> new ResourceNotFoundException("Recebimento não encontrado"));
        if (!receipt.isPosted()) {
            throw new BusinessRuleException("Somente recebimento postado no estoque pode gerar conta a pagar");
        }
        if (originRepository.existsByOriginTypeAndOriginDocumentId(
                PayableOrigin.OriginType.PURCHASE_RECEIPT, receipt.getId())) {
            return originRepository
                    .findByOriginTypeAndOriginDocumentId(PayableOrigin.OriginType.PURCHASE_RECEIPT, receipt.getId())
                    .map(o -> toResponse(getDetailed(o.getPayable().getId())))
                    .orElseThrow();
        }

        PurchaseOrder order = receipt.getPurchaseOrder();
        FinanceGenerationSettings settings = settingsRepository
                .findByOrganizationId(receipt.getOrganization().getId())
                .orElse(null);

        ReceiptFinanceCalculator.Breakdown breakdown =
                ReceiptFinanceCalculator.calculate(receipt, order, settings);
        if (breakdown.merchandise().compareTo(BigDecimal.ZERO) <= 0
                && breakdown.freight().compareTo(BigDecimal.ZERO) <= 0
                && breakdown.tax().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Recebimento sem valor aceito para gerar conta a pagar");
        }

        LocalDate issue = receipt.getInvoiceIssuedAt() != null
                ? receipt.getInvoiceIssuedAt()
                : (receipt.getReceiptDate() != null ? receipt.getReceiptDate() : LocalDate.now());
        String docNumber = StringUtils.hasText(receipt.getInvoiceNumber())
                ? receipt.getInvoiceNumber()
                : (StringUtils.hasText(receipt.getReceiptNumber())
                        ? receipt.getReceiptNumber()
                        : order.getOrderNumber());

        String notes = "Gerada do recebimento "
                + receipt.getReceiptNumber()
                + " (pedido "
                + order.getOrderNumber()
                + "). Mercadoria="
                + breakdown.merchandise()
                + "; pedido total="
                + nz(order.getTotalAmount())
                + "; diferença preservada="
                + breakdown.orderVsReceivedDiff();

        PayableResponse main = createInternal(
                new PayableCreateRequest(
                        receipt.getOrganization().getId(),
                        receipt.getStore() != null ? receipt.getStore().getId() : null,
                        receipt.getSupplier().getId(),
                        request.paymentConditionId(),
                        request.financialCategoryId(),
                        request.costCenterId(),
                        docNumber,
                        issue,
                        issue,
                        breakdown.mainAmount(),
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        notes,
                        request.idempotencyKey() != null
                                ? request.idempotencyKey()
                                : "auto-receipt-" + receipt.getId(),
                        true,
                        buildInstallmentsFromCondition(
                                request.paymentConditionId(), breakdown.mainAmount(), issue)),
                PayableOrigin.OriginType.PURCHASE_RECEIPT,
                receipt.getId(),
                docNumber,
                false);

        if (breakdown.freightSeparate().compareTo(BigDecimal.ZERO) > 0) {
            createInternal(
                    new PayableCreateRequest(
                            receipt.getOrganization().getId(),
                            receipt.getStore() != null ? receipt.getStore().getId() : null,
                            receipt.getSupplier().getId(),
                            request.paymentConditionId(),
                            request.financialCategoryId(),
                            request.costCenterId(),
                            docNumber + "-FRETE",
                            issue,
                            issue,
                            breakdown.freightSeparate(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            "Frete segregado do recebimento " + receipt.getReceiptNumber(),
                            "auto-receipt-freight-" + receipt.getId(),
                            true,
                            buildInstallmentsFromCondition(
                                    request.paymentConditionId(), breakdown.freightSeparate(), issue)),
                    PayableOrigin.OriginType.FREIGHT,
                    receipt.getId(),
                    docNumber + "-FRETE",
                    false);
        }

        if (breakdown.taxSeparate().compareTo(BigDecimal.ZERO) > 0) {
            createInternal(
                    new PayableCreateRequest(
                            receipt.getOrganization().getId(),
                            receipt.getStore() != null ? receipt.getStore().getId() : null,
                            receipt.getSupplier().getId(),
                            request.paymentConditionId(),
                            request.financialCategoryId(),
                            request.costCenterId(),
                            docNumber + "-IMP",
                            issue,
                            issue,
                            breakdown.taxSeparate(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            "Impostos segregados do recebimento " + receipt.getReceiptNumber(),
                            "auto-receipt-tax-" + receipt.getId(),
                            true,
                            buildInstallmentsFromCondition(
                                    request.paymentConditionId(), breakdown.taxSeparate(), issue)),
                    PayableOrigin.OriginType.ADJUSTMENT,
                    UUID.nameUUIDFromBytes(("tax-" + receipt.getId()).getBytes()),
                    docNumber + "-IMP",
                    false);
        }

        return main;
    }

    /** Chamado pelo post do recebimento quando a geração automática está habilitada. */
    @Transactional
    public void tryAutoGenerateFromReceipt(PurchaseReceipt receipt) {
        FinanceGenerationSettings settings = settingsRepository
                .findByOrganizationId(receipt.getOrganization().getId())
                .orElse(null);
        if (settings == null || !settings.shouldGeneratePayableOnReceipt()) {
            return;
        }
        if (originRepository.existsByOriginTypeAndOriginDocumentId(
                PayableOrigin.OriginType.PURCHASE_RECEIPT, receipt.getId())) {
            return;
        }
        try {
            generateFromPurchaseReceipt(new PayableFromPurchaseRequest(
                    receipt.getId(), null, null, null, "auto-receipt-" + receipt.getId()));
        } catch (RuntimeException ex) {
            domainAuditService.record(
                    "FINANCE",
                    "Payable",
                    receipt.getId(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "Falha ao gerar AP automática: " + ex.getMessage());
        }
    }

    /** Geração na aprovação do pedido (modo ON_ORDER_APPROVED). */
    @Transactional
    public void tryAutoGenerateFromOrderApproved(PurchaseOrder order) {
        FinanceGenerationSettings settings = settingsRepository
                .findByOrganizationId(order.getOrganization().getId())
                .orElse(null);
        if (settings == null || !settings.shouldGeneratePayableOnOrderApproved()) {
            return;
        }
        if (originRepository.existsByOriginTypeAndOriginDocumentId(
                PayableOrigin.OriginType.PURCHASE_ORDER, order.getId())) {
            return;
        }
        BigDecimal total = nz(order.getTotalAmount());
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            LocalDate issue = LocalDate.now();
            createInternal(
                    new PayableCreateRequest(
                            order.getOrganization().getId(),
                            order.getStore() != null ? order.getStore().getId() : null,
                            order.getSupplier().getId(),
                            null,
                            null,
                            null,
                            order.getOrderNumber(),
                            issue,
                            issue,
                            total,
                            BigDecimal.ZERO,
                            BigDecimal.ZERO,
                            "Gerada na aprovação do pedido " + order.getOrderNumber(),
                            "auto-po-approved-" + order.getId(),
                            true,
                            buildInstallmentsFromCondition(null, total, issue)),
                    PayableOrigin.OriginType.PURCHASE_ORDER,
                    order.getId(),
                    order.getOrderNumber(),
                    false);
        } catch (RuntimeException ex) {
            domainAuditService.record(
                    "FINANCE",
                    "Payable",
                    order.getId(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "Falha ao gerar AP na aprovação: " + ex.getMessage());
        }
    }

    /** Crédito/ajuste na conclusão da devolução ao fornecedor. */
    @Transactional
    public PayableResponse generateCreditFromSupplierReturn(
            br.com.systemcommerce.purchase.entity.SupplierReturn supplierReturn) {
        if (originRepository.existsByOriginTypeAndOriginDocumentId(
                PayableOrigin.OriginType.SUPPLIER_RETURN, supplierReturn.getId())) {
            return originRepository
                    .findByOriginTypeAndOriginDocumentId(
                            PayableOrigin.OriginType.SUPPLIER_RETURN, supplierReturn.getId())
                    .map(o -> toResponse(getDetailed(o.getPayable().getId())))
                    .orElseThrow();
        }
        BigDecimal credit = supplierReturn.getItems().stream()
                .map(i -> {
                    BigDecimal qty = i.getQuantity() != null ? i.getQuantity() : BigDecimal.ZERO;
                    BigDecimal cost = i.getUnitCost() != null ? i.getUnitCost() : BigDecimal.ZERO;
                    return qty.multiply(cost).setScale(2, RoundingMode.HALF_UP);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (credit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Devolução sem valor para gerar crédito/ajuste");
        }
        LocalDate issue = LocalDate.now();
        return createInternal(
                new PayableCreateRequest(
                        supplierReturn.getOrganization().getId(),
                        supplierReturn.getStore() != null ? supplierReturn.getStore().getId() : null,
                        supplierReturn.getSupplier().getId(),
                        null,
                        null,
                        null,
                        "CRED-" + supplierReturn.getReturnNumber(),
                        issue,
                        issue,
                        credit,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        "Crédito/ajuste por devolução " + supplierReturn.getReturnNumber()
                                + " — compensar em pagamentos futuros ou estornar AP relacionada",
                        "auto-return-" + supplierReturn.getId(),
                        true,
                        buildInstallmentsFromCondition(null, credit, issue)),
                PayableOrigin.OriginType.SUPPLIER_RETURN,
                supplierReturn.getId(),
                supplierReturn.getReturnNumber(),
                false);
    }

    @Transactional
    public void tryAutoGenerateFromSupplierReturn(
            br.com.systemcommerce.purchase.entity.SupplierReturn supplierReturn) {
        try {
            generateCreditFromSupplierReturn(supplierReturn);
        } catch (RuntimeException ex) {
            domainAuditService.record(
                    "FINANCE",
                    "Payable",
                    supplierReturn.getId(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "Falha ao gerar crédito de devolução: " + ex.getMessage());
        }
    }

    /**
     * Análise de cancelamento: bloqueia se houver pagamento; se parcial, exige estorno.
     * Retorna mensagem descritiva do estado.
     */
    @Transactional(readOnly = true)
    public String analyzeCancelability(UUID payableId) {
        Payable payable = getDetailed(payableId);
        if (payable.isPaid()) {
            return "BLOQUEADO: conta totalmente paga — use estorno de liquidação";
        }
        if (payable.getPaidAmount() != null && payable.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            return "BLOQUEADO: pagamentos parciais de "
                    + payable.getPaidAmount()
                    + " — estorne liquidações antes do cancelamento";
        }
        if (payable.getStatus() == Payable.Status.CANCELLED) {
            return "JA_CANCELADA";
        }
        return "PERMITIDO";
    }

    @Transactional
    public PayableResponse updateDraft(UUID id, PayableCreateRequest request) {
        Payable payable = getDetailed(id);
        if (!payable.isEditable()) {
            throw new BusinessRuleException("Somente rascunhos podem ser editados");
        }
        applyHeader(payable, request, false);
        replaceInstallments(payable, request.installments(), request.issueDate());
        payableRepository.save(payable);
        domainAuditService.record(
                "FINANCE", "Payable", id, AuditLog.AuditAction.UPDATE, null, null, "Rascunho de conta a pagar atualizado");
        return toResponse(getDetailed(id));
    }

    @Transactional
    public PayableResponse cancel(UUID id, PayableCancelRequest request) {
        Payable payable = getDetailed(id);
        String analysis = analyzeCancelability(id);
        if (analysis.startsWith("BLOQUEADO")) {
            throw new BusinessRuleException(analysis);
        }
        if (payable.getStatus() == Payable.Status.CANCELLED) {
            return toResponse(payable);
        }
        Payable.Status from = payable.getStatus();
        payable.setStatus(Payable.Status.CANCELLED);
        payable.setCancelReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        payable.getInstallments().forEach(i -> i.setStatus(PayableInstallment.Status.CANCELLED));
        payableRepository.save(payable);
        appendStatus(payable, from, Payable.Status.CANCELLED, request.reason());
        domainAuditService.record(
                "FINANCE", "Payable", id, AuditLog.AuditAction.STATUS_CHANGE, null, null, "Conta a pagar cancelada");
        return toResponse(getDetailed(id));
    }

    @Transactional
    public PayableResponse renegotiate(UUID id, List<PayableInstallmentRequest> newInstallments) {
        Payable payable = getDetailed(id);
        if (payable.isPaid() || payable.getStatus() == Payable.Status.CANCELLED) {
            throw new BusinessRuleException("Conta não pode ser renegociada neste status");
        }
        Payable.Status from = payable.getStatus();
        replaceInstallments(payable, newInstallments, payable.getIssueDate());
        payable.setStatus(Payable.Status.RENEGOTIATED);
        recalculateHeaderFromInstallments(payable);
        payable.setStatus(Payable.Status.OPEN);
        payableRepository.save(payable);
        appendStatus(payable, from, Payable.Status.OPEN, "Renegociação de parcelas");
        domainAuditService.record(
                "FINANCE", "Payable", id, AuditLog.AuditAction.UPDATE, null, null, "Conta a pagar renegociada");
        return toResponse(getDetailed(id));
    }

    @Transactional
    public void refreshPayableAfterSettlement(UUID payableId) {
        Payable payable = payableRepository
                .findForUpdate(payableId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a pagar não encontrada"));
        BigDecimal paid = payable.getInstallments().stream()
                .map(PayableInstallment::getSettledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        payable.setPaidAmount(paid);
        payable.setBalanceAmount(payable.getTotalAmount().subtract(paid).max(BigDecimal.ZERO));
        Payable.Status from = payable.getStatus();
        if (payable.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
            payable.setStatus(Payable.Status.PAID);
        } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
            payable.setStatus(Payable.Status.PARTIALLY_PAID);
        } else {
            refreshPayableOverdue(payable);
            if (payable.getStatus() != Payable.Status.OVERDUE) {
                payable.setStatus(Payable.Status.OPEN);
            }
        }
        payableRepository.save(payable);
        if (from != payable.getStatus()) {
            appendStatus(payable, from, payable.getStatus(), "Atualização após liquidação");
        }
    }

    private PayableResponse createInternal(
            PayableCreateRequest request,
            PayableOrigin.OriginType originType,
            UUID originDocumentId,
            String originDocumentNumber,
            boolean manual) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = payableRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(getDetailed(existing.get().getId()));
            }
        }
        if (originDocumentId != null
                && originRepository.existsByOriginTypeAndOriginDocumentId(originType, originDocumentId)) {
            throw new ConflictException("Já existe conta a pagar para esta origem");
        }

        Organization org = organizationService.requireUsable(request.organizationId());
        Supplier supplier = supplierService.requireUsableForPurchase(request.supplierId());

        Payable payable = new Payable();
        payable.setOrganization(org);
        applyHeader(payable, request, true);
        payable.setSupplier(supplier);
        payable.setIdempotencyKey(MoneyAndQuantityUtils.blankToNull(request.idempotencyKey()));
        replaceInstallments(payable, request.installments(), request.issueDate());

        boolean open = request.openImmediately() == null || request.openImmediately();
        payable.setStatus(open ? Payable.Status.OPEN : Payable.Status.DRAFT);
        Payable saved = payableRepository.save(payable);

        if (originDocumentId != null || originType == PayableOrigin.OriginType.MANUAL_EXPENSE) {
            PayableOrigin origin = new PayableOrigin();
            origin.setPayable(saved);
            origin.setOriginType(originType);
            origin.setOriginDocumentId(originDocumentId != null ? originDocumentId : saved.getId());
            origin.setOriginDocumentNumber(originDocumentNumber);
            originRepository.save(origin);
            saved.getOrigins().add(origin);
        }

        appendStatus(saved, null, saved.getStatus(), manual ? "Criação manual" : "Geração por documento");
        domainAuditService.record(
                "FINANCE",
                "Payable",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                manual ? "Conta a pagar manual" : "Conta a pagar gerada");
        return toResponse(getDetailed(saved.getId()));
    }

    private void applyHeader(Payable payable, PayableCreateRequest request, boolean creating) {
        if (request.storeId() != null) {
            payable.setStore(storeService.requireUsable(request.storeId()));
        } else {
            payable.setStore(null);
        }
        if (request.paymentConditionId() != null) {
            payable.setPaymentCondition(paymentCatalogService.requireUsableCondition(request.paymentConditionId()));
        }
        if (request.financialCategoryId() != null) {
            payable.setFinancialCategory(financialCategoryService.requireUsable(
                    request.financialCategoryId(),
                    br.com.systemcommerce.finance.account.entity.FinancialCategory.UsageScope.PURCHASE));
        }
        if (request.costCenterId() != null) {
            payable.setCostCenter(costCenterService.requirePostable(request.costCenterId()));
        }
        payable.setDocumentNumber(MoneyAndQuantityUtils.blankToNull(request.documentNumber()));
        payable.setIssueDate(request.issueDate());
        payable.setCompetenceDate(request.competenceDate());
        BigDecimal original = request.originalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = nz(request.plannedDiscount());
        BigDecimal addition = nz(request.plannedAddition());
        BigDecimal total = original.subtract(discount).add(addition).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Total da conta a pagar deve ser positivo");
        }
        payable.setOriginalAmount(original);
        payable.setPlannedDiscount(discount);
        payable.setPlannedAddition(addition);
        payable.setTotalAmount(total);
        if (creating) {
            payable.setPaidAmount(BigDecimal.ZERO);
            payable.setBalanceAmount(total);
        } else {
            payable.setBalanceAmount(total.subtract(payable.getPaidAmount()).max(BigDecimal.ZERO));
        }
        payable.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
    }

    private void replaceInstallments(Payable payable, List<PayableInstallmentRequest> requests, LocalDate issueDate) {
        BigDecimal sum = requests.stream()
                .map(PayableInstallmentRequest::originalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (sum.compareTo(payable.getTotalAmount()) != 0) {
            throw new BusinessRuleException("Parcelas devem totalizar o valor da conta (" + payable.getTotalAmount() + ")");
        }
        payable.getInstallments().clear();
        for (PayableInstallmentRequest req : requests) {
            if (req.originalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Parcela não pode ter valor negativo ou zero");
            }
            PayableInstallment inst = new PayableInstallment();
            inst.setPayable(payable);
            inst.setInstallmentNumber(req.installmentNumber());
            inst.setIssueDate(issueDate);
            inst.setDueDate(req.dueDate());
            inst.setOriginalAmount(req.originalAmount().setScale(2, RoundingMode.HALF_UP));
            inst.setBalanceAmount(inst.getOriginalAmount());
            inst.setSettledAmount(BigDecimal.ZERO);
            inst.setStatus(PayableInstallment.Status.OPEN);
            inst.setBarcode(MoneyAndQuantityUtils.blankToNull(req.barcode()));
            inst.setDigitableLine(MoneyAndQuantityUtils.blankToNull(req.digitableLine()));
            inst.setReferenceCode(MoneyAndQuantityUtils.blankToNull(req.referenceCode()));
            inst.refreshOverdue(LocalDate.now());
            payable.getInstallments().add(inst);
        }
    }

    private void recalculateHeaderFromInstallments(Payable payable) {
        BigDecimal total = payable.getInstallments().stream()
                .map(PayableInstallment::getOriginalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        payable.setOriginalAmount(total);
        payable.setTotalAmount(total);
        BigDecimal paid = payable.getInstallments().stream()
                .map(PayableInstallment::getSettledAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        payable.setPaidAmount(paid);
        payable.setBalanceAmount(total.subtract(paid).max(BigDecimal.ZERO));
    }

    private List<PayableInstallmentRequest> buildInstallmentsFromCondition(
            UUID conditionId, BigDecimal total, LocalDate baseDate) {
        if (conditionId == null) {
            return List.of(new PayableInstallmentRequest(1, baseDate, total, null, null, null));
        }
        PaymentCondition condition = paymentCatalogService.requireUsableCondition(conditionId);
        var calc = paymentCatalogService.calculateDueDates(
                condition.getId(),
                new br.com.systemcommerce.finance.paymentcatalog.dto.CalculateDueDatesRequest(baseDate, total));
        return calc.installments().stream()
                .map(i -> new PayableInstallmentRequest(
                        i.sequenceNo(), i.dueDate(), i.amount(), null, null, null))
                .toList();
    }

    private void refreshOverdueFlags(UUID organizationId) {
        if (organizationId == null) {
            return;
        }
        LocalDate today = LocalDate.now();
        installmentRepository
                .findAgenda(organizationId, today.minusYears(1), today)
                .forEach(i -> {
                    PayableInstallment.Status before = i.getStatus();
                    i.refreshOverdue(today);
                    if (before != i.getStatus()) {
                        installmentRepository.save(i);
                    }
                });
    }

    private void refreshPayableOverdue(Payable payable) {
        LocalDate today = LocalDate.now();
        boolean overdue = false;
        for (PayableInstallment i : payable.getInstallments()) {
            i.refreshOverdue(today);
            if (i.getStatus() == PayableInstallment.Status.OVERDUE) {
                overdue = true;
            }
        }
        if (overdue
                && payable.getStatus() != Payable.Status.PAID
                && payable.getStatus() != Payable.Status.CANCELLED
                && payable.getStatus() != Payable.Status.DRAFT) {
            payable.setStatus(Payable.Status.OVERDUE);
        }
    }

    private void appendStatus(Payable payable, Payable.Status from, Payable.Status to, String reason) {
        PayableStatusHistory h = new PayableStatusHistory();
        h.setPayable(payable);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setReason(reason);
        CurrentUser.id().ifPresent(h::setChangedBy);
        statusHistoryRepository.save(h);
    }

    private Payable getDetailed(UUID id) {
        return payableRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a pagar não encontrada"));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private PayableResponse toResponse(Payable p) {
        return new PayableResponse(
                p.getId(),
                p.getOrganization().getId(),
                p.getStore() != null ? p.getStore().getId() : null,
                p.getSupplier().getId(),
                p.getSupplier().getLegalName() != null
                        ? p.getSupplier().getLegalName()
                        : p.getSupplier().getTradeName(),
                p.getPaymentCondition() != null ? p.getPaymentCondition().getId() : null,
                p.getFinancialCategory() != null ? p.getFinancialCategory().getId() : null,
                p.getCostCenter() != null ? p.getCostCenter().getId() : null,
                p.getDocumentNumber(),
                p.getIssueDate(),
                p.getCompetenceDate(),
                p.getOriginalAmount(),
                p.getPlannedDiscount(),
                p.getPlannedAddition(),
                p.getTotalAmount(),
                p.getPaidAmount(),
                p.getBalanceAmount(),
                p.getStatus(),
                p.getNotes(),
                p.getInstallments().stream().map(this::toInstallmentResponse).toList(),
                p.getOrigins().stream()
                        .map(o -> new PayableOriginResponse(
                                o.getId(), o.getOriginType(), o.getOriginDocumentId(), o.getOriginDocumentNumber()))
                        .toList(),
                p.getVersion(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    private PayableInstallmentResponse toInstallmentResponse(PayableInstallment i) {
        return new PayableInstallmentResponse(
                i.getId(),
                i.getInstallmentNumber(),
                i.getIssueDate(),
                i.getDueDate(),
                i.getOriginalAmount(),
                i.getInterestAmount(),
                i.getFineAmount(),
                i.getDiscountAmount(),
                i.getSettledAmount(),
                i.getBalanceAmount(),
                i.getStatus(),
                i.getBarcode(),
                i.getDigitableLine(),
                i.getReferenceCode(),
                i.getVersion());
    }
}
