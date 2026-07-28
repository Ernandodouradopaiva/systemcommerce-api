package br.com.systemcommerce.purchase.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Cria recebimento em DRAFT (Prompt 62) — não movimenta estoque. */
public record GoodsReceiptCreateRequest(
        @NotNull UUID purchaseOrderId,
        @NotNull LocalDate receiptDate,
        @Size(max = 80) String invoiceNumber,
        @Size(max = 20) String invoiceSeries,
        @Size(max = 60) String accessKey,
        LocalDate invoiceIssuedAt,
        @Size(max = 200) String carrierName,
        @Size(max = 2000) String notes,
        @NotEmpty @Valid List<GoodsReceiptItemRequest> items) {}
