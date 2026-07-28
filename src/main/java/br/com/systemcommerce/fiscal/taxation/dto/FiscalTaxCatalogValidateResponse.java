package br.com.systemcommerce.fiscal.taxation.dto;

public record FiscalTaxCatalogValidateResponse(
        boolean valid, String code, String description, String catalogVersion, String message) {}
