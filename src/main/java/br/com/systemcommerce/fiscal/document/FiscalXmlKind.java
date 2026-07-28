package br.com.systemcommerce.fiscal.document;

/** Tipos de XML armazenados em {@code fiscal_document_xmls}. */
public final class FiscalXmlKind {

    public static final String OUTBOUND_UNSIGNED = "OUTBOUND_UNSIGNED";
    public static final String OUTBOUND_SIGNED = "OUTBOUND_SIGNED";
    public static final String AUTHORIZED = "AUTHORIZED";
    public static final String EVENT = "EVENT";
    public static final String VOIDING = "VOIDING";

    private FiscalXmlKind() {}
}
