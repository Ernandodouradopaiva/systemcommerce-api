package br.com.systemcommerce.finance.payable.service;

import br.com.systemcommerce.finance.payable.dto.FinanceGenerationSettingsResponse;
import br.com.systemcommerce.finance.payable.dto.FinanceGenerationSettingsUpdateRequest;
import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import br.com.systemcommerce.finance.payable.repository.FinanceGenerationSettingsRepository;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FinanceGenerationSettingsService {

    private final FinanceGenerationSettingsRepository settingsRepository;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public FinanceGenerationSettingsResponse get(UUID organizationId) {
        return toResponse(requireOrCreate(organizationId));
    }

    @Transactional
    public FinanceGenerationSettings requireOrCreate(UUID organizationId) {
        return settingsRepository
                .findByOrganizationId(organizationId)
                .orElseGet(() -> {
                    Organization org = organizationService.requireUsable(organizationId);
                    FinanceGenerationSettings s = new FinanceGenerationSettings();
                    s.setOrganization(org);
                    s.setPayableGenerationMode(FinanceGenerationSettings.PayableGenerationMode.ON_RECEIPT);
                    s.setFreightHandling(FinanceGenerationSettings.FreightHandling.INCORPORATED);
                    s.setGeneratePayableOnReceipt(true);
                    s.setGenerateReceivableOnInvoice(true);
                    s.setGenerateAndSettlePosCash(true);
                    return settingsRepository.save(s);
                });
    }

    @Transactional
    public FinanceGenerationSettingsResponse update(FinanceGenerationSettingsUpdateRequest request) {
        FinanceGenerationSettings s = requireOrCreate(request.organizationId());
        if (request.payableGenerationMode() != null) {
            s.setPayableGenerationMode(request.payableGenerationMode());
            s.setGeneratePayableOnReceipt(
                    request.payableGenerationMode()
                            == FinanceGenerationSettings.PayableGenerationMode.ON_RECEIPT);
            s.setGeneratePayableOnOrderApproved(
                    request.payableGenerationMode()
                            == FinanceGenerationSettings.PayableGenerationMode.ON_ORDER_APPROVED);
            s.setGeneratePayableOnInvoiceEntry(
                    request.payableGenerationMode()
                            == FinanceGenerationSettings.PayableGenerationMode.ON_INVOICE_ENTRY);
        }
        if (request.freightHandling() != null) {
            s.setFreightHandling(request.freightHandling());
        }
        if (request.segregateTaxes() != null) {
            s.setSegregateTaxes(request.segregateTaxes());
        }
        if (request.generatePayableOnReceipt() != null) {
            s.setGeneratePayableOnReceipt(request.generatePayableOnReceipt());
        }
        if (request.generatePayableOnOrderApproved() != null) {
            s.setGeneratePayableOnOrderApproved(request.generatePayableOnOrderApproved());
        }
        if (request.generatePayableOnInvoiceEntry() != null) {
            s.setGeneratePayableOnInvoiceEntry(request.generatePayableOnInvoiceEntry());
        }
        if (request.generateReceivableOnInvoice() != null) {
            s.setGenerateReceivableOnInvoice(request.generateReceivableOnInvoice());
        }
        if (request.generateAndSettlePosCash() != null) {
            s.setGenerateAndSettlePosCash(request.generateAndSettlePosCash());
        }
        if (request.settlePosCash() != null) {
            s.setSettlePosCash(request.settlePosCash());
        }
        if (request.settlePosPix() != null) {
            s.setSettlePosPix(request.settlePosPix());
        }
        if (request.settlePosCardImmediately() != null) {
            s.setSettlePosCardImmediately(request.settlePosCardImmediately());
        }
        if (request.posPixHolderId() != null) {
            s.setPosPixHolderId(request.posPixHolderId());
        }
        if (request.posCardAcquirerHolderId() != null) {
            s.setPosCardAcquirerHolderId(request.posCardAcquirerHolderId());
        }
        settingsRepository.save(s);
        domainAuditService.record(
                "FINANCE",
                "FinanceGenerationSettings",
                s.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                null,
                "Configuração de geração financeira atualizada");
        return toResponse(s);
    }

    public FinanceGenerationSettings require(UUID organizationId) {
        return settingsRepository
                .findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Configuração financeira não encontrada"));
    }

    private FinanceGenerationSettingsResponse toResponse(FinanceGenerationSettings s) {
        return new FinanceGenerationSettingsResponse(
                s.getId(),
                s.getOrganization().getId(),
                s.getPayableGenerationMode(),
                s.getFreightHandling(),
                s.getSegregateTaxes(),
                s.getGeneratePayableOnReceipt(),
                s.getGeneratePayableOnOrderApproved(),
                s.getGeneratePayableOnInvoiceEntry(),
                s.getGenerateReceivableOnInvoice(),
                s.getGenerateAndSettlePosCash(),
                s.getSettlePosCash(),
                s.getSettlePosPix(),
                s.getSettlePosCardImmediately(),
                s.getPosPixHolderId(),
                s.getPosCardAcquirerHolderId());
    }
}
