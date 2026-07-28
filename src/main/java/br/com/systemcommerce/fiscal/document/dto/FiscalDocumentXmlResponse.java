package br.com.systemcommerce.fiscal.document.dto;

import java.time.Instant;
import java.util.UUID;

public record FiscalDocumentXmlResponse(UUID id, String kind, String sha256, Instant storedAt) {}
