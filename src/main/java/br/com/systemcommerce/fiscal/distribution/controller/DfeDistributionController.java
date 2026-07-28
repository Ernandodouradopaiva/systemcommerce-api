package br.com.systemcommerce.fiscal.distribution.controller;

import br.com.systemcommerce.fiscal.distribution.dto.DfeDistributionDocumentResponse;
import br.com.systemcommerce.fiscal.distribution.dto.DfeDistributionQueryResponse;
import br.com.systemcommerce.fiscal.distribution.dto.RecipientManifestRequest;
import br.com.systemcommerce.fiscal.distribution.dto.RecipientManifestationResponse;
import br.com.systemcommerce.fiscal.distribution.service.DfeDistributionService;
import br.com.systemcommerce.fiscal.distribution.service.RecipientManifestationService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/fiscal/dfe-distribution")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal DFe Distribution", description = "Distribuição DFe e manifestação (Prompt 143)")
public class DfeDistributionController {

    private final DfeDistributionService distributionService;
    private final RecipientManifestationService manifestationService;

    @PostMapping("/establishments/{establishmentId}/query")
    @PreAuthorize("hasAuthority('FISCAL_DFE_DISTRIBUTION_QUERY')")
    public ResponseEntity<ApiResponse<DfeDistributionQueryResponse>> query(
            @PathVariable UUID establishmentId) {
        return ResponseEntity.ok(ApiResponse.of(distributionService.queryIncremental(establishmentId)));
    }

    @GetMapping("/establishments/{establishmentId}/documents")
    @PreAuthorize("hasAuthority('FISCAL_DFE_DISTRIBUTION_READ')")
    public ResponseEntity<ApiResponse<List<DfeDistributionDocumentResponse>>> listDocuments(
            @PathVariable UUID establishmentId,
            @RequestParam(defaultValue = "false") boolean unrecognizedOnly) {
        return ResponseEntity.ok(
                ApiResponse.of(distributionService.listDocuments(establishmentId, unrecognizedOnly)));
    }

    @GetMapping("/documents/{id}")
    @PreAuthorize("hasAuthority('FISCAL_DFE_DISTRIBUTION_READ')")
    public ResponseEntity<ApiResponse<DfeDistributionDocumentResponse>> getDocument(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(distributionService.getDocument(id)));
    }

    @PostMapping("/documents/{id}/link-incoming")
    @PreAuthorize("hasAuthority('FISCAL_DFE_DISTRIBUTION_QUERY')")
    public ResponseEntity<ApiResponse<DfeDistributionDocumentResponse>> linkIncoming(
            @PathVariable UUID id, @RequestBody Map<String, UUID> body) {
        return ResponseEntity.ok(
                ApiResponse.of(distributionService.linkIncoming(id, body.get("incomingDocumentId"))));
    }

    @PostMapping("/documents/{id}/flag-suspicious")
    @PreAuthorize("hasAuthority('FISCAL_DFE_DISTRIBUTION_QUERY')")
    public ResponseEntity<ApiResponse<DfeDistributionDocumentResponse>> flagSuspicious(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                ApiResponse.of(distributionService.flagSuspicious(id, body.getOrDefault("reason", "Suspeito"))));
    }

    @PostMapping("/manifestations")
    @PreAuthorize("hasAuthority('FISCAL_MANIFESTATION_MANAGE')")
    public ResponseEntity<ApiResponse<RecipientManifestationResponse>> requestManifest(
            @Valid @RequestBody RecipientManifestRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(manifestationService.request(request)));
    }

    @PostMapping("/manifestations/{id}/transmit")
    @PreAuthorize("hasAuthority('FISCAL_MANIFESTATION_MANAGE')")
    public ResponseEntity<ApiResponse<RecipientManifestationResponse>> transmit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(manifestationService.transmit(id)));
    }

    @GetMapping("/manifestations/by-access-key/{accessKey}")
    @PreAuthorize("hasAuthority('FISCAL_MANIFESTATION_READ')")
    public ResponseEntity<ApiResponse<List<RecipientManifestationResponse>>> history(
            @PathVariable String accessKey) {
        return ResponseEntity.ok(ApiResponse.of(manifestationService.historyByAccessKey(accessKey)));
    }
}
