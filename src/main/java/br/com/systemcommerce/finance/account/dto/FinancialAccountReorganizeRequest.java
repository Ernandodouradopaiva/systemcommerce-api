package br.com.systemcommerce.finance.account.dto;

import java.util.UUID;

public record FinancialAccountReorganizeRequest(UUID newParentId, Integer sortOrder) {}
