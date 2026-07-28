package br.com.systemcommerce.pos.settings.entity;

/** Chaves canônicas das configurações do PDV. */
public final class PosSettingKeys {

    public static final String REQUIRE_CUSTOMER_ON_SALE = "REQUIRE_CUSTOMER_ON_SALE";
    public static final String ALLOW_UNIDENTIFIED_CONSUMER = "ALLOW_UNIDENTIFIED_CONSUMER";
    public static final String ALLOW_NEGATIVE_STOCK = "ALLOW_NEGATIVE_STOCK";
    public static final String DEFAULT_OPERATOR_DISCOUNT_PERCENT = "DEFAULT_OPERATOR_DISCOUNT_PERCENT";
    public static final String HIGH_WITHDRAWAL_LIMIT = "HIGH_WITHDRAWAL_LIMIT";
    public static final String MAX_CASH_IN_DRAWER = "MAX_CASH_IN_DRAWER";
    public static final String AUTO_WITHDRAWAL_ALERT = "AUTO_WITHDRAWAL_ALERT";
    public static final String ALLOW_SUSPENDED_SALE = "ALLOW_SUSPENDED_SALE";
    public static final String SUSPENDED_SALE_TTL_HOURS = "SUSPENDED_SALE_TTL_HOURS";
    public static final String AUTO_PRINT = "AUTO_PRINT";
    public static final String PRINT_COPIES = "PRINT_COPIES";
    public static final String PRINTER_WIDTH = "PRINTER_WIDTH";
    public static final String RECEIPT_FOOTER_MESSAGE = "RECEIPT_FOOTER_MESSAGE";
    public static final String REQUIRE_ITEM_CANCEL_REASON = "REQUIRE_ITEM_CANCEL_REASON";
    public static final String REQUIRE_CANCEL_AUTHORIZATION = "REQUIRE_CANCEL_AUTHORIZATION";
    public static final String ENABLED_PAYMENT_METHODS = "ENABLED_PAYMENT_METHODS";
    public static final String MAX_INSTALLMENTS = "MAX_INSTALLMENTS";
    public static final String MIN_INSTALLMENT_AMOUNT = "MIN_INSTALLMENT_AMOUNT";
    public static final String ALLOW_CLOSE_WITH_DIFFERENCE = "ALLOW_CLOSE_WITH_DIFFERENCE";
    public static final String CLOSE_DIFFERENCE_LIMIT = "CLOSE_DIFFERENCE_LIMIT";
    public static final String REQUIRE_CLOSE_JUSTIFICATION = "REQUIRE_CLOSE_JUSTIFICATION";
    public static final String INACTIVITY_TIMEOUT_MINUTES = "INACTIVITY_TIMEOUT_MINUTES";
    public static final String BLOCK_TERMINAL_ON_INACTIVITY = "BLOCK_TERMINAL_ON_INACTIVITY";
    public static final String SOUNDS_ENABLED = "SOUNDS_ENABLED";
    public static final String SHORTCUTS_JSON = "SHORTCUTS_JSON";

    private PosSettingKeys() {}
}
