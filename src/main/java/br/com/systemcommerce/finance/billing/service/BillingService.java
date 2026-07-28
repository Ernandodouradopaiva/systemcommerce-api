package br.com.systemcommerce.finance.billing.service;

import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.finance.billing.adapter.BillingProviderAdapter;
import br.com.systemcommerce.finance.billing.adapter.BillingProviderRegistry;
import br.com.systemcommerce.finance.billing.dto.BillingDtos.*;
import br.com.systemcommerce.finance.billing.entity.*;
import br.com.systemcommerce.finance.billing.repository.BillingDocumentRepository;
import br.com.systemcommerce.finance.billing.repository.BillingWebhookEventRepository;
import br.com.systemcommerce.finance.receivable.entity.ReceivableInstallment;
import br.com.systemcommerce.finance.receivable.repository.ReceivableInstallmentRepository;
import br.com.systemcommerce.finance.receivable.repository.ReceivableRepository;
import br.com.systemcommerce.finance.receivable.service.ReceivableService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.pos.store.service.StoreService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillingDocumentRepository documentRepository;
    private final BillingWebhookEventRepository webhookEventRepository;
    private final BillingProviderRegistry providerRegistry;
    private final OrganizationService organizationService;
    private final StoreService storeService;
    private final CustomerService customerService;
    private final ReceivableRepository receivableRepository;
    private final ReceivableInstallmentRepository installmentRepository;
    private final ReceivableService receivableService;
    private final DomainAuditService domainAuditService;

    @Transactional
    public BillingResponse create(CreateBillingRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = documentRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toResponse(existing.get());
            }
        }

        BillingDocument doc = new BillingDocument();
        doc.setOrganization(organizationService.requireUsable(request.organizationId()));
        if (request.storeId() != null) {
            doc.setStore(storeService.requireUsable(request.storeId()));
        }
        doc.setCustomer(customerService.requireUsableForSale(request.customerId()));
        if (request.receivableId() != null) {
            doc.setReceivable(receivableRepository
                    .findById(request.receivableId())
                    .orElseThrow(() -> new ResourceNotFoundException("Conta a receber não encontrada")));
        }
        if (request.receivableInstallmentId() != null) {
            ReceivableInstallment installment = installmentRepository
                    .findForUpdate(request.receivableInstallmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parcela não encontrada"));
            if (installment.getStatus() == ReceivableInstallment.Status.CANCELLED
                    || installment.getStatus() == ReceivableInstallment.Status.WRITTEN_OFF) {
                throw new BusinessRuleException("Parcela não permite cobrança no status " + installment.getStatus());
            }
            doc.setReceivableInstallment(installment);
            if (doc.getReceivable() == null) {
                doc.setReceivable(installment.getReceivable());
            }
        }
        doc.setBillingType(request.billingType());
        doc.setAmount(request.amount().setScale(2, RoundingMode.HALF_UP));
        doc.setDueDate(request.dueDate());
        doc.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        doc.setIdempotencyKey(request.idempotencyKey());
        doc.setStatus(BillingDocument.Status.DRAFT);
        appendHistory(doc, null, BillingDocument.Status.DRAFT, "Cobrança criada", null);

        BillingDocument saved = documentRepository.save(doc);
        domainAuditService.record(
                "FINANCE", "BillingDocument", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Cobrança criada");

        if (Boolean.TRUE.equals(request.registerImmediately())) {
            return register(saved.getId(), new RegisterRequest(request.providerCode()), request.pixExpiresInSeconds());
        }
        return toResponse(saved);
    }

    @Transactional
    public BillingResponse register(UUID id, RegisterRequest request, Integer pixExpiresInSeconds) {
        BillingDocument doc = requireDetailed(id);
        if (doc.getStatus() != BillingDocument.Status.DRAFT && doc.getStatus() != BillingDocument.Status.CANCELLED) {
            if (doc.getStatus() == BillingDocument.Status.REGISTERED
                    || doc.getStatus() == BillingDocument.Status.PENDING) {
                return toResponse(doc);
            }
            throw new BusinessRuleException("Cobrança não pode ser registrada no status " + doc.getStatus());
        }
        BillingProviderAdapter adapter = providerRegistry.resolve(request != null ? request.providerCode() : null);
        doc.setProviderCode(adapter.providerCode());

        if (doc.getBillingType() == BillingDocument.BillingType.BOLETO) {
            var reg = adapter.registerBankSlip(doc);
            BankSlip slip = doc.getBankSlip();
            if (slip == null) {
                slip = new BankSlip();
                slip.setBillingDocument(doc);
                doc.setBankSlip(slip);
            }
            slip.setDigitableLine(reg.digitableLine());
            slip.setBarcode(reg.barcode());
            slip.setNossoNumero(reg.nossoNumero());
            slip.setBankCode(reg.bankCode());
            slip.setWallet(reg.wallet());
            slip.setPdfUrl(reg.pdfUrl());
            slip.setRegisteredAt(Instant.now());
            doc.setExternalId(reg.externalId());
            if (doc.getReceivableInstallment() != null) {
                doc.getReceivableInstallment().setNossoNumero(reg.nossoNumero());
                doc.getReceivableInstallment().setBoletoNumber(reg.barcode());
                doc.getReceivableInstallment().setBillingCode(reg.externalId());
            }
        } else {
            int seconds = pixExpiresInSeconds != null && pixExpiresInSeconds > 0 ? pixExpiresInSeconds : 3600;
            Instant expiresAt = Instant.now().plus(seconds, ChronoUnit.SECONDS);
            var reg = adapter.registerPix(doc, expiresAt);
            PixCharge pix = doc.getPixCharge();
            if (pix == null) {
                pix = new PixCharge();
                pix.setBillingDocument(doc);
                doc.setPixCharge(pix);
            }
            pix.setTxid(reg.txid());
            pix.setQrCode(reg.qrCode());
            pix.setQrCodeImageUrl(reg.qrCodeImageUrl());
            pix.setCopyPaste(reg.copyPaste());
            pix.setExpiresAt(reg.expiresAt());
            doc.setExternalId(reg.externalId());
            if (doc.getReceivableInstallment() != null) {
                doc.getReceivableInstallment().setPixTxid(reg.txid());
                doc.getReceivableInstallment().setBillingCode(reg.externalId());
            }
        }

        BillingDocument.Status from = doc.getStatus();
        doc.setStatus(BillingDocument.Status.PENDING);
        appendHistory(doc, from, BillingDocument.Status.PENDING, "Registrada no provedor " + adapter.providerCode(), null);
        BillingDocument saved = documentRepository.save(doc);
        domainAuditService.record(
                "FINANCE", "BillingDocument", saved.getId(), AuditLog.AuditAction.UPDATE, null, null, "Cobrança registrada");
        return toResponse(saved);
    }

    @Transactional
    public BillingResponse cancel(UUID id, CancelRequest request) {
        BillingDocument doc = requireDetailed(id);
        if (doc.getStatus() == BillingDocument.Status.PAID || doc.getStatus() == BillingDocument.Status.REFUNDED) {
            throw new BusinessRuleException("Cobrança paga/reembolsada não pode ser cancelada");
        }
        if (doc.getStatus() == BillingDocument.Status.CANCELLED) {
            return toResponse(doc);
        }
        if (StringUtils.hasText(doc.getProviderCode()) && doc.getExternalId() != null) {
            providerRegistry.resolve(doc.getProviderCode()).cancel(doc);
        }
        BillingDocument.Status from = doc.getStatus();
        doc.setStatus(BillingDocument.Status.CANCELLED);
        appendHistory(
                doc,
                from,
                BillingDocument.Status.CANCELLED,
                request != null && StringUtils.hasText(request.reason()) ? request.reason() : "Cancelada",
                null);
        BillingDocument saved = documentRepository.save(doc);
        domainAuditService.record(
                "FINANCE", "BillingDocument", saved.getId(), AuditLog.AuditAction.UPDATE, null, null, "Cobrança cancelada");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public BillingResponse get(UUID id) {
        return toResponse(requireDetailed(id));
    }

    @Transactional(readOnly = true)
    public Page<BillingResponse> list(UUID organizationId, Pageable pageable) {
        return documentRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable).map(this::toResponse);
    }

    /**
     * Webhook idempotente por (organization, provider, eventId).
     * Atualiza parcela vinculada sem apagar histórico.
     */
    @Transactional
    public BillingResponse processWebhook(WebhookRequest request) {
        var existingEvent = webhookEventRepository.findByOrganizationIdAndProviderCodeAndEventId(
                request.organizationId(), request.providerCode().toUpperCase(), request.eventId());
        if (existingEvent.isPresent()) {
            BillingWebhookEvent ev = existingEvent.get();
            if (ev.getBillingDocument() != null) {
                return toResponse(requireDetailed(ev.getBillingDocument().getId()));
            }
            return null;
        }

        BillingWebhookEvent event = new BillingWebhookEvent();
        event.setOrganization(organizationService.requireUsable(request.organizationId()));
        event.setProviderCode(request.providerCode().toUpperCase());
        event.setEventId(request.eventId());
        event.setEventType(MoneyAndQuantityUtils.blankToNull(request.eventType()));
        event.setPayload(request.payload());

        BillingDocument doc = null;
        if (StringUtils.hasText(request.externalId())) {
            doc = documentRepository
                    .findByOrganizationIdAndProviderCodeAndExternalId(
                            request.organizationId(),
                            request.providerCode().toUpperCase(),
                            request.externalId())
                    .orElse(null);
        }
        event.setBillingDocument(doc);

        String type = request.eventType() != null ? request.eventType().toUpperCase() : "";
        try {
            if (doc != null && (type.contains("PAID") || type.contains("PAYMENT") || type.contains("LIQUID"))) {
                markPaid(doc, request.paidAmount(), request.paidAt(), request.endToEndId(), request.eventId());
            } else if (doc != null && (type.contains("EXPIRE") || type.contains("CANCEL"))) {
                if (doc.getStatus() != BillingDocument.Status.PAID) {
                    BillingDocument.Status from = doc.getStatus();
                    BillingDocument.Status to = type.contains("EXPIRE")
                            ? BillingDocument.Status.EXPIRED
                            : BillingDocument.Status.CANCELLED;
                    doc.setStatus(to);
                    appendHistory(doc, from, to, "Webhook: " + type, request.eventId());
                    documentRepository.save(doc);
                }
            }
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            webhookEventRepository.save(event);
        } catch (RuntimeException ex) {
            event.setProcessed(false);
            event.setErrorMessage(ex.getMessage());
            webhookEventRepository.save(event);
            throw ex;
        }

        return doc != null ? toResponse(requireDetailed(doc.getId())) : null;
    }

    private void markPaid(
            BillingDocument doc, BigDecimal paidAmount, Instant paidAt, String endToEndId, String eventId) {
        if (doc.getStatus() == BillingDocument.Status.PAID) {
            return;
        }
        if (doc.getBillingType() == BillingDocument.BillingType.PIX
                && doc.getPixCharge() != null
                && doc.getPixCharge().getExpiresAt() != null
                && Instant.now().isAfter(doc.getPixCharge().getExpiresAt())
                && doc.getStatus() != BillingDocument.Status.PAID) {
            // ainda aceita pagamento tardio reportado pelo provedor; marca divergência via notes
            appendHistory(doc, doc.getStatus(), doc.getStatus(), "Pagamento após expiração PIX", eventId);
        }
        BigDecimal amount = paidAmount != null
                ? paidAmount.setScale(2, RoundingMode.HALF_UP)
                : doc.getAmount();
        Instant at = paidAt != null ? paidAt : Instant.now();

        if (doc.getBankSlip() != null) {
            doc.getBankSlip().setPaidAt(at);
            doc.getBankSlip().setPaidAmount(amount);
        }
        if (doc.getPixCharge() != null) {
            doc.getPixCharge().setPaidAt(at);
            doc.getPixCharge().setPaidAmount(amount);
            if (StringUtils.hasText(endToEndId)) {
                doc.getPixCharge().setEndToEndId(endToEndId);
            }
        }

        BillingDocument.Status from = doc.getStatus();
        doc.setStatus(BillingDocument.Status.PAID);
        appendHistory(doc, from, BillingDocument.Status.PAID, "Pagamento recebido via webhook", eventId);

        if (doc.getReceivableInstallment() != null) {
            applyPaymentToInstallment(doc.getReceivableInstallment().getId(), amount);
        }
        documentRepository.save(doc);
        domainAuditService.record(
                "FINANCE", "BillingDocument", doc.getId(), AuditLog.AuditAction.UPDATE, null, null, "Cobrança paga");
    }

    private void applyPaymentToInstallment(UUID installmentId, BigDecimal amount) {
        ReceivableInstallment installment = installmentRepository
                .findForUpdate(installmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parcela não encontrada"));
        if (installment.getStatus() == ReceivableInstallment.Status.RECEIVED
                || installment.getStatus() == ReceivableInstallment.Status.CANCELLED
                || installment.getStatus() == ReceivableInstallment.Status.WRITTEN_OFF) {
            return;
        }
        BigDecimal apply = amount.min(installment.getBalanceAmount());
        BigDecimal newReceived = installment.getReceivedAmount().add(apply);
        installment.setReceivedAmount(newReceived);
        installment.setBalanceAmount(
                installment.getOriginalAmount().subtract(newReceived).max(BigDecimal.ZERO));
        if (installment.getBalanceAmount().compareTo(BigDecimal.ZERO) == 0) {
            installment.setStatus(ReceivableInstallment.Status.RECEIVED);
        } else {
            installment.setStatus(ReceivableInstallment.Status.PARTIALLY_RECEIVED);
        }
        installmentRepository.save(installment);
        receivableService.refreshReceivableAfterSettlement(installment.getReceivable().getId());
    }

    private BillingDocument requireDetailed(UUID id) {
        return documentRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cobrança não encontrada"));
    }

    private void appendHistory(
            BillingDocument doc,
            BillingDocument.Status from,
            BillingDocument.Status to,
            String notes,
            String externalEventId) {
        BillingStatusHistory h = new BillingStatusHistory();
        h.setBillingDocument(doc);
        h.setFromStatus(from != null ? from.name() : null);
        h.setToStatus(to.name());
        h.setNotes(notes);
        h.setExternalEventId(externalEventId);
        CurrentUser.id().ifPresent(h::setChangedBy);
        doc.getStatusHistory().add(h);
    }

    private BillingResponse toResponse(BillingDocument doc) {
        BankSlipResponse slip = null;
        if (doc.getBankSlip() != null) {
            BankSlip s = doc.getBankSlip();
            slip = new BankSlipResponse(
                    s.getDigitableLine(),
                    s.getBarcode(),
                    s.getNossoNumero(),
                    s.getBankCode(),
                    s.getWallet(),
                    s.getRegisteredAt());
        }
        PixChargeResponse pix = null;
        if (doc.getPixCharge() != null) {
            PixCharge p = doc.getPixCharge();
            pix = new PixChargeResponse(p.getTxid(), p.getQrCode(), p.getCopyPaste(), p.getExpiresAt(), p.getPaidAt());
        }
        List<StatusHistoryResponse> history = doc.getStatusHistory() == null
                ? List.of()
                : doc.getStatusHistory().stream()
                        .map(h -> new StatusHistoryResponse(
                                h.getFromStatus(), h.getToStatus(), h.getChangedAt(), h.getNotes(), h.getExternalEventId()))
                        .toList();
        return new BillingResponse(
                doc.getId(),
                doc.getOrganization().getId(),
                doc.getCustomer().getId(),
                doc.getReceivableInstallment() != null ? doc.getReceivableInstallment().getId() : null,
                doc.getBillingType().name(),
                doc.getAmount(),
                doc.getDueDate(),
                doc.getStatus().name(),
                doc.getExternalId(),
                doc.getProviderCode(),
                slip,
                pix,
                history);
    }
}
