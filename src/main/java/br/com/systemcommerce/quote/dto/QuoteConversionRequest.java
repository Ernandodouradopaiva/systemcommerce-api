package br.com.systemcommerce.quote.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * Conversão total (quando {@code items} vazio/nulo) ou parcial (itens/quantidades informados) do orçamento em
 * pedido de venda. {@code forceExpired} exige a permissão {@code QUOTE_FORCE_CONVERT_EXPIRED}.
 */
public record QuoteConversionRequest(@Valid List<QuoteConversionItemRequest> items, Boolean forceExpired) {}
