package br.com.systemcommerce.pos.cash.controller;



import br.com.systemcommerce.pos.cash.dto.CashMovementReasonResponse;

import br.com.systemcommerce.pos.cash.dto.CashMovementResponse;

import br.com.systemcommerce.pos.cash.dto.CashMovementReverseRequest;

import br.com.systemcommerce.pos.cash.dto.CashMovementTypeSummaryResponse;

import br.com.systemcommerce.pos.cash.dto.CashPhysicalBalanceResponse;

import br.com.systemcommerce.pos.cash.dto.CashSupplyRequest;

import br.com.systemcommerce.pos.cash.dto.CashWithdrawalRequest;

import br.com.systemcommerce.pos.cash.entity.CashMovementReason;

import br.com.systemcommerce.pos.cash.service.CashMovementService;

import br.com.systemcommerce.shared.pagination.PageResponse;

import br.com.systemcommerce.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.util.List;

import java.util.UUID;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;

import org.springframework.data.domain.Sort;

import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestHeader;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.ResponseStatus;

import org.springframework.web.bind.annotation.RestController;



@RestController

@RequestMapping("/api/v1/cash-movements")

@RequiredArgsConstructor

@SecurityRequirement(name = "bearer-jwt")

@Tag(name = "Cash Movements", description = "Suprimento, sangria e movimentações internas do caixa")

public class CashMovementController {



    private final CashMovementService cashMovementService;



    @PostMapping("/supply")

    @PreAuthorize("hasAuthority('POS_CASH_SUPPLY')")

    @ResponseStatus(HttpStatus.CREATED)

    @Operation(summary = "Registra suprimento", description = "Idempotente via header Idempotency-Key")

    public ResponseEntity<ApiResponse<CashMovementResponse>> supply(

            @Valid @RequestBody CashSupplyRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)

                .body(ApiResponse.of(

                        cashMovementService.registerSupply(request.cashSessionId(), request, idempotencyKey)));

    }



    @PostMapping("/withdrawal")

    @PreAuthorize("hasAuthority('POS_CASH_WITHDRAWAL')")

    @ResponseStatus(HttpStatus.CREATED)

    @Operation(summary = "Registra sangria", description = "Idempotente via header Idempotency-Key")

    public ResponseEntity<ApiResponse<CashMovementResponse>> withdrawal(

            @Valid @RequestBody CashWithdrawalRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)

                .body(ApiResponse.of(cashMovementService.registerWithdrawal(

                        request.cashSessionId(), request, idempotencyKey)));

    }



    @GetMapping

    @PreAuthorize("hasAuthority('POS_CASH_MOVEMENT_READ')")

    @Operation(summary = "Lista movimentações da sessão")

    public ResponseEntity<PageResponse<CashMovementResponse>> list(

            @RequestParam UUID cashSessionId,

            @PageableDefault(size = 50, sort = "occurredAt", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(PageResponse.from(cashMovementService.list(cashSessionId, pageable)));

    }



    @GetMapping("/physical-balance")

    @PreAuthorize("hasAuthority('POS_CASH_MOVEMENT_READ')")

    @Operation(summary = "Consulta saldo físico esperado em dinheiro")

    public ResponseEntity<ApiResponse<CashPhysicalBalanceResponse>> physicalBalance(

            @RequestParam UUID cashSessionId) {

        return ResponseEntity.ok(ApiResponse.of(cashMovementService.physicalBalance(cashSessionId)));

    }



    @GetMapping("/summary-by-type")

    @PreAuthorize("hasAuthority('POS_CASH_MOVEMENT_READ')")

    @Operation(summary = "Resumo de movimentações por tipo")

    public ResponseEntity<ApiResponse<CashMovementTypeSummaryResponse>> summaryByType(

            @RequestParam UUID cashSessionId) {

        return ResponseEntity.ok(ApiResponse.of(cashMovementService.summaryByType(cashSessionId)));

    }



    @GetMapping("/reasons")

    @PreAuthorize("hasAuthority('POS_CASH_MOVEMENT_READ') or hasAuthority('POS_CASH_SUPPLY') or hasAuthority('POS_CASH_WITHDRAWAL')")

    @Operation(summary = "Lista motivos de suprimento/sangria")

    public ResponseEntity<ApiResponse<List<CashMovementReasonResponse>>> reasons(

            @RequestParam(required = false) CashMovementReason.AppliesTo appliesTo) {

        return ResponseEntity.ok(ApiResponse.of(cashMovementService.listReasons(appliesTo)));

    }



    @PostMapping("/{id}/reverse")

    @PreAuthorize("hasAuthority('POS_CASH_MOVEMENT_REVERSE')")

    @ResponseStatus(HttpStatus.CREATED)

    @Operation(summary = "Estorna movimentação via lançamento inverso")

    public ResponseEntity<ApiResponse<CashMovementResponse>> reverse(

            @PathVariable UUID id,

            @RequestBody(required = false) CashMovementReverseRequest request,

            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        return ResponseEntity.status(HttpStatus.CREATED)

                .body(ApiResponse.of(cashMovementService.reverse(

                        id, request != null ? request : new CashMovementReverseRequest(null), idempotencyKey)));

    }

}


