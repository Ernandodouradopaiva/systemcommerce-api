package br.com.systemcommerce.finance.billing.adapter;

import br.com.systemcommerce.finance.billing.entity.BillingDocument;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Adapter de provedor bancário — o domínio não depende de banco específico.
 * Integração real pode ser plugada posteriormente.
 */
public interface BillingProviderAdapter {

    String providerCode();

    BankSlipRegistration registerBankSlip(BillingDocument document);

    PixRegistration registerPix(BillingDocument document, Instant expiresAt);

    void cancel(BillingDocument document);

    record BankSlipRegistration(
            String externalId,
            String digitableLine,
            String barcode,
            String nossoNumero,
            String bankCode,
            String wallet,
            String pdfUrl) {}

    record PixRegistration(
            String externalId,
            String txid,
            String qrCode,
            String qrCodeImageUrl,
            String copyPaste,
            Instant expiresAt) {}

    record PaymentNotification(
            String externalId,
            String eventId,
            String eventType,
            BigDecimal paidAmount,
            Instant paidAt,
            String endToEndId,
            String payload) {}
}
