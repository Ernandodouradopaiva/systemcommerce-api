package br.com.systemcommerce.pos.report.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Linha agregada genérica de relatório PDV. */
public record PosAggRow(
        UUID id,
        String code,
        String name,
        long count,
        BigDecimal quantity,
        BigDecimal totalAmount,
        BigDecimal averageTicket,
        BigDecimal discountAmount,
        String extra) {}
