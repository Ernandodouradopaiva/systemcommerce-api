package br.com.systemcommerce.fiscal.operation.controller;

import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationCreateRequest;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationResolvedResponse;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationResponse;
import br.com.systemcommerce.fiscal.operation.dto.FiscalOperationUpdateRequest;
import br.com.systemcommerce.fiscal.operation.service.FiscalOperationService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/operations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Operations", description = "Operações fiscais e CFOP (Prompt 128)")
public class FiscalOperationController {

    private final FiscalOperationService operationService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_OPERATION_READ')")
    public ResponseEntity<ApiResponse<List<FiscalOperationResponse>>> list(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(operationService.list(organizationId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_OPERATION_READ')")
    public ResponseEntity<ApiResponse<FiscalOperationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(operationService.getById(id)));
    }

    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('FISCAL_OPERATION_READ')")
    public ResponseEntity<ApiResponse<FiscalOperationResolvedResponse>> resolve(
            @RequestParam String operationCode,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) String originUf,
            @RequestParam(required = false) String destUf,
            @RequestParam(required = false) Boolean finalConsumer,
            @RequestParam(required = false) String model) {
        return ResponseEntity.ok(ApiResponse.of(operationService.resolve(
                operationCode, organizationId, storeId, originUf, destUf, finalConsumer, model)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_OPERATION_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalOperationResponse>> create(
            @Valid @RequestBody FiscalOperationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(operationService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_OPERATION_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalOperationResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody FiscalOperationUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(operationService.update(id, request)));
    }
}
