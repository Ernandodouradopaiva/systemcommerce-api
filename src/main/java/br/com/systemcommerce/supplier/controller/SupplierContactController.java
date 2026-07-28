package br.com.systemcommerce.supplier.controller;

import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.supplier.dto.SupplierContactRequest;
import br.com.systemcommerce.supplier.dto.SupplierContactResponse;
import br.com.systemcommerce.supplier.service.SupplierContactService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/contacts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Suppliers", description = "Contatos do fornecedor (Prompt 57)")
public class SupplierContactController {

    private final SupplierContactService contactService;

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    @Operation(summary = "Lista contatos do fornecedor")
    public ResponseEntity<ApiResponse<List<SupplierContactResponse>>> list(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(ApiResponse.of(contactService.list(supplierId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria contato do fornecedor")
    public ResponseEntity<ApiResponse<SupplierContactResponse>> create(
            @PathVariable UUID supplierId, @Valid @RequestBody SupplierContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(contactService.create(supplierId, request)));
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @Operation(summary = "Atualiza contato do fornecedor")
    public ResponseEntity<ApiResponse<SupplierContactResponse>> update(
            @PathVariable UUID supplierId,
            @PathVariable UUID contactId,
            @Valid @RequestBody SupplierContactRequest request) {
        return ResponseEntity.ok(ApiResponse.of(contactService.update(supplierId, contactId, request)));
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove contato do fornecedor")
    public ResponseEntity<Void> delete(@PathVariable UUID supplierId, @PathVariable UUID contactId) {
        contactService.delete(supplierId, contactId);
        return ResponseEntity.noContent().build();
    }
}
