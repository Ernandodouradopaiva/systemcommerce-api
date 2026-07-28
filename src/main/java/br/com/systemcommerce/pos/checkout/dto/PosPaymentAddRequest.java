package br.com.systemcommerce.pos.checkout.dto;

import br.com.systemcommerce.payment.entity.Payment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PosPaymentAddRequest(
        @NotNull(message = "Forma de pagamento é obrigatória") Payment.PaymentMethod method,
        @NotNull(message = "Valor é obrigatório")
                @DecimalMin(value = "0.01", message = "Valor deve ser positivo")
                BigDecimal amount,
        @DecimalMin(value = "0.00", message = "Valor recebido não pode ser negativo") BigDecimal tenderedAmount,
        @Min(value = 1, message = "Parcelas devem ser pelo menos 1") Integer installments,
        @Size(max = 100) String externalReference,
        @Size(max = 500) String notes,
        @Size(max = 60) String authorizationCode,
        @Size(max = 60) String nsu,
        @Size(max = 40) String cardBrand,
        @Size(max = 60) String acquirer,
        Boolean confirmImmediately) {}
