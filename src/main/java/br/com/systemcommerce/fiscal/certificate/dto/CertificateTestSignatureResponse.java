package br.com.systemcommerce.fiscal.certificate.dto;

public record CertificateTestSignatureResponse(boolean success, String message, String signatureBase64) {}
