package br.com.systemcommerce.finance.paymentcatalog.dto;

import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentMethod;
import java.util.UUID;

public record PaymentMethodResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        PaymentMethod.MethodType methodType,
        boolean allowsPurchase,
        boolean allowsSale,
        boolean allowsPos,
        PaymentMethod.MethodStatus status,
        boolean usable,
        Integer sortOrder,
        Long version) {}
