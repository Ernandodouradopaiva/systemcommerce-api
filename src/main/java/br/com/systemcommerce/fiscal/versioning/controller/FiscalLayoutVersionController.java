package br.com.systemcommerce.fiscal.versioning.controller;

import br.com.systemcommerce.fiscal.versioning.entity.FiscalLayoutVersion;
import br.com.systemcommerce.fiscal.versioning.entity.FiscalTaxRuleSetVersion;
import br.com.systemcommerce.fiscal.versioning.service.FiscalLayoutVersionService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/fiscal/layout-versions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Layout Versions", description = "Versionamento / Reforma Tributária (Prompt 145)")
public class FiscalLayoutVersionController {

    private final FiscalLayoutVersionService layoutVersionService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_SCHEMA_READ') or hasAuthority('FISCAL_CONFIGURATION_MANAGE')")
    public ResponseEntity<ApiResponse<List<FiscalLayoutVersion>>> list() {
        return ResponseEntity.ok(ApiResponse.of(layoutVersionService.listVersions()));
    }

    @GetMapping("/resolve")
    @PreAuthorize("hasAuthority('FISCAL_SCHEMA_READ')")
    public ResponseEntity<ApiResponse<FiscalLayoutVersion>> resolve(
            @RequestParam String model,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate issueDate) {
        return ResponseEntity.ok(ApiResponse.of(layoutVersionService.resolveForEmission(model, issueDate)));
    }

    @PostMapping("/nt-updates")
    @PreAuthorize("hasAuthority('FISCAL_CONFIGURATION_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalLayoutVersion>> registerNt(@RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApiResponse.of(layoutVersionService.registerNtUpdate(
                body.get("ntCode"),
                body.get("description"),
                LocalDate.parse(body.getOrDefault("validFrom", LocalDate.now().toString())))));
    }

    @PostMapping("/rule-sets/{id}/lock")
    @PreAuthorize("hasAuthority('FISCAL_TAX_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalTaxRuleSetVersion>> lock(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(layoutVersionService.lockRuleSet(id)));
    }
}
