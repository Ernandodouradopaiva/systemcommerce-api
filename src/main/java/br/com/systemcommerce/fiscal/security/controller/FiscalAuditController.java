package br.com.systemcommerce.fiscal.security.controller;

import br.com.systemcommerce.fiscal.security.entity.FiscalAuditEvent;
import br.com.systemcommerce.fiscal.security.service.FiscalAuditService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/audit-events")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Audit", description = "Auditoria fiscal (Prompt 147)")
public class FiscalAuditController {

    private final FiscalAuditService fiscalAuditService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_MONITOR_READ') or hasAuthority('FISCAL_GLOBAL_ACCESS')")
    public ResponseEntity<ApiResponse<Page<FiscalAuditEvent>>> query(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID documentId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.of(fiscalAuditService.query(organizationId, action, documentId, pageable)));
    }
}
