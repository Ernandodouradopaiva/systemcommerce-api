package br.com.systemcommerce.carrier.dto;

import java.util.UUID;

public record CarrierContactResponse(
        UUID id, String name, String phone, String email, String roleLabel, boolean primaryContact) {}
