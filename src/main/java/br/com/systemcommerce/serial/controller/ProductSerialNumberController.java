package br.com.systemcommerce.serial.controller;

import br.com.systemcommerce.serial.dto.ProductSerialNumberResponse;
import br.com.systemcommerce.serial.dto.ProductSerialRegisterRequest;
import br.com.systemcommerce.serial.dto.ProductSerialStatusChangeRequest;
import br.com.systemcommerce.serial.dto.SerialNumberStatusHistoryResponse;
import br.com.systemcommerce.serial.entity.ProductSerialStatus;
import br.com.systemcommerce.serial.service.ProductSerialNumberService;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/serial-numbers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Serial Numbers", description = "Controle por número de série (Prompt 77)")
public class ProductSerialNumberController {

    private final ProductSerialNumberService productSerialNumberService;

    @GetMapping
    @PreAuthorize("hasAuthority('SERIAL_READ')")
    @Operation(summary = "Lista números de série")
    public ResponseEntity<PageResponse<ProductSerialNumberResponse>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) ProductSerialStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                productSerialNumberService.list(organizationId, productId, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SERIAL_READ')")
    @Operation(summary = "Consulta série por ID")
    public ResponseEntity<ApiResponse<ProductSerialNumberResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productSerialNumberService.getById(id)));
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('SERIAL_READ')")
    @Operation(summary = "Histórico de status da série")
    public ResponseEntity<ApiResponse<List<SerialNumberStatusHistoryResponse>>> statusHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(productSerialNumberService.statusHistory(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SERIAL_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra série no recebimento")
    public ResponseEntity<ApiResponse<ProductSerialNumberResponse>> register(
            @Valid @RequestBody ProductSerialRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(productSerialNumberService.registerOnReceipt(request)));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('SERIAL_MANAGE')")
    @Operation(summary = "Altera status da série")
    public ResponseEntity<ApiResponse<ProductSerialNumberResponse>> changeStatus(
            @PathVariable UUID id, @Valid @RequestBody ProductSerialStatusChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.of(productSerialNumberService.changeStatus(id, request)));
    }
}
