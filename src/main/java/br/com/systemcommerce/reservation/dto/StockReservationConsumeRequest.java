package br.com.systemcommerce.reservation.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record StockReservationConsumeRequest(@NotEmpty @Valid List<StockReservationLineRequest> items) {}
