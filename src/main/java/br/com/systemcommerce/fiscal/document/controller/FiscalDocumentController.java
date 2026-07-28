package br.com.systemcommerce.fiscal.document.controller;

import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentAttachXmlRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentCreateRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentStatusTransitionRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentUpdateRequest;
import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentXmlResponse;
import br.com.systemcommerce.fiscal.document.service.FiscalDocumentService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/fiscal/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Documents", description = "Documentos fiscais eletrônicos base (Prompt 129)")
public class FiscalDocumentController {

    private final FiscalDocumentService documentService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_READ')")
    public ResponseEntity<PageResponse<FiscalDocumentResponse>> list(
            @RequestParam UUID organizationId, Pageable pageable) {
        return ResponseEntity.ok(documentService.list(organizationId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_READ')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(documentService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_CREATE')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> createDraft(
            @Valid @RequestBody FiscalDocumentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(documentService.createDraft(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_UPDATE')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody FiscalDocumentUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(documentService.update(id, request)));
    }

    @PostMapping("/{id}/transition-status")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_UPDATE')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> transitionStatus(
            @PathVariable UUID id, @Valid @RequestBody FiscalDocumentStatusTransitionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(documentService.transitionStatus(id, request)));
    }

    @PostMapping("/{id}/mark-authorized")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_UPDATE')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> markAuthorized(
            @PathVariable UUID id,
            @RequestParam(required = false) String sefazCstat,
            @RequestParam(required = false) String sefazXmotivo) {
        return ResponseEntity.ok(ApiResponse.of(documentService.markAuthorized(id, sefazCstat, sefazXmotivo)));
    }

    @PostMapping("/{id}/xml")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_UPDATE')")
    public ResponseEntity<ApiResponse<FiscalDocumentXmlResponse>> attachXml(
            @PathVariable UUID id, @Valid @RequestBody FiscalDocumentAttachXmlRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(documentService.attachXml(id, request)));
    }
}
