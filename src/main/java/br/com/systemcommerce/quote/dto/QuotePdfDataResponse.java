package br.com.systemcommerce.quote.dto;

/** Dados agregados para renderização de PDF do orçamento no front (Prompt 64) — sem regra comercial. */
public record QuotePdfDataResponse(
        QuoteResponse quote,
        String organizationLegalName,
        String organizationDocument,
        String storeName,
        String storeDocument) {}
