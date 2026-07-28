package br.com.systemcommerce.payment.dto;

import br.com.systemcommerce.payment.entity.Payment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentCreateRequest(
        @NotNull(message = "Venda é obrigatória") UUID saleId,
        @NotNull(message = "Forma de pagamento é obrigatória") Payment.PaymentMethod method,
        @NotNull(message = "Valor é obrigatório")
                @DecimalMin(value = "0.01", message = "Valor deve ser positivo")
                BigDecimal amount,
        Instant paidAt,
        @Size(max = 100) String externalReference,
        @Size(max = 500) String notes,
        @Min(value = 1, message = "Parcelas devem ser pelo menos 1") Integer installments,
        @DecimalMin(value = "0.00", message = "Valor recebido não pode ser negativo") BigDecimal tenderedAmount,
        Boolean confirmImmediately,
        @Size(max = 60) String authorizationCode,
        @Size(max = 60) String nsu,
        @Size(max = 40) String cardBrand,
        @Size(max = 60) String acquirer) {

    /** Compatibilidade com chamadas sem metadados TEF. */
    public PaymentCreateRequest(
            UUID saleId,
            Payment.PaymentMethod method,
            BigDecimal amount,
            Instant paidAt,
            String externalReference,
            String notes,
            Integer installments,
            BigDecimal tenderedAmount,
            Boolean confirmImmediately) {
        this(
                saleId,
                method,
                amount,
                paidAt,
                externalReference,
                notes,
                installments,
                tenderedAmount,
                confirmImmediately,
                null,
                null,
                null,
                null);
    }
}
