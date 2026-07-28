package br.com.systemcommerce.fiscal.taxation.controller;

import br.com.systemcommerce.fiscal.taxation.dto.FiscalTaxCatalogImportRequest;
import br.com.systemcommerce.fiscal.taxation.dto.FiscalTaxCatalogResponse;
import br.com.systemcommerce.fiscal.taxation.dto.FiscalTaxCatalogValidateResponse;
import br.com.systemcommerce.fiscal.taxation.dto.FiscalTaxCatalogVersionResponse;
import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog;
import br.com.systemcommerce.fiscal.taxation.service.FiscalTaxCatalogService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/v1/fiscal/tax-catalogs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Tax Catalogs", description = "Catálogos tributários versionados (Prompt 124)")
public class FiscalTaxCatalogController {

    private final FiscalTaxCatalogService catalogService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_TAX_CATALOG_READ')")
    public ResponseEntity<PageResponse<FiscalTaxCatalogResponse>> list(
            @RequestParam(required = false) FiscalTaxCatalog.CatalogType catalogType,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onlyValidOn,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(catalogService.list(catalogType, code, uf, onlyValidOn, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_TAX_CATALOG_READ')")
    public ResponseEntity<ApiResponse<FiscalTaxCatalogResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(catalogService.getById(id)));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('FISCAL_TAX_CATALOG_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalTaxCatalogVersionResponse>> importCatalog(
            @Valid @RequestBody FiscalTaxCatalogImportRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(catalogService.importCatalog(request)));
    }

    @GetMapping("/versions")
    @PreAuthorize("hasAuthority('FISCAL_TAX_CATALOG_READ')")
    public ResponseEntity<ApiResponse<List<FiscalTaxCatalogVersionResponse>>> versions(
            @RequestParam FiscalTaxCatalog.CatalogType catalogType) {
        return ResponseEntity.ok(ApiResponse.of(catalogService.versions(catalogType)));
    }

    @GetMapping("/validate")
    @PreAuthorize("hasAuthority('FISCAL_TAX_CATALOG_READ')")
    public ResponseEntity<ApiResponse<FiscalTaxCatalogValidateResponse>> validate(
            @RequestParam FiscalTaxCatalog.CatalogType catalogType,
            @RequestParam String code,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate on) {
        return ResponseEntity.ok(ApiResponse.of(catalogService.validateCode(catalogType, code, uf, on)));
    }
}
