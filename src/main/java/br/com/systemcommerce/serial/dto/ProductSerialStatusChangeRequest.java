package br.com.systemcommerce.serial.dto;

import br.com.systemcommerce.serial.entity.ProductSerialStatus;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProductSerialStatusChangeRequest(@NotNull ProductSerialStatus targetStatus, String notes) {}
