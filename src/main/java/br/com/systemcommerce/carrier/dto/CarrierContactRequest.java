package br.com.systemcommerce.carrier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CarrierContactRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 30) String phone,
        @Size(max = 255) String email,
        @Size(max = 80) String roleLabel,
        Boolean primaryContact) {}
