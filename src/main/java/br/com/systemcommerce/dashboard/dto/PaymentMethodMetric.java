package br.com.systemcommerce.dashboard.dto;

import br.com.systemcommerce.payment.entity.Payment;
import java.math.BigDecimal;

public record PaymentMethodMetric(Payment.PaymentMethod method, BigDecimal totalAmount, long count) {}
