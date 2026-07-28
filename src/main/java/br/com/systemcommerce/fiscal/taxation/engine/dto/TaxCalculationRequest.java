package br.com.systemcommerce.fiscal.taxation.engine.dto;

import br.com.systemcommerce.fiscal.party.TaxpayerIndicator;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationChannel;
import br.com.systemcommerce.fiscal.taxation.engine.CalculationPurpose;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TaxCalculationRequest(
        @NotNull UUID organizationId,
        @NotNull UUID storeId,
        UUID establishmentId,
        Boolean simulation,
        LocalDate issuedOn,
        String operationCode,
        CalculationChannel channel,
        String originUf,
        String destinationUf,
        String destinationIbge,
        CalculationPurpose purpose,
        Boolean finalConsumer,
        TaxpayerIndicator taxpayerIndicator,
        String originDocumentType,
        UUID originDocumentId,
        @NotEmpty @Valid List<TaxCalculationItemRequest> items) {}
