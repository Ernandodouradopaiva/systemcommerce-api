package br.com.systemcommerce.finance.paymentcatalog.dto;

import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentConditionCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull PaymentCondition.ConditionType conditionType,
        Integer installmentCount,
        Integer intervalDays,
        Integer firstDueDays,
        BigDecimal minAmount,
        Boolean allowsPurchase,
        Boolean allowsSale,
        Boolean allowsPos,
        @NotEmpty @Valid List<InstallmentRequest> installments) {}
