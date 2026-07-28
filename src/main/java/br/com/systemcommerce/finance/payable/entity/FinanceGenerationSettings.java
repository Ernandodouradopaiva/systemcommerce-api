package br.com.systemcommerce.finance.payable.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "finance_generation_settings")
public class FinanceGenerationSettings extends AuditableEntity {

    public enum PayableGenerationMode {
        ON_ORDER_APPROVED,
        ON_RECEIPT,
        ON_INVOICE_ENTRY,
        MANUAL
    }

    public enum FreightHandling {
        INCORPORATED,
        SEPARATE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Legado — permanece alinhado ao modo ON_RECEIPT. */
    @Column(name = "generate_payable_on_receipt", nullable = false)
    private Boolean generatePayableOnReceipt = true;

    @Column(name = "generate_receivable_on_invoice", nullable = false)
    private Boolean generateReceivableOnInvoice = true;

    @Column(name = "generate_and_settle_pos_cash", nullable = false)
    private Boolean generateAndSettlePosCash = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "payable_generation_mode", nullable = false, length = 40)
    private PayableGenerationMode payableGenerationMode = PayableGenerationMode.ON_RECEIPT;

    @Enumerated(EnumType.STRING)
    @Column(name = "freight_handling", nullable = false, length = 40)
    private FreightHandling freightHandling = FreightHandling.INCORPORATED;

    @Column(name = "segregate_taxes", nullable = false)
    private Boolean segregateTaxes = false;

    @Column(name = "generate_payable_on_order_approved", nullable = false)
    private Boolean generatePayableOnOrderApproved = false;

    @Column(name = "generate_payable_on_invoice_entry", nullable = false)
    private Boolean generatePayableOnInvoiceEntry = false;

    @Column(name = "settle_pos_cash", nullable = false)
    private Boolean settlePosCash = true;

    @Column(name = "settle_pos_pix", nullable = false)
    private Boolean settlePosPix = true;

    /** Cartão: false = deixa previsão em aberto (adquirente); true = liquida na hora. */
    @Column(name = "settle_pos_card_immediately", nullable = false)
    private Boolean settlePosCardImmediately = false;

    @Column(name = "pos_pix_holder_id")
    private UUID posPixHolderId;

    @Column(name = "pos_card_acquirer_holder_id")
    private UUID posCardAcquirerHolderId;

    public boolean shouldGeneratePayableOnReceipt() {
        if (payableGenerationMode == PayableGenerationMode.ON_RECEIPT) {
            return Boolean.TRUE.equals(generatePayableOnReceipt);
        }
        return false;
    }

    public boolean shouldGeneratePayableOnOrderApproved() {
        return payableGenerationMode == PayableGenerationMode.ON_ORDER_APPROVED
                || Boolean.TRUE.equals(generatePayableOnOrderApproved);
    }

    public boolean shouldGeneratePayableOnInvoiceEntry() {
        return payableGenerationMode == PayableGenerationMode.ON_INVOICE_ENTRY
                || Boolean.TRUE.equals(generatePayableOnInvoiceEntry);
    }
}
