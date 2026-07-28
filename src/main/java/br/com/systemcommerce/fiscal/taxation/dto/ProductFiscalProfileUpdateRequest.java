package br.com.systemcommerce.fiscal.taxation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProductFiscalProfileUpdateRequest(
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
        LocalDate validFrom,
        LocalDate validUntil,
        List<ProductTaxClassificationRequest> classifications) {}
