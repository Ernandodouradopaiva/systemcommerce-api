package br.com.systemcommerce.fiscal.taxation.dto;

import br.com.systemcommerce.fiscal.taxation.entity.ProductTaxClassification;
import java.util.UUID;

public record ProductTaxClassificationResponse(
        UUID id,
        String taxType,
        String cstOrCsosn,
        String cfopCode,
        String extraJson,
        ProductTaxClassification.ClassificationStatus status) {}
