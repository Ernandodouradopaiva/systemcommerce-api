package br.com.systemcommerce.finance.paymentcatalog.dto;

import java.util.List;

public record CalculateDueDatesResponse(List<DueDateItem> installments) {}
