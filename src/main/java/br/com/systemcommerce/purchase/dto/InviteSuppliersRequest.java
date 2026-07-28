package br.com.systemcommerce.purchase.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record InviteSuppliersRequest(@NotEmpty List<UUID> supplierIds) {}
