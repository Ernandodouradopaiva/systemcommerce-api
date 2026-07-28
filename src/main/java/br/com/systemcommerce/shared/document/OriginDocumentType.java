package br.com.systemcommerce.shared.document;

/**
 * Tipos de documento suportados pela rastreabilidade documental (Prompt 56).
 * Deve espelhar o CHECK constraint de {@code document_conversions} (V183).
 */
public enum OriginDocumentType {
    PURCHASE_REQUEST,
    SUPPLIER_QUOTATION,
    PURCHASE_ORDER,
    PURCHASE_RECEIPT,
    SUPPLIER_RETURN,
    QUOTE,
    SALES_ORDER,
    STOCK_RESERVATION,
    PICKING_ORDER,
    SHIPMENT,
    SALE,
    INVOICE_PROCESS
}
