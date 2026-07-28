package br.com.systemcommerce.pos.sale.dto;



import jakarta.validation.constraints.NotNull;

import jakarta.validation.constraints.Size;

import java.util.UUID;



public record PosSaleCustomerRequest(@NotNull UUID customerId, Long expectedVersion) {}


