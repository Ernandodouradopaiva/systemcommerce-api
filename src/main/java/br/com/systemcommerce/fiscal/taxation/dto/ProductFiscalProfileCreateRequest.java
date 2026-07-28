package br.com.systemcommerce.fiscal.taxation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ProductFiscalProfileCreateRequest(
        @NotNull UUID productId,
        @NotNull UUID organizationId,
        UUID storeId,
        @Size(max = 2) String uf,
        @NotBlank @Size(max = 10) String ncmCode,
        @Size(max = 10) String cestCode,
        @Size(max = 10) String exTipi,
        @NotBlank @Size(max = 5) String originCode,
        @Size(max = 10) String commercialUom,
        @Size(max = 10) String taxableUom,
        BigDecimal conversionFactor,
        @Size(max = 14) String gtinCommercial,
        @Size(max = 14) String gtinTaxable,
        @Size(max = 20) String ipiFraming,
        @Size(max = 20) String relevantScaleIndicator,
        @Size(max = 14) String manufacturerCnpj,
        @Size(max = 20) String benefitCode,
        @NotNull LocalDate validFrom,
        LocalDate validUntil,
        List<ProductTaxClassificationRequest> classifications) {}
