package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.payment.entity.Payment;
import java.math.BigDecimal;

public record PaymentMethodTotal(Payment.PaymentMethod method, BigDecimal amount) {}
