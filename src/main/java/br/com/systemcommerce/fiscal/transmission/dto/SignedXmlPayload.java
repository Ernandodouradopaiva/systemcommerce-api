package br.com.systemcommerce.fiscal.transmission.dto;

public record SignedXmlPayload(byte[] xmlUtf8, String accessKey) {}
