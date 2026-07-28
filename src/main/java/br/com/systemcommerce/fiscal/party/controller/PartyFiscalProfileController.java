package br.com.systemcommerce.fiscal.party.controller;

import br.com.systemcommerce.fiscal.party.PartyType;
import br.com.systemcommerce.fiscal.party.dto.PartyFiscalHistoryResponse;
import br.com.systemcommerce.fiscal.party.dto.PartyFiscalProfileCreateRequest;
import br.com.systemcommerce.fiscal.party.dto.PartyFiscalProfileResponse;
import br.com.systemcommerce.fiscal.party.dto.PartyFiscalProfileUpdateRequest;
import br.com.systemcommerce.fiscal.party.service.PartyFiscalProfileService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/fiscal/party-profiles")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Party Fiscal Profiles", description = "Perfis fiscais de clientes e fornecedores (Prompt 126)")
public class PartyFiscalProfileController {

    private final PartyFiscalProfileService profileService;

    @GetMapping("/by-party")
    @PreAuthorize("hasAuthority('FISCAL_PARTY_PROFILE_READ')")
    public ResponseEntity<ApiResponse<List<PartyFiscalProfileResponse>>> listByParty(
            @RequestParam PartyType partyType, @RequestParam UUID partyId) {
        return ResponseEntity.ok(ApiResponse.of(profileService.listByParty(partyType, partyId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_PARTY_PROFILE_READ')")
    public ResponseEntity<ApiResponse<PartyFiscalProfileResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(profileService.getById(id)));
    }

    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('FISCAL_PARTY_PROFILE_READ')")
    public ResponseEntity<ApiResponse<PartyFiscalProfileResponse>> resolve(
            @RequestParam PartyType partyType,
            @RequestParam UUID partyId,
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return ResponseEntity.ok(ApiResponse.of(profileService.resolve(partyType, partyId, organizationId, storeId, on)));
    }

    @GetMapping("/by-party/history")
    @PreAuthorize("hasAuthority('FISCAL_PARTY_PROFILE_READ')")
    public ResponseEntity<ApiResponse<List<PartyFiscalHistoryResponse>>> history(
            @RequestParam PartyType partyType, @RequestParam UUID partyId) {
        return ResponseEntity.ok(ApiResponse.of(profileService.history(partyType, partyId)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_PARTY_PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<PartyFiscalProfileResponse>> create(
            @Valid @RequestBody PartyFiscalProfileCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(profileService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_PARTY_PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<PartyFiscalProfileResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PartyFiscalProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(profileService.update(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FISCAL_PARTY_PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<PartyFiscalProfileResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(profileService.activate(id)));
    }

    @PostMapping("/{id}/inactivate")
    @PreAuthorize("hasAuthority('FISCAL_PARTY_PROFILE_UPDATE')")
    public ResponseEntity<ApiResponse<PartyFiscalProfileResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(profileService.inactivate(id)));
    }
}
