package br.com.systemcommerce.pricing.dto;

import jakarta.validation.constraints.Size;

public record DiscountAuthorizationDecisionRequest(@Size(max = 500) String decisionNotes) {}
