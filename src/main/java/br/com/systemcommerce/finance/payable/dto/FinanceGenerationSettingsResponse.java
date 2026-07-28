package br.com.systemcommerce.finance.payable.dto;

import br.com.systemcommerce.finance.payable.entity.FinanceGenerationSettings;
import java.util.UUID;

public record FinanceGenerationSettingsResponse(
        UUID id,
        UUID organizationId,
        FinanceGenerationSettings.PayableGenerationMode payableGenerationMode,
        FinanceGenerationSettings.FreightHandling freightHandling,
        Boolean segregateTaxes,
        Boolean generatePayableOnReceipt,
        Boolean generatePayableOnOrderApproved,
        Boolean generatePayableOnInvoiceEntry,
        Boolean generateReceivableOnInvoice,
        Boolean generateAndSettlePosCash,
        Boolean settlePosCash,
        Boolean settlePosPix,
        Boolean settlePosCardImmediately,
        UUID posPixHolderId,
        UUID posCardAcquirerHolderId) {}
