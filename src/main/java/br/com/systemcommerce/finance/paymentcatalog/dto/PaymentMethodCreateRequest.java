package br.com.systemcommerce.finance.paymentcatalog.dto;

import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PaymentMethodCreateRequest(
        @NotNull UUID organizationId,
        @NotBlank @Size(max = 40) String code,
        @NotBlank @Size(max = 120) String name,
        @NotNull PaymentMethod.MethodType methodType,
        Boolean allowsPurchase,
        Boolean allowsSale,
        Boolean allowsPos,
        Integer sortOrder) {}
