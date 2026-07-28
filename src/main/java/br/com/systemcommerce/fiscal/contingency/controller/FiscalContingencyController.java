package br.com.systemcommerce.fiscal.contingency.controller;

import br.com.systemcommerce.fiscal.contingency.dto.ContingencyActivateRequest;
import br.com.systemcommerce.fiscal.contingency.dto.ContingencyDocumentResponse;
import br.com.systemcommerce.fiscal.contingency.dto.FiscalContingencyResponse;
import br.com.systemcommerce.fiscal.contingency.service.FiscalContingencyService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/contingencies")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Contingency", description = "Contingência fiscal (Prompt 139)")
public class FiscalContingencyController {

    private final FiscalContingencyService contingencyService;

    @PostMapping("/activate")
    @PreAuthorize("hasAuthority('FISCAL_CONTINGENCY_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalContingencyResponse>> activate(
            @Valid @RequestBody ContingencyActivateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(contingencyService.activate(request)));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('FISCAL_CONTINGENCY_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalContingencyResponse>> close(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(contingencyService.close(id)));
    }

    @PostMapping("/{id}/documents/{documentId}")
    @PreAuthorize("hasAuthority('FISCAL_CONTINGENCY_MANAGE')")
    public ResponseEntity<ApiResponse<ContingencyDocumentResponse>> registerDocument(
            @PathVariable UUID id, @PathVariable UUID documentId) {
        return ResponseEntity.ok(ApiResponse.of(contingencyService.registerDocument(id, documentId)));
    }

    @PostMapping("/{id}/retransmit-pending")
    @PreAuthorize("hasAuthority('FISCAL_CONTINGENCY_MANAGE')")
    public ResponseEntity<ApiResponse<List<ContingencyDocumentResponse>>> retransmitPending(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(contingencyService.retransmitPending(id)));
    }

    @GetMapping("/pending-documents")
    @PreAuthorize("hasAuthority('FISCAL_CONTINGENCY_READ')")
    public ResponseEntity<ApiResponse<List<ContingencyDocumentResponse>>> listPending() {
        return ResponseEntity.ok(ApiResponse.of(contingencyService.listPending()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('FISCAL_CONTINGENCY_READ')")
    public ResponseEntity<ApiResponse<FiscalContingencyResponse>> getActive(
            @RequestParam UUID establishmentId, @RequestParam String model) {
        return ResponseEntity.ok(ApiResponse.of(contingencyService.getActive(establishmentId, model)));
    }
}
