package br.com.systemcommerce.pos.terminal.controller;

import br.com.systemcommerce.pos.terminal.dto.PosTerminalCreateRequest;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalLinkWarehouseRequest;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalResponse;
import br.com.systemcommerce.pos.terminal.dto.PosTerminalUpdateRequest;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.pos.terminal.service.PosTerminalService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos-terminals")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "POS Terminals", description = "Cadastro de terminais de PDV")
public class PosTerminalController {

    private final PosTerminalService posTerminalService;

    @GetMapping
    @PreAuthorize("hasAuthority('POS_TERMINAL_READ') or hasAuthority('POS_TERMINAL_MANAGE')")
    @Operation(summary = "Lista terminais paginados")
    public ResponseEntity<PageResponse<PosTerminalResponse>> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID warehouseId,
            @RequestParam(required = false) PosTerminal.TerminalStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(posTerminalService.list(storeId, warehouseId, status, search, pageable)));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('POS_TERMINAL_READ') or hasAuthority('POS_TERMINAL_MANAGE')")
    @Operation(
            summary = "Lista terminais disponíveis para abertura de caixa",
            description = "Ativos, com loja ativa e depósito ativo autorizado para venda")
    public ResponseEntity<PageResponse<PosTerminalResponse>> listAvailable(
            @RequestParam(required = false) UUID storeId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(posTerminalService.listAvailable(storeId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_TERMINAL_READ') or hasAuthority('POS_TERMINAL_MANAGE')")
    @Operation(summary = "Consulta terminal por ID")
    public ResponseEntity<ApiResponse<PosTerminalResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(posTerminalService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('POS_TERMINAL_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra terminal de PDV")
    public ResponseEntity<ApiResponse<PosTerminalResponse>> create(
            @Valid @RequestBody PosTerminalCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(posTerminalService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('POS_TERMINAL_MANAGE')")
    @Operation(summary = "Edita terminal de PDV")
    public ResponseEntity<ApiResponse<PosTerminalResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PosTerminalUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(posTerminalService.update(id, request)));
    }

    @PatchMapping("/{id}/warehouse")
    @PreAuthorize("hasAuthority('POS_TERMINAL_MANAGE')")
    @Operation(summary = "Vincula terminal a um depósito da mesma loja")
    public ResponseEntity<ApiResponse<PosTerminalResponse>> linkWarehouse(
            @PathVariable UUID id, @Valid @RequestBody PosTerminalLinkWarehouseRequest request) {
        return ResponseEntity.ok(ApiResponse.of(posTerminalService.linkWarehouse(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('POS_TERMINAL_MANAGE')")
    @Operation(summary = "Ativa terminal")
    public ResponseEntity<ApiResponse<PosTerminalResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(posTerminalService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('POS_TERMINAL_MANAGE')")
    @Operation(summary = "Inativa terminal (não poderá abrir caixa)")
    public ResponseEntity<ApiResponse<PosTerminalResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(posTerminalService.inactivate(id)));
    }
}
