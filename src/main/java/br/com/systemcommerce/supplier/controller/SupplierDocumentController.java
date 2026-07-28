package br.com.systemcommerce.supplier.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.supplier.dto.SupplierDocumentRequest;
import br.com.systemcommerce.supplier.dto.SupplierDocumentResponse;
import br.com.systemcommerce.supplier.service.SupplierDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Metadados de documentos do fornecedor — sem upload binário (Prompt 57). */
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/documents")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Suppliers", description = "Documentos (metadados) do fornecedor (Prompt 57)")
public class SupplierDocumentController {

    private final SupplierDocumentService documentService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Lista documentos (metadados) do fornecedor")
    public ResponseEntity<ApiResponse<List<SupplierDocumentResponse>>> list(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(ApiResponse.of(documentService.list(supplierId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra documento (metadados) do fornecedor")
    public ResponseEntity<ApiResponse<SupplierDocumentResponse>> create(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierDocumentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(documentService.create(supplierId, request)));
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove documento (metadados) do fornecedor")
    public ResponseEntity<Void> delete(@PathVariable UUID supplierId, @PathVariable UUID documentId) {
        documentService.delete(supplierId, documentId);
        return ResponseEntity.noContent().build();
    }
}
