package br.com.systemcommerce.finance.security;

/** Códigos de evento de auditoria financeira (Prompt 119). */
public final class FinanceAuditEvents {
    private FinanceAuditEvents() {}

    public static final String CREATE = "FINANCE_CREATE";
    public static final String UPDATE = "FINANCE_UPDATE";
    public static final String PAYMENT = "FINANCE_PAYMENT";
    public static final String RECEIPT = "FINANCE_RECEIPT";
    public static final String REVERSAL = "FINANCE_REVERSAL";
    public static final String DISCOUNT = "FINANCE_DISCOUNT";
    public static final String INTEREST = "FINANCE_INTEREST";
    public static final String FINE = "FINANCE_FINE";
    public static final String TRANSFER = "FINANCE_TRANSFER";
    public static final String RECONCILIATION = "FINANCE_RECONCILIATION";
    public static final String PERIOD_CLOSE = "FINANCE_PERIOD_CLOSE";
    public static final String PERIOD_REOPEN = "FINANCE_PERIOD_REOPEN";
    public static final String IMPORT = "FINANCE_IMPORT";
    public static final String EXPORT = "FINANCE_EXPORT";
    public static final String BALANCE_ACCESS = "FINANCE_BALANCE_ACCESS";
    public static final String DENIED_ACCESS = "FINANCE_DENIED_ACCESS";
    public static final String APPROVAL_REQUEST = "FINANCE_APPROVAL_REQUEST";
    public static final String APPROVAL_DECIDE = "FINANCE_APPROVAL_DECIDE";
}
