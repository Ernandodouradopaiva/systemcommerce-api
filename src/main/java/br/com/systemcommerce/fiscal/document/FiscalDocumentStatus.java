package br.com.systemcommerce.fiscal.document;

/**
 * Status interno do DFe (independente do cStat SEFAZ).
 * Inclui valores do Prompt 129 e aliases usados no fluxo atual.
 */
public enum FiscalDocumentStatus {
    DRAFT,
    VALIDATING,
    VALIDATION_FAILED,
    VALIDATED,
    READY_TO_SIGN,
    SIGNED,
    QUEUED,
    SENT,
    TRANSMITTED,
    PROCESSING,
    AUTHORIZED,
    /** Autorizado na SEFAZ; estoque/financeiro ainda não consolidados (Prompt 150). */
    AUTHORIZED_PENDING_INTEGRATION,
    REJECTED,
    DENIED,
    DENIED_WHEN_APPLICABLE,
    CANCEL_PENDING,
    CANCELLED,
    CORRECTED,
    CONTINGENCY,
    CONTINGENCY_PENDING,
    CONTINGENCY_TRANSMITTED,
    VOIDED,
    ERROR
}
