package br.com.systemcommerce.shipment.dto;

import br.com.systemcommerce.shipment.entity.DeliveryProof;
import java.time.Instant;
import java.util.UUID;

public record DeliveryProofResponse(
        UUID id, DeliveryProof.ProofType proofType, String storageRef, String recipientName, Instant capturedAt) {}
