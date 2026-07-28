package br.com.systemcommerce.fiscal.establishment.dto;

import jakarta.validation.constraints.Size;

public record FiscalEstablishmentSeriesRequest(
        @Size(max = 10) String nfeSeries, @Size(max = 10) String nfceSeries) {}
