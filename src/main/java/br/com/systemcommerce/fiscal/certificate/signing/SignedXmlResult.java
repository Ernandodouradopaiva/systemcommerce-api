package br.com.systemcommerce.fiscal.certificate.signing;

public record SignedXmlResult(byte[] signedXmlUtf8, String thumbprint, String signatureId) {}
