package br.com.systemcommerce.pos.cancellation.dto;

import jakarta.validation.constraints.Size;

public record CancellationDecisionRequest(@Size(max = 500) String decisionNotes) {}
