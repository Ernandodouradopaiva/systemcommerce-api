package br.com.systemcommerce.storeaccess.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StoreContextSwitchRequest(@NotNull UUID storeId) {}
