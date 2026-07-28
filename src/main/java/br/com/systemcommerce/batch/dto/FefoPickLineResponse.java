package br.com.systemcommerce.batch.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record FefoPickLineResponse(UUID batchId, String batchCode, BigDecimal quantity) {}
