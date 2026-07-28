package br.com.systemcommerce.finance.costcenter.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CostCenterAssignStoreRequest(@NotNull UUID storeId, Boolean primaryAssignment) {}
