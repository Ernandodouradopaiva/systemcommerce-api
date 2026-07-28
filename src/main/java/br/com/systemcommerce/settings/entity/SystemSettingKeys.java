package br.com.systemcommerce.settings.entity;

import java.util.Map;
import java.util.Set;

public final class SystemSettingKeys {

    public static final String REQUIRE_SELLER = "REQUIRE_SELLER";
    public static final String ALLOW_NEGATIVE_STOCK = "ALLOW_NEGATIVE_STOCK";
    public static final String DEFAULT_WAREHOUSE = "DEFAULT_WAREHOUSE";
    public static final String REQUIRE_CUSTOMER_ON_SALE = "REQUIRE_CUSTOMER_ON_SALE";
    public static final String UI_THEME = "UI_THEME";

    public static final Set<String> USER_SCOPE_WHITELIST = Set.of(UI_THEME);

    public static final Map<String, String> DEFAULTS = Map.of(
            REQUIRE_SELLER, "false",
            ALLOW_NEGATIVE_STOCK, "false",
            DEFAULT_WAREHOUSE, "",
            REQUIRE_CUSTOMER_ON_SALE, "false",
            UI_THEME, "light");

    private SystemSettingKeys() {}
}
