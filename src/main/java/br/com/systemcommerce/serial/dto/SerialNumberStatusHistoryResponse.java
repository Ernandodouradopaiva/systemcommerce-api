package br.com.systemcommerce.serial.dto;

import br.com.systemcommerce.serial.entity.ProductSerialStatus;
import java.time.Instant;

public record SerialNumberStatusHistoryResponse(
        ProductSerialStatus fromStatus, ProductSerialStatus toStatus, String notes, Instant changedAt) {}
