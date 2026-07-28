package br.com.systemcommerce.customer.controller;

import br.com.systemcommerce.customer.dto.CustomerAddressRequest;
import br.com.systemcommerce.customer.dto.CustomerAddressResponse;
import br.com.systemcommerce.customer.dto.CustomerBlockRequest;
import br.com.systemcommerce.customer.dto.CustomerCommercialConditionRequest;
import br.com.systemcommerce.customer.dto.CustomerCommercialConditionResponse;
import br.com.systemcommerce.customer.dto.CustomerConsentRequest;
import br.com.systemcommerce.customer.dto.CustomerConsentResponse;
import br.com.systemcommerce.customer.dto.CustomerContactRequest;
import br.com.systemcommerce.customer.dto.CustomerContactResponse;
import br.com.systemcommerce.customer.dto.CustomerCreateRequest;
import br.com.systemcommerce.customer.dto.CustomerResponse;
import br.com.systemcommerce.customer.dto.CustomerStatusHistoryResponse;
import br.com.systemcommerce.customer.dto.CustomerUpdateRequest;
import br.com.systemcommerce.customer.entity.Customer;
import br.com.systemcommerce.customer.service.CustomerAddressService;
import br.com.systemcommerce.customer.service.CustomerCommercialConditionService;
import br.com.systemcommerce.customer.service.CustomerConsentService;
import br.com.systemcommerce.customer.service.CustomerContactService;
import br.com.systemcommerce.customerstore.dto.CustomerOriginStoreResponse;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipCreateRequest;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipNotesRequest;
import br.com.systemcommerce.customerstore.dto.CustomerStoreRelationshipResponse;
import br.com.systemcommerce.customerstore.entity.CustomerStoreRelationshipStatus;
import br.com.systemcommerce.customerstore.service.CustomerStoreRelationshipService;
import br.com.systemcommerce.customer.service.CustomerService;
import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Customers", description = "Cadastro de clientes (PF/PJ)")
public class CustomerController {

    private final CustomerService customerService;
    private final CustomerStoreRelationshipService customerStoreRelationshipService;
    private final CustomerAddressService customerAddressService;
    private final CustomerContactService customerContactService;
    private final CustomerCommercialConditionService customerCommercialConditionService;
    private final CustomerConsentService customerConsentService;

    @GetMapping
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(
            summary = "Lista clientes paginados",
            description = "Filtros: name, document, status (ACTIVE|INACTIVE|BLOCKED) e search (nome/documento/e-mail)")
    public ResponseEntity<PageResponse<CustomerResponse>> list(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String document,
            @RequestParam(required = false) Customer.CustomerStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(customerService.list(name, document, status, search, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Consulta cliente por ID")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<CustomerResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(customerService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria cliente PF ou PJ")
    public ResponseEntity<ApiResponse<CustomerResponse>> create(@Valid @RequestBody CustomerCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(customerService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Atualiza cliente")
    public ResponseEntity<ApiResponse<CustomerResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(customerService.update(id, request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE') or hasAuthority('CUSTOMER_STATUS_MANAGE')")
    @Operation(summary = "Ativa cliente")
    public ResponseEntity<ApiResponse<CustomerResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(customerService.activate(id)));
    }

    @PatchMapping("/{id}/inactivate")
    @PreAuthorize(
            "hasAuthority('CUSTOMER_DELETE') or hasAuthority('CUSTOMER_UPDATE') or hasAuthority('CUSTOMER_STATUS_MANAGE')")
    @Operation(summary = "Inativa cliente")
    public ResponseEntity<ApiResponse<CustomerResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(customerService.deactivate(id)));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasAuthority('CUSTOMER_STATUS_MANAGE')")
    @Operation(
            summary = "Bloqueia cliente",
            description = "Cliente BLOCKED não gera novo pedido/venda; orçamento segue allowQuoteWhenBlocked")
    public ResponseEntity<ApiResponse<CustomerResponse>> block(
            @PathVariable UUID id, @Valid @RequestBody CustomerBlockRequest request) {
        return ResponseEntity.ok(ApiResponse.of(customerService.block(id, request)));
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasAuthority('CUSTOMER_STATUS_MANAGE')")
    @Operation(summary = "Desbloqueia cliente (retorna para ACTIVE)")
    public ResponseEntity<ApiResponse<CustomerResponse>> unblock(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(customerService.unblock(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CUSTOMER_DELETE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Exclui cliente",
            description = "Exclusão lógica (inativação) se houver vendas vinculadas; caso contrário remove o registro")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/status-history")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Histórico de mudanças de status do cliente (imutável)")
    public ResponseEntity<PageResponse<CustomerStatusHistoryResponse>> statusHistory(
            @PathVariable UUID id, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(customerService.statusHistory(id, pageable)));
    }

    @GetMapping("/{id}/origin-store")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Consulta loja de origem do cadastro do cliente")
    public ResponseEntity<ApiResponse<CustomerOriginStoreResponse>> originStore(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(customerStoreRelationshipService.getOriginStore(id)));
    }

    @GetMapping("/{id}/store-relationships")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Lista vínculos locais do cliente por loja")
    public ResponseEntity<PageResponse<CustomerStoreRelationshipResponse>> storeRelationships(
            @PathVariable UUID id,
            @RequestParam(required = false) CustomerStoreRelationshipStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(
                customerStoreRelationshipService.listByCustomer(id, status, pageable)));
    }

    @GetMapping("/{id}/store-relationships/{storeId}")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Consulta vínculo local cliente-loja")
    public ResponseEntity<ApiResponse<CustomerStoreRelationshipResponse>> storeRelationship(
            @PathVariable UUID id, @PathVariable UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(customerStoreRelationshipService.getRelationship(id, storeId)));
    }

    @PostMapping("/{id}/store-relationships")
    @PreAuthorize("hasAuthority('CUSTOMER_CREATE') or hasAuthority('CUSTOMER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria vínculo local cliente-loja")
    public ResponseEntity<ApiResponse<CustomerStoreRelationshipResponse>> createStoreRelationship(
            @PathVariable UUID id, @Valid @RequestBody CustomerStoreRelationshipCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(customerStoreRelationshipService.create(id, request)));
    }

    @PatchMapping("/{id}/store-relationships/{storeId}/notes")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Atualiza notas locais do vínculo cliente-loja")
    public ResponseEntity<ApiResponse<CustomerStoreRelationshipResponse>> updateStoreRelationshipNotes(
            @PathVariable UUID id,
            @PathVariable UUID storeId,
            @Valid @RequestBody CustomerStoreRelationshipNotesRequest request) {
        return ResponseEntity.ok(
                ApiResponse.of(customerStoreRelationshipService.updateLocalNotes(id, storeId, request)));
    }

    // ===================== Endereços =====================

    @GetMapping("/{id}/addresses")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Lista endereços do cliente")
    public ResponseEntity<ApiResponse<List<CustomerAddressResponse>>> listAddresses(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(customerAddressService.list(id)));
    }

    @PostMapping("/{id}/addresses")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria endereço do cliente")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> createAddress(
            @PathVariable UUID id, @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(customerAddressService.create(id, request)));
    }

    @PutMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Atualiza endereço do cliente")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> updateAddress(
            @PathVariable UUID id, @PathVariable UUID addressId, @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.of(customerAddressService.update(id, addressId, request)));
    }

    @DeleteMapping("/{id}/addresses/{addressId}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa endereço do cliente (soft delete)")
    public ResponseEntity<Void> deleteAddress(@PathVariable UUID id, @PathVariable UUID addressId) {
        customerAddressService.delete(id, addressId);
        return ResponseEntity.noContent().build();
    }

    // ===================== Contatos =====================

    @GetMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Lista contatos do cliente")
    public ResponseEntity<ApiResponse<List<CustomerContactResponse>>> listContacts(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(customerContactService.list(id)));
    }

    @PostMapping("/{id}/contacts")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria contato do cliente")
    public ResponseEntity<ApiResponse<CustomerContactResponse>> createContact(
            @PathVariable UUID id, @Valid @RequestBody CustomerContactRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(customerContactService.create(id, request)));
    }

    @PutMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Atualiza contato do cliente")
    public ResponseEntity<ApiResponse<CustomerContactResponse>> updateContact(
            @PathVariable UUID id, @PathVariable UUID contactId, @Valid @RequestBody CustomerContactRequest request) {
        return ResponseEntity.ok(ApiResponse.of(customerContactService.update(id, contactId, request)));
    }

    @DeleteMapping("/{id}/contacts/{contactId}")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Inativa contato do cliente (soft delete)")
    public ResponseEntity<Void> deleteContact(@PathVariable UUID id, @PathVariable UUID contactId) {
        customerContactService.delete(id, contactId);
        return ResponseEntity.noContent().build();
    }

    // ===================== Condição comercial (organização) =====================

    @GetMapping("/{id}/commercial-condition")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Consulta condição comercial do cliente (nível organização)")
    public ResponseEntity<ApiResponse<CustomerCommercialConditionResponse>> getCommercialCondition(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(
                customerCommercialConditionService.get(id).orElse(null)));
    }

    @PutMapping("/{id}/commercial-condition")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Cria ou atualiza condição comercial do cliente")
    public ResponseEntity<ApiResponse<CustomerCommercialConditionResponse>> upsertCommercialCondition(
            @PathVariable UUID id, @Valid @RequestBody CustomerCommercialConditionRequest request) {
        return ResponseEntity.ok(ApiResponse.of(customerCommercialConditionService.upsert(id, request)));
    }

    // ===================== Consentimentos (LGPD) =====================

    @GetMapping("/{id}/consents")
    @PreAuthorize("hasAuthority('CUSTOMER_READ')")
    @Operation(summary = "Lista consentimentos do cliente (LGPD)")
    public ResponseEntity<ApiResponse<List<CustomerConsentResponse>>> listConsents(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(customerConsentService.list(id)));
    }

    @PostMapping("/{id}/consents")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra consentimento do cliente (LGPD)")
    public ResponseEntity<ApiResponse<CustomerConsentResponse>> createConsent(
            @PathVariable UUID id, @Valid @RequestBody CustomerConsentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(customerConsentService.create(id, request)));
    }

    @PatchMapping("/{id}/consents/{consentId}/revoke")
    @PreAuthorize("hasAuthority('CUSTOMER_UPDATE')")
    @Operation(summary = "Revoga consentimento do cliente (histórico preservado)")
    public ResponseEntity<ApiResponse<CustomerConsentResponse>> revokeConsent(
            @PathVariable UUID id, @PathVariable UUID consentId) {
        return ResponseEntity.ok(ApiResponse.of(customerConsentService.revoke(id, consentId)));
    }
}
