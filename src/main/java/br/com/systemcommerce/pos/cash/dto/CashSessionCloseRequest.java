package br.com.systemcommerce.pos.cash.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record CashSessionCloseRequest(
        @NotNull(message = "valor informado é obrigatório")
                @DecimalMin(value = "0.00", message = "Valor informado não pode ser negativo")
                BigDecimal countedAmount,
        @Size(max = 1000) String closingNotes,
        @Valid List<CashConferenceRequest.CountedByMethod> countedByMethod) {

    public CashSessionCloseRequest(BigDecimal countedAmount, String closingNotes) {
        this(countedAmount, closingNotes, null);
    }
}
