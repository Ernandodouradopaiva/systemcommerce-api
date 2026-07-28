package br.com.systemcommerce.dashboard.dto;

import br.com.systemcommerce.sale.entity.Sale;
import java.math.BigDecimal;

public record StatusAmountMetric(Sale.SaleStatus status, long count, BigDecimal totalAmount) {}
