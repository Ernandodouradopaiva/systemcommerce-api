package br.com.systemcommerce.fiscal.certificate.controller;

import br.com.systemcommerce.fiscal.certificate.dto.CertificateActivateRequest;
import br.com.systemcommerce.fiscal.certificate.dto.CertificateRenewResponse;
import br.com.systemcommerce.fiscal.certificate.dto.CertificateTestSignatureResponse;
import br.com.systemcommerce.fiscal.certificate.dto.CertificateUsageLogResponse;
import br.com.systemcommerce.fiscal.certificate.dto.CertificateValidationHistoryResponse;
import br.com.systemcommerce.fiscal.certificate.dto.DigitalCertificateResponse;
import br.com.systemcommerce.fiscal.certificate.entity.DigitalCertificate;
import br.com.systemcommerce.fiscal.certificate.service.DigitalCertificateService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/fiscal/certificates")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Digital Certificates", description = "Certificados digitais fiscais (Prompt 123)")
public class DigitalCertificateController {

    private final DigitalCertificateService certificateService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_READ')")
    public ResponseEntity<PageResponse<DigitalCertificateResponse>> list(
            @RequestParam(required = false) UUID organizationId, @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(certificateService.list(organizationId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_READ')")
    public ResponseEntity<ApiResponse<DigitalCertificateResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(certificateService.getById(id)));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_UPLOAD')")
    public ResponseEntity<ApiResponse<DigitalCertificateResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart("password") String password,
            @RequestPart("organizationId") String organizationId,
            @RequestPart(value = "type", required = false) String type,
            @RequestPart(value = "holderName", required = false) String holderName) {
        DigitalCertificate.CertificateType resolvedType = DigitalCertificate.CertificateType.A1;
        if (type != null && !type.isBlank()) {
            resolvedType = DigitalCertificate.CertificateType.valueOf(type.trim().toUpperCase());
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(certificateService.upload(
                        file, password, UUID.fromString(organizationId.trim()), resolvedType, holderName)));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_TEST')")
    public ResponseEntity<ApiResponse<DigitalCertificateResponse>> validate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(certificateService.validate(id)));
    }

    @PostMapping("/{id}/test-signature")
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_TEST')")
    public ResponseEntity<ApiResponse<CertificateTestSignatureResponse>> testSignature(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(certificateService.testSignature(id)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_ACTIVATE')")
    public ResponseEntity<ApiResponse<DigitalCertificateResponse>> activate(
            @PathVariable UUID id, @Valid @RequestBody CertificateActivateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(certificateService.activate(id, request)));
    }

    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_REVOKE')")
    public ResponseEntity<ApiResponse<DigitalCertificateResponse>> revoke(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(certificateService.revoke(id)));
    }

    @PostMapping("/{id}/renew")
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_UPLOAD')")
    public ResponseEntity<ApiResponse<CertificateRenewResponse>> renew(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(certificateService.renew(id)));
    }

    @GetMapping("/{id}/validation-history")
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_READ')")
    public ResponseEntity<ApiResponse<List<CertificateValidationHistoryResponse>>> validationHistory(
            @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(certificateService.validationHistory(id)));
    }

    @GetMapping("/{id}/usage-logs")
    @PreAuthorize("hasAuthority('FISCAL_CERTIFICATE_READ')")
    public ResponseEntity<ApiResponse<List<CertificateUsageLogResponse>>> usageLogs(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(certificateService.usageLogs(id)));
    }
}
