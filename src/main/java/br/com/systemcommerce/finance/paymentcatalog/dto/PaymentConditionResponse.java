package br.com.systemcommerce.finance.paymentcatalog.dto;

import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PaymentConditionResponse(
        UUID id,
        UUID organizationId,
        String code,
        String name,
        PaymentCondition.ConditionType conditionType,
        Integer installmentCount,
        Integer intervalDays,
        Integer firstDueDays,
        BigDecimal minAmount,
        boolean allowsPurchase,
        boolean allowsSale,
        boolean allowsPos,
        PaymentCondition.ConditionStatus status,
        boolean usable,
        List<InstallmentResponse> installments,
        Long version) {}
