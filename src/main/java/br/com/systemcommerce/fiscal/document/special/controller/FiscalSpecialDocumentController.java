package br.com.systemcommerce.fiscal.document.special.controller;

import br.com.systemcommerce.fiscal.document.dto.FiscalDocumentResponse;
import br.com.systemcommerce.fiscal.document.special.dto.SpecialDocumentEmitRequest;
import br.com.systemcommerce.fiscal.document.special.service.FiscalSpecialDocumentService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/special-documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Special Documents", description = "NF-e especiais (Prompt 140)")
public class FiscalSpecialDocumentController {

    private final FiscalSpecialDocumentService specialDocumentService;

    @PostMapping("/complementary")
    @PreAuthorize("hasAuthority('FISCAL_SPECIAL_EMIT')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> emitComplementary(
            @Valid @RequestBody SpecialDocumentEmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(specialDocumentService.emitComplementary(request)));
    }

    @PostMapping("/adjustment")
    @PreAuthorize("hasAuthority('FISCAL_SPECIAL_EMIT')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> emitAdjustment(
            @Valid @RequestBody SpecialDocumentEmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(specialDocumentService.emitAdjustment(request)));
    }

    @PostMapping("/remittance")
    @PreAuthorize("hasAuthority('FISCAL_SPECIAL_EMIT')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> emitRemittance(
            @Valid @RequestBody SpecialDocumentEmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(specialDocumentService.emitRemittance(request)));
    }

    @PostMapping("/return")
    @PreAuthorize("hasAuthority('FISCAL_SPECIAL_EMIT')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> emitReturn(
            @Valid @RequestBody SpecialDocumentEmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(specialDocumentService.emitReturn(request)));
    }

    @PostMapping("/own-entry")
    @PreAuthorize("hasAuthority('FISCAL_SPECIAL_EMIT')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> emitOwnEntry(
            @Valid @RequestBody SpecialDocumentEmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(specialDocumentService.emitOwnEntry(request)));
    }

    @PostMapping("/annulment")
    @PreAuthorize("hasAuthority('FISCAL_SPECIAL_EMIT')")
    public ResponseEntity<ApiResponse<FiscalDocumentResponse>> emitAnnulment(
            @Valid @RequestBody SpecialDocumentEmitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(specialDocumentService.emitAnnulmentDocument(request)));
    }
}
