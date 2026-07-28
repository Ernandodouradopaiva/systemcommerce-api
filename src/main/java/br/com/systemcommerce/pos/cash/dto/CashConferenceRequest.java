package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.payment.entity.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** Contagem informada pelo operador — diferenças são calculadas somente na API. */
public record CashConferenceRequest(
        @NotNull(message = "valor contado em dinheiro é obrigatório")
                @DecimalMin(value = "0.00", message = "Valor contado não pode ser negativo")
                BigDecimal countedAmount,
        @Valid List<CountedByMethod> countedByMethod) {

    public record CountedByMethod(
            @NotNull Payment.PaymentMethod method,
            @NotNull @DecimalMin(value = "0.00") BigDecimal amount) {}
}
