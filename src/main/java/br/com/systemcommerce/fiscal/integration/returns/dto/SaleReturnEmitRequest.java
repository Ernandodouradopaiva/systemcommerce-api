package br.com.systemcommerce.fiscal.integration.returns.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SaleReturnEmitRequest(
        UUID saleReturnId,
        UUID saleId,
        UUID originalDocumentId,
        String originalAccessKey) {

    public boolean hasSaleReturnId() {
        return saleReturnId != null;
    }
}
