package br.com.systemcommerce.fiscal.inbound.controller;

import br.com.systemcommerce.fiscal.inbound.dto.IncomingFiscalDocumentResponse;
import br.com.systemcommerce.fiscal.inbound.dto.IncomingFiscalImportRequest;
import br.com.systemcommerce.fiscal.inbound.dto.IncomingFiscalLinkRequest;
import br.com.systemcommerce.fiscal.inbound.dto.IncomingManifestRequest;
import br.com.systemcommerce.fiscal.inbound.service.IncomingFiscalService;
import br.com.systemcommerce.shared.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/incoming")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Incoming", description = "Documentos fiscais de entrada (Prompt 142)")
public class IncomingFiscalController {

    private final IncomingFiscalService incomingFiscalService;

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('FISCAL_INCOMING_IMPORT')")
    public ResponseEntity<ApiResponse<IncomingFiscalDocumentResponse>> importXml(
            @Valid @RequestBody IncomingFiscalImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(incomingFiscalService.importXml(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_INCOMING_READ')")
    public ResponseEntity<ApiResponse<IncomingFiscalDocumentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(incomingFiscalService.getById(id)));
    }

    @PostMapping("/{id}/link")
    @PreAuthorize("hasAuthority('FISCAL_INCOMING_LINK')")
    public ResponseEntity<ApiResponse<IncomingFiscalDocumentResponse>> link(
            @PathVariable UUID id, @Valid @RequestBody IncomingFiscalLinkRequest request) {
        return ResponseEntity.ok(ApiResponse.of(incomingFiscalService.link(id, request)));
    }

    @PostMapping("/{id}/match-items")
    @PreAuthorize("hasAuthority('FISCAL_INCOMING_LINK')")
    public ResponseEntity<ApiResponse<IncomingFiscalDocumentResponse>> matchItems(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(incomingFiscalService.matchItems(id)));
    }

    @PostMapping("/{id}/manifest")
    @PreAuthorize("hasAuthority('FISCAL_INCOMING_MANIFEST')")
    public ResponseEntity<ApiResponse<Void>> manifest(
            @PathVariable UUID id, @Valid @RequestBody IncomingManifestRequest request) {
        incomingFiscalService.manifestDestinatario(id, request);
        return ResponseEntity.ok(ApiResponse.of(null));
    }
}
