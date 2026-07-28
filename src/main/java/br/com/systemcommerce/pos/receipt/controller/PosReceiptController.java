package br.com.systemcommerce.pos.receipt.controller;

import br.com.systemcommerce.pos.receipt.dto.PosReceiptResponse;
import br.com.systemcommerce.pos.receipt.dto.ReceiptPrintLogResponse;
import br.com.systemcommerce.pos.receipt.dto.ReceiptPrintRequest;
import br.com.systemcommerce.pos.receipt.dto.ReceiptReprintRequest;
import br.com.systemcommerce.pos.receipt.entity.ReceiptPrintLog;
import br.com.systemcommerce.pos.receipt.service.PosReceiptService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/receipts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "POS Receipts",
        description =
                """
                Comprovantes oficiais do PDV (não fiscais). A API monta os dados; o front apenas formata \
                e envia à impressora do navegador. Reimpressão é auditada.
                """)
public class PosReceiptController {

    private final PosReceiptService posReceiptService;

    @GetMapping
    @PreAuthorize("hasAuthority('POS_RECEIPT_PRINT')")
    @Operation(summary = "Obtém dados oficiais do comprovante (sem registrar impressão)")
    public ResponseEntity<ApiResponse<PosReceiptResponse>> getReceipt(
            @RequestParam ReceiptPrintLog.PrintType type,
            @RequestParam(required = false) UUID saleId,
            @RequestParam(required = false) UUID paymentId,
            @RequestParam(required = false) UUID cashSessionId,
            @RequestParam(required = false) UUID cashMovementId,
            @RequestParam(required = false) UUID saleCancellationId) {
        return ResponseEntity.ok(ApiResponse.of(posReceiptService.getReceipt(
                type, saleId, paymentId, cashSessionId, cashMovementId, saleCancellationId)));
    }

    @PostMapping("/print")
    @PreAuthorize("hasAuthority('POS_RECEIPT_PRINT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra impressão e devolve comprovante com autenticação/sequência")
    public ResponseEntity<ApiResponse<PosReceiptResponse>> print(@Valid @RequestBody ReceiptPrintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(posReceiptService.registerPrint(request)));
    }

    @PostMapping("/reprint")
    @PreAuthorize("hasAuthority('POS_RECEIPT_REPRINT')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra reimpressão auditada (2ª via)")
    public ResponseEntity<ApiResponse<PosReceiptResponse>> reprint(
            @Valid @RequestBody ReceiptReprintRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(posReceiptService.registerReprint(request)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('POS_RECEIPT_PRINT') or hasAuthority('POS_RECEIPT_REPRINT')")
    @Operation(summary = "Histórico de impressões / reimpressões")
    public ResponseEntity<PageResponse<ReceiptPrintLogResponse>> history(
            @RequestParam(required = false) UUID saleId,
            @RequestParam(required = false) UUID cashSessionId,
            @RequestParam(required = false) ReceiptPrintLog.PrintType printType,
            @RequestParam(required = false) Boolean isReprint,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                posReceiptService.history(saleId, cashSessionId, printType, isReprint, pageable)));
    }
}
