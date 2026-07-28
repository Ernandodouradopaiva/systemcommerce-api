package br.com.systemcommerce.report.dto;

import java.time.Instant;
import java.util.UUID;

public record CustomerReportRow(
        UUID id, String name, String document, String type, String status, Instant createdAt) {}
