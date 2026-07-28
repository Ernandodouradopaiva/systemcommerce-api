package br.com.systemcommerce.fiscal.taxation.dto;

import br.com.systemcommerce.fiscal.taxation.entity.ProductFiscalProfile;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProductFiscalProfileResponse(
        UUID id,
        UUID productId,
        UUID organizationId,
        UUID storeId,
        String uf,
        String ncmCode,
        String cestCode,
        String exTipi,
        String originCode,
        String commercialUom,
        String taxableUom,
        BigDecimal conversionFactor,
        String gtinCommercial,
        String gtinTaxable,
        String ipiFraming,
        String relevantScaleIndicator,
        String manufacturerCnpj,
        String benefitCode,
        ProductFiscalProfile.ProfileStatus status,
        boolean usable,
        LocalDate validFrom,
        LocalDate validUntil,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        List<ProductTaxClassificationResponse> classifications) {}
