package br.com.systemcommerce.pos.cancellation.controller;

import br.com.systemcommerce.pos.cancellation.dto.SaleReturnCreateRequest;
import br.com.systemcommerce.pos.cancellation.dto.SaleReturnResponse;
import br.com.systemcommerce.pos.cancellation.service.PosReturnService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/returns")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "POS Returns", description = "Devoluções futuras como documento próprio (não edita a venda original).")
public class PosReturnController {

    private final PosReturnService posReturnService;

    @PostMapping
    @PreAuthorize("hasAuthority('POS_RETURN_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra devolução futura")
    public ResponseEntity<ApiResponse<SaleReturnResponse>> register(
            @Valid @RequestBody SaleReturnCreateRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(posReturnService.register(request, idempotencyKey)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_RETURN_CREATE')")
    @Operation(summary = "Consulta devolução")
    public ResponseEntity<ApiResponse<SaleReturnResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(posReturnService.getById(id)));
    }
}
