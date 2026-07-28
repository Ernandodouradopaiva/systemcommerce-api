package br.com.systemcommerce.fiscal.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FiscalDocumentAttachXmlRequest(@NotBlank @Size(max = 40) String kind, @NotBlank String content) {}
