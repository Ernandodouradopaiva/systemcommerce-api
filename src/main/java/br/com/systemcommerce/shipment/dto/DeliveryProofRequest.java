package br.com.systemcommerce.shipment.dto;

import br.com.systemcommerce.shipment.entity.DeliveryProof;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeliveryProofRequest(
        @NotNull DeliveryProof.ProofType proofType, @NotBlank String storageRef, String recipientName) {}
