package br.com.systemcommerce.finance.receivable.service;

import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.finance.account.entity.FinancialCategory;
import br.com.systemcommerce.finance.account.service.FinancialCategoryService;
import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.costcenter.service.CostCenterService;
import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import br.com.systemcommerce.finance.payable.repository.FinanceGenerationSettingsRepository;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import br.com.systemcommerce.finance.paymentcatalog.service.PaymentCatalogService;
import br.com.systemcommerce.finance.receivable.dto.ReceivableBalanceResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableCancelRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableCreateRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableFromSaleRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableInstallmentRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableInstallmentResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableOriginResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableResponse;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementAllocationRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableSettlementCreateRequest;
import br.com.systemcommerce.finance.receivable.dto.ReceivableWriteOffRequest;
import br.com.systemcommerce.finance.receivable.entity.Receivable;
import br.com.systemcommerce.finance.receivable.entity.ReceivableInstallment;
import br.com.systemcommerce.finance.receivable.entity.ReceivableOrigin;
import br.com.systemcommerce.finance.receivable.entity.ReceivableStatusHistory;
import br.com.systemcommerce.finance.receivable.repository.ReceivableInstallmentRepository;
import br.com.systemcommerce.finance.receivable.repository.ReceivableOriginRepository;
import br.com.systemcommerce.finance.receivable.repository.ReceivableRepository;
import br.com.systemcommerce.finance.receivable.repository.ReceivableStatusHistoryRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.sale.entity.Sale;
import br.com.systemcommerce.sale.repository.SaleRepository;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ReceivableService {

    private final ReceivableRepository receivableRepository;
    private final ReceivableInstallmentRepository installmentRepository;
    private final ReceivableOriginRepository originRepository;
    private final ReceivableStatusHistoryRepository statusHistoryRepository;
    private final FinanceGenerationSettingsRepository settingsRepository;
    private final SaleRepository saleRepository;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final CustomerService customerService;
    private final PaymentCatalogService paymentCatalogService;
    private final FinancialCategoryService financialCategoryService;
    private final CostCenterService costCenterService;
    private final BankFinanceService bankFinanceService;
    private final ObjectProvider<ReceivableSettlementService> settlementServiceProvider;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<ReceivableResponse> list(
            UUID organizationId, UUID customerId, Receivable.Status status, String search, Pageable pageable) {
        refreshOverdueFlags(organizationId);
        Specification<Receivable> spec = (root, q, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (organizationId != null) {
                preds.add(cb.equal(root.get("organization").get("id"), organizationId));
            }
            if (customerId != null) {
                preds.add(cb.equal(root.get("customer").get("id"), customerId));
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
        return receivableRepository.findAll(spec, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ReceivableResponse getById(UUID id) {
        Receivable receivable = getDetailed(id);
        refreshReceivableOverdue(receivable);
        return toResponse(receivable);
    }

    @Transactional(readOnly = true)
    public ReceivableBalanceResponse balance(UUID id) {
        Receivable receivable = getDetailed(id);
        refreshReceivableOverdue(receivable);
        return new ReceivableBalanceResponse(
                receivable.getId(),
                receivable.getTotalAmount(),
                receivable.getReceivedAmount(),
                receivable.getBalanceAmount(),
                receivable.getStatus());
    }

    @Transactional(readOnly = true)
    public List<ReceivableInstallmentResponse> installments(UUID receivableId) {
        getDetailed(receivableId);
        return installmentRepository.findByReceivableIdOrderByInstallmentNumberAsc(receivableId).stream()
                .map(this::toInstallmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivableInstallmentResponse> agenda(UUID organizationId, LocalDate from, LocalDate to) {
        LocalDate start = from != null ? from : LocalDate.now();
        LocalDate end = to != null ? to : start.plusDays(30);
        return installmentRepository.findAgenda(organizationId, start, end).stream()
                .peek(i -> i.refreshOverdue(LocalDate.now()))
                .filter(i -> i.getStatus() != ReceivableInstallment.Status.RECEIVED
                        && i.getStatus() != ReceivableInstallment.Status.CANCELLED
                        && i.getStatus() != ReceivableInstallment.Status.WRITTEN_OFF)
                .map(this::toInstallmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivableInstallmentResponse> byCustomer(UUID customerId) {
        return installmentRepository.findByCustomerId(customerId).stream()
                .peek(i -> i.refreshOverdue(LocalDate.now()))
                .map(this::toInstallmentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReceivableStatusHistory> history(UUID id) {
        getDetailed(id);
        return statusHistoryRepository.findByReceivableIdOrderByChangedAtAsc(id);
    }

    @Transactional
    public ReceivableResponse createManual(ReceivableCreateRequest request) {
        return createInternal(request, ReceivableOrigin.OriginType.MANUAL_CHARGE, null, null, true);
    }

    @Transactional
    public ReceivableResponse generateFromSale(ReceivableFromSaleRequest request) {
        Sale sale = saleRepository
                .findDetailedById(request.saleId())
                .orElseThrow(() -> new ResourceNotFoundException("Venda não encontrada"));
        if (sale.getCustomer() == null) {
            throw new BusinessRuleException("Venda sem cliente não pode gerar conta a receber");
        }
        if (!sale.isConfirmedLike()) {
            throw new BusinessRuleException("Somente venda confirmada/paga pode gerar conta a receber");
        }

        ReceivableOrigin.OriginType originType =
                sale.isPos() ? ReceivableOrigin.OriginType.POS : ReceivableOrigin.OriginType.SALE;
        if (originRepository.existsByOriginTypeAndOriginDocumentId(originType, sale.getId())) {
            return originRepository
                    .findByOriginTypeAndOriginDocumentId(originType, sale.getId())
                    .map(o -> toResponse(getDetailed(o.getReceivable().getId())))
                    .orElseThrow();
        }

        BigDecimal total = sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO;
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Venda sem valor total para gerar conta a receber");
        }

        LocalDate issueDate = sale.getSaleDate() != null
                ? sale.getSaleDate().atZone(ZoneOffset.UTC).toLocalDate()
                : LocalDate.now();
        List<ReceivableInstallmentRequest> installments =
                buildInstallmentsFromCondition(request.paymentConditionId(), total, issueDate);

        ReceivableCreateRequest create = new ReceivableCreateRequest(
                sale.getOrganization().getId(),
                sale.getStore() != null ? sale.getStore().getId() : null,
                sale.getCustomer().getId(),
                request.paymentConditionId(),
                request.financialCategoryId(),
                request.costCenterId(),
                sale.getSaleNumber(),
                issueDate,
                issueDate,
                total,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "Gerada automaticamente da venda " + sale.getSaleNumber(),
                request.idempotencyKey() != null ? request.idempotencyKey() : "auto-sale-" + sale.getId(),
                true,
                installments);
        ReceivableResponse response = createInternal(create, originType, sale.getId(), sale.getSaleNumber(), false);
        if (sale.getSeller() != null) {
            Receivable receivable = getDetailed(response.id());
            receivable.setSalespersonId(sale.getSeller().getId());
            receivableRepository.save(receivable);
        }
        return toResponse(getDetailed(response.id()));
    }

    /** Chamado no faturamento do pedido quando a geração automática está habilitada. */
    @Transactional
    public void tryAutoGenerateFromInvoice(SalesOrder order) {
        FinanceGenerationSettings settings = settingsRepository
                .findByOrganizationId(order.getOrganization().getId())
                .orElse(null);
        if (settings == null || !Boolean.TRUE.equals(settings.getGenerateReceivableOnInvoice())) {
            return;
        }
        if (originRepository.existsByOriginTypeAndOriginDocumentId(
                ReceivableOrigin.OriginType.SALES_ORDER, order.getId())) {
            return;
        }
        try {
            if (order.getGeneratedSale() != null) {
                ReceivableResponse ar = generateFromSale(new ReceivableFromSaleRequest(
                        order.getGeneratedSale().getId(), null, null, null, "auto-invoice-" + order.getId()));
                // Origem tipada do pedido (além da SALE) — anti-duplicidade do faturamento
                if (!originRepository.existsByOriginTypeAndOriginDocumentId(
                        ReceivableOrigin.OriginType.SALES_ORDER, order.getId())) {
                    Receivable receivable = getDetailed(ar.id());
                    ReceivableOrigin orderOrigin = new ReceivableOrigin();
                    orderOrigin.setReceivable(receivable);
                    orderOrigin.setOriginType(ReceivableOrigin.OriginType.SALES_ORDER);
                    orderOrigin.setOriginDocumentId(order.getId());
                    orderOrigin.setOriginDocumentNumber(order.getOrderNumber());
                    originRepository.save(orderOrigin);
                }
            } else {
                domainAuditService.record(
                        "FINANCE",
                        "Receivable",
                        order.getId(),
                        AuditLog.AuditAction.OTHER,
                        null,
                        null,
                        "Geração AR automática do pedido " + order.getOrderNumber()
                                + " adiada — venda gerada ainda não disponível");
            }
        } catch (RuntimeException ex) {
            domainAuditService.record(
                    "FINANCE",
                    "Receivable",
                    order.getId(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "Falha ao gerar AR automática: " + ex.getMessage());
        }
    }

    /**
     * Cancelamento de venda: cancela ARs abertas; se já recebidas, exige estorno/crédito (não cancela).
     */
    @Transactional
    public void handleSaleCancellation(UUID saleId, String reason) {
        for (ReceivableOrigin.OriginType type :
                List.of(ReceivableOrigin.OriginType.SALE, ReceivableOrigin.OriginType.POS)) {
            originRepository.findByOriginTypeAndOriginDocumentId(type, saleId).ifPresent(origin -> {
                Receivable receivable = getDetailed(origin.getReceivable().getId());
                if (receivable.isReceived()
                        || (receivable.getReceivedAmount() != null
                                && receivable.getReceivedAmount().compareTo(BigDecimal.ZERO) > 0)) {
                    domainAuditService.record(
                            "FINANCE",
                            "Receivable",
                            receivable.getId(),
                            AuditLog.AuditAction.OTHER,
                            null,
                            null,
                            "Cancelamento de venda com AR já recebida — exige estorno/crédito. Motivo: " + reason);
                    return;
                }
                if (receivable.getStatus() != Receivable.Status.CANCELLED) {
                    cancel(receivable.getId(), new ReceivableCancelRequest(
                            reason != null ? reason : "Cancelamento da venda de origem"));
                }
            });
        }
    }

    /** Chamado na finalização POS quando geração + liquidação automática está habilitada. */
    @Transactional
    public void tryAutoGenerateAndSettlePos(Sale sale, UUID holderId, UUID cashSessionId) {
        FinanceGenerationSettings settings = settingsRepository
                .findByOrganizationId(sale.getOrganization().getId())
                .orElse(null);
        if (settings == null || !Boolean.TRUE.equals(settings.getGenerateAndSettlePosCash())) {
            return;
        }
        try {
            ReceivableResponse ar = generateFromSale(new ReceivableFromSaleRequest(
                    sale.getId(), null, null, null, "auto-pos-" + sale.getId()));

            if (ar.balanceAmount() != null && ar.balanceAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return;
            }

            UUID resolvedHolder = holderId;
            if (resolvedHolder == null) {
                UUID storeId = sale.getStore() != null ? sale.getStore().getId() : null;
                resolvedHolder = bankFinanceService
                        .resolvePosCashHolderId(sale.getOrganization().getId(), storeId, cashSessionId)
                        .orElse(null);
            }

            if (resolvedHolder != null && ar.installments() != null && !ar.installments().isEmpty()) {
                List<ReceivableSettlementAllocationRequest> allocations = ar.installments().stream()
                        .filter(i -> i.balanceAmount() != null && i.balanceAmount().compareTo(BigDecimal.ZERO) > 0)
                        .map(i -> new ReceivableSettlementAllocationRequest(
                                i.id(), i.balanceAmount(), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))
                        .toList();
                if (!allocations.isEmpty()) {
                    settlementServiceProvider
                            .getObject()
                            .settle(new ReceivableSettlementCreateRequest(
                                    sale.getOrganization().getId(),
                                    sale.getStore() != null ? sale.getStore().getId() : null,
                                    resolvedHolder,
                                    cashSessionId,
                                    null,
                                    LocalDate.now(ZoneOffset.UTC),
                                    null,
                                    BigDecimal.ZERO,
                                    null,
                                    BigDecimal.ZERO,
                                    "POS-" + sale.getSaleNumber(),
                                    sale.getId().toString(),
                                    "Liquidação automática PDV à vista",
                                    "auto-pos-settle-" + sale.getId(),
                                    true,
                                    allocations));
                }
            }

            domainAuditService.record(
                    "FINANCE",
                    "Receivable",
                    sale.getId(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "AR gerada no POS"
                            + (resolvedHolder != null ? " e liquidada (holder=" + resolvedHolder + ")" : " (sem holder)")
                            + ", cashSession="
                            + cashSessionId);
        } catch (RuntimeException ex) {
            domainAuditService.record(
                    "FINANCE",
                    "Receivable",
                    sale.getId(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "Falha ao gerar/liquidar AR no POS: " + ex.getMessage());
        }
    }

    @Transactional
    public ReceivableResponse updateDraft(UUID id, ReceivableCreateRequest request) {
        Receivable receivable = getDetailed(id);
        if (!receivable.isEditable()) {
            throw new BusinessRuleException("Somente rascunhos podem ser editados");
        }
        applyHeader(receivable, request, false);
        replaceInstallments(receivable, request.installments(), request.issueDate());
        receivableRepository.save(receivable);
        domainAuditService.record(
                "FINANCE",
                "Receivable",
                id,
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Rascunho de conta a receber atualizado");
        return toResponse(getDetailed(id));
    }

    @Transactional
    public ReceivableResponse cancel(UUID id, ReceivableCancelRequest request) {
        Receivable receivable = getDetailed(id);
        if (receivable.isReceived()) {
            throw new BusinessRuleException("Conta recebida não pode ser cancelada — use estorno de liquidação");
        }
        if (receivable.getStatus() == Receivable.Status.CANCELLED) {
            return toResponse(receivable);
        }
        if (receivable.getStatus() == Receivable.Status.WRITTEN_OFF) {
            throw new BusinessRuleException("Conta baixada não pode ser cancelada");
        }
        Receivable.Status from = receivable.getStatus();
        receivable.setStatus(Receivable.Status.CANCELLED);
        receivable.setCancelReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        receivable.getInstallments().forEach(i -> i.setStatus(ReceivableInstallment.Status.CANCELLED));
        receivableRepository.save(receivable);
        appendStatus(receivable, from, Receivable.Status.CANCELLED, request.reason());
        domainAuditService.record(
                "FINANCE", "Receivable", id, AuditLog.AuditAction.STATUS_CHANGE, null, null, "Conta a receber cancelada");
        return toResponse(getDetailed(id));
    }

    @Transactional
    public ReceivableResponse renegotiate(UUID id, List<ReceivableInstallmentRequest> newInstallments) {
        Receivable receivable = getDetailed(id);
        if (receivable.isReceived()
                || receivable.getStatus() == Receivable.Status.CANCELLED
                || receivable.getStatus() == Receivable.Status.WRITTEN_OFF) {
            throw new BusinessRuleException("Conta não pode ser renegociada neste status");
        }
        Receivable.Status from = receivable.getStatus();
        replaceInstallments(receivable, newInstallments, receivable.getIssueDate());
        receivable.setStatus(Receivable.Status.RENEGOTIATED);
        recalculateHeaderFromInstallments(receivable);
        receivable.setStatus(Receivable.Status.OPEN);
        receivableRepository.save(receivable);
        appendStatus(receivable, from, Receivable.Status.OPEN, "Renegociação de parcelas");
        domainAuditService.record(
                "FINANCE", "Receivable", id, AuditLog.AuditAction.UPDATE, null, null, "Conta a receber renegociada");
        return toResponse(getDetailed(id));
    }

    @Transactional
    public ReceivableResponse writeOff(UUID id, ReceivableWriteOffRequest request) {
        Receivable receivable = getDetailed(id);
        if (receivable.isReceived()) {
            throw new BusinessRuleException("Conta já recebida não pode ser baixada");
        }
        if (receivable.getStatus() == Receivable.Status.CANCELLED) {
            throw new BusinessRuleException("Conta cancelada não pode ser baixada");
        }
        if (receivable.getStatus() == Receivable.Status.WRITTEN_OFF) {
            return toResponse(receivable);
        }
        Receivable.Status from = receivable.getStatus();
        receivable.setStatus(Receivable.Status.WRITTEN_OFF);
        receivable.setWriteOffReason(MoneyAndQuantityUtils.requireText(request.reason(), "Motivo"));
        receivable.setBalanceAmount(BigDecimal.ZERO);
        receivable.getInstallments().forEach(i -> {
            if (i.getStatus() != ReceivableInstallment.Status.RECEIVED
                    && i.getStatus() != ReceivableInstallment.Status.CANCELLED) {
                i.setStatus(ReceivableInstallment.Status.WRITTEN_OFF);
                i.setBalanceAmount(BigDecimal.ZERO);
            }
        });
        receivableRepository.save(receivable);
        appendStatus(receivable, from, Receivable.Status.WRITTEN_OFF, request.reason());
        domainAuditService.record(
                "FINANCE",
                "Receivable",
                id,
                AuditLog.AuditAction.STATUS_CHANGE,
                null,
                null,
                "Baixa sem recebimento da conta a receber");
        return toResponse(getDetailed(id));
    }

    @Transactional
    public void refreshReceivableAfterSettlement(UUID receivableId) {
        Receivable receivable = receivableRepository
                .findForUpdate(receivableId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a receber não encontrada"));
        BigDecimal received = receivable.getInstallments().stream()
                .map(ReceivableInstallment::getReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        receivable.setReceivedAmount(received);
        receivable.setBalanceAmount(receivable.getTotalAmount().subtract(received).max(BigDecimal.ZERO));
        Receivable.Status from = receivable.getStatus();
        if (receivable.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
            receivable.setStatus(Receivable.Status.RECEIVED);
        } else if (received.compareTo(BigDecimal.ZERO) > 0) {
            receivable.setStatus(Receivable.Status.PARTIALLY_RECEIVED);
        } else {
            refreshReceivableOverdue(receivable);
            if (receivable.getStatus() != Receivable.Status.OVERDUE) {
                receivable.setStatus(Receivable.Status.OPEN);
            }
        }
        receivableRepository.save(receivable);
        if (from != receivable.getStatus()) {
            appendStatus(receivable, from, receivable.getStatus(), "Atualização após liquidação");
        }
    }

    private ReceivableResponse createInternal(
            ReceivableCreateRequest request,
            ReceivableOrigin.OriginType originType,
            UUID originDocumentId,
            String originDocumentNumber,
            boolean manual) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = receivableRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(getDetailed(existing.get().getId()));
            }
        }
        if (originDocumentId != null
                && originRepository.existsByOriginTypeAndOriginDocumentId(originType, originDocumentId)) {
            throw new ConflictException("Já existe conta a receber para esta origem");
        }

        Organization org = organizationService.requireUsable(request.organizationId());
        Customer customer = customerService.requireUsableForSale(request.customerId());

        Receivable receivable = new Receivable();
        receivable.setOrganization(org);
        applyHeader(receivable, request, true);
        receivable.setCustomer(customer);
        receivable.setIdempotencyKey(MoneyAndQuantityUtils.blankToNull(request.idempotencyKey()));
        replaceInstallments(receivable, request.installments(), request.issueDate());

        boolean open = request.openImmediately() == null || request.openImmediately();
        receivable.setStatus(open ? Receivable.Status.OPEN : Receivable.Status.DRAFT);
        Receivable saved = receivableRepository.save(receivable);

        if (originDocumentId != null || originType == ReceivableOrigin.OriginType.MANUAL_CHARGE) {
            ReceivableOrigin origin = new ReceivableOrigin();
            origin.setReceivable(saved);
            origin.setOriginType(originType);
            origin.setOriginDocumentId(originDocumentId != null ? originDocumentId : saved.getId());
            origin.setOriginDocumentNumber(originDocumentNumber);
            originRepository.save(origin);
            saved.getOrigins().add(origin);
        }

        appendStatus(saved, null, saved.getStatus(), manual ? "Criação manual" : "Geração por documento");
        domainAuditService.record(
                "FINANCE",
                "Receivable",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                null,
                manual ? "Conta a receber manual" : "Conta a receber gerada");
        return toResponse(getDetailed(saved.getId()));
    }

    private void applyHeader(Receivable receivable, ReceivableCreateRequest request, boolean creating) {
        if (request.storeId() != null) {
            receivable.setStore(storeService.requireUsable(request.storeId()));
        } else {
            receivable.setStore(null);
        }
        if (request.paymentConditionId() != null) {
            receivable.setPaymentCondition(paymentCatalogService.requireUsableCondition(request.paymentConditionId()));
        }
        if (request.financialCategoryId() != null) {
            receivable.setFinancialCategory(financialCategoryService.requireUsable(
                    request.financialCategoryId(), FinancialCategory.UsageScope.SALE));
        }
        if (request.costCenterId() != null) {
            receivable.setCostCenter(costCenterService.requirePostable(request.costCenterId()));
        }
        receivable.setDocumentNumber(MoneyAndQuantityUtils.blankToNull(request.documentNumber()));
        receivable.setIssueDate(request.issueDate());
        receivable.setCompetenceDate(request.competenceDate());
        BigDecimal original = request.originalAmount().setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = nz(request.plannedDiscount());
        BigDecimal addition = nz(request.plannedAddition());
        BigDecimal total = original.subtract(discount).add(addition).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Total da conta a receber deve ser positivo");
        }
        receivable.setOriginalAmount(original);
        receivable.setPlannedDiscount(discount);
        receivable.setPlannedAddition(addition);
        receivable.setTotalAmount(total);
        if (creating) {
            receivable.setReceivedAmount(BigDecimal.ZERO);
            receivable.setBalanceAmount(total);
        } else {
            receivable.setBalanceAmount(total.subtract(receivable.getReceivedAmount()).max(BigDecimal.ZERO));
        }
        receivable.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
    }

    private void replaceInstallments(
            Receivable receivable, List<ReceivableInstallmentRequest> requests, LocalDate issueDate) {
        BigDecimal sum = requests.stream()
                .map(ReceivableInstallmentRequest::originalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        if (sum.compareTo(receivable.getTotalAmount()) != 0) {
            throw new BusinessRuleException(
                    "Parcelas devem totalizar o valor da conta (" + receivable.getTotalAmount() + ")");
        }
        receivable.getInstallments().clear();
        for (ReceivableInstallmentRequest req : requests) {
            if (req.originalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessRuleException("Parcela não pode ter valor negativo ou zero");
            }
            ReceivableInstallment inst = new ReceivableInstallment();
            inst.setReceivable(receivable);
            inst.setInstallmentNumber(req.installmentNumber());
            inst.setIssueDate(issueDate);
            inst.setDueDate(req.dueDate());
            inst.setOriginalAmount(req.originalAmount().setScale(2, RoundingMode.HALF_UP));
            inst.setBalanceAmount(inst.getOriginalAmount());
            inst.setReceivedAmount(BigDecimal.ZERO);
            inst.setStatus(ReceivableInstallment.Status.OPEN);
            inst.setNossoNumero(MoneyAndQuantityUtils.blankToNull(req.nossoNumero()));
            inst.setBillingCode(MoneyAndQuantityUtils.blankToNull(req.billingCode()));
            inst.setPixTxid(MoneyAndQuantityUtils.blankToNull(req.pixTxid()));
            inst.setBoletoNumber(MoneyAndQuantityUtils.blankToNull(req.boletoNumber()));
            inst.setNotes(MoneyAndQuantityUtils.blankToNull(req.notes()));
            inst.refreshOverdue(LocalDate.now());
            receivable.getInstallments().add(inst);
        }
    }

    private void recalculateHeaderFromInstallments(Receivable receivable) {
        BigDecimal total = receivable.getInstallments().stream()
                .map(ReceivableInstallment::getOriginalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        receivable.setOriginalAmount(total);
        receivable.setTotalAmount(total);
        BigDecimal received = receivable.getInstallments().stream()
                .map(ReceivableInstallment::getReceivedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        receivable.setReceivedAmount(received);
        receivable.setBalanceAmount(total.subtract(received).max(BigDecimal.ZERO));
    }

    private List<ReceivableInstallmentRequest> buildInstallmentsFromCondition(
            UUID conditionId, BigDecimal total, LocalDate baseDate) {
        if (conditionId == null) {
            return List.of(new ReceivableInstallmentRequest(1, baseDate, total, null, null, null, null, null));
        }
        PaymentCondition condition = paymentCatalogService.requireUsableCondition(conditionId);
        var calc = paymentCatalogService.calculateDueDates(
                condition.getId(),
                new br.com.systemcommerce.finance.paymentcatalog.dto.CalculateDueDatesRequest(baseDate, total));
        return calc.installments().stream()
                .map(i -> new ReceivableInstallmentRequest(
                        i.sequenceNo(), i.dueDate(), i.amount(), null, null, null, null, null))
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
                    ReceivableInstallment.Status before = i.getStatus();
                    i.refreshOverdue(today);
                    if (before != i.getStatus()) {
                        installmentRepository.save(i);
                    }
                });
    }

    private void refreshReceivableOverdue(Receivable receivable) {
        LocalDate today = LocalDate.now();
        boolean overdue = false;
        for (ReceivableInstallment i : receivable.getInstallments()) {
            i.refreshOverdue(today);
            if (i.getStatus() == ReceivableInstallment.Status.OVERDUE) {
                overdue = true;
            }
        }
        if (overdue
                && receivable.getStatus() != Receivable.Status.RECEIVED
                && receivable.getStatus() != Receivable.Status.CANCELLED
                && receivable.getStatus() != Receivable.Status.WRITTEN_OFF
                && receivable.getStatus() != Receivable.Status.DRAFT) {
            receivable.setStatus(Receivable.Status.OVERDUE);
        }
    }

    private void appendStatus(Receivable receivable, Receivable.Status from, Receivable.Status to, String reason) {
        ReceivableStatusHistory h = new ReceivableStatusHistory();
        h.setReceivable(receivable);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setReason(reason);
        CurrentUser.id().ifPresent(h::setChangedBy);
        statusHistoryRepository.save(h);
    }

    private Receivable getDetailed(UUID id) {
        return receivableRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta a receber não encontrada"));
    }

    private BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v.setScale(2, RoundingMode.HALF_UP);
    }

    private ReceivableResponse toResponse(Receivable r) {
        String customerName = r.getCustomer().getName();
        return new ReceivableResponse(
                r.getId(),
                r.getOrganization().getId(),
                r.getStore() != null ? r.getStore().getId() : null,
                r.getCustomer().getId(),
                customerName,
                r.getPaymentCondition() != null ? r.getPaymentCondition().getId() : null,
                r.getFinancialCategory() != null ? r.getFinancialCategory().getId() : null,
                r.getCostCenter() != null ? r.getCostCenter().getId() : null,
                r.getDocumentNumber(),
                r.getIssueDate(),
                r.getCompetenceDate(),
                r.getOriginalAmount(),
                r.getPlannedDiscount(),
                r.getPlannedAddition(),
                r.getTotalAmount(),
                r.getReceivedAmount(),
                r.getBalanceAmount(),
                r.getStatus(),
                r.getNotes(),
                r.getInstallments().stream().map(this::toInstallmentResponse).toList(),
                r.getOrigins().stream()
                        .map(o -> new ReceivableOriginResponse(
                                o.getId(), o.getOriginType(), o.getOriginDocumentId(), o.getOriginDocumentNumber()))
                        .toList(),
                r.getVersion(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }

    private ReceivableInstallmentResponse toInstallmentResponse(ReceivableInstallment i) {
        return new ReceivableInstallmentResponse(
                i.getId(),
                i.getInstallmentNumber(),
                i.getIssueDate(),
                i.getDueDate(),
                i.getOriginalAmount(),
                i.getInterestAmount(),
                i.getFineAmount(),
                i.getDiscountAmount(),
                i.getReceivedAmount(),
                i.getBalanceAmount(),
                i.getStatus(),
                i.getNossoNumero(),
                i.getBillingCode(),
                i.getPixTxid(),
                i.getBoletoNumber(),
                i.getNotes(),
                i.getVersion());
    }
}
