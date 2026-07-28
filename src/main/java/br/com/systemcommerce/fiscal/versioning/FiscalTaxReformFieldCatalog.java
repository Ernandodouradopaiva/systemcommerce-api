package br.com.systemcommerce.fiscal.versioning;

/**
 * Catálogo de campos da Reforma Tributária (IBS/CBS/IS) para mapeamento XML versionado.
 * Campos atuais do leiaute legado NÃO são removidos — convivência via feature flags.
 */
public final class FiscalTaxReformFieldCatalog {

    public static final String IBS = "IBS";
    public static final String CBS = "CBS";
    public static final String IMPOSTO_SELETIVO = "IS";
    public static final String C_CLASS_TRIB = "cClassTrib";
    public static final String NT_2025_002 = "2025.002";

    private FiscalTaxReformFieldCatalog() {}
}
