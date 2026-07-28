package br.com.systemcommerce.fiscal.validation.controller;

import br.com.systemcommerce.fiscal.validation.dto.FiscalSchemaCreateRequest;
import br.com.systemcommerce.fiscal.validation.dto.FiscalSchemaResponse;
import br.com.systemcommerce.fiscal.validation.service.FiscalSchemaService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
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
@RequestMapping("/api/v1/fiscal/schemas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Schemas", description = "Schemas XSD e leiautes (Prompt 131)")
public class FiscalSchemaController {

    private final FiscalSchemaService schemaService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_SCHEMA_READ')")
    public ResponseEntity<List<FiscalSchemaResponse>> list(@RequestParam String model) {
        return ResponseEntity.ok(schemaService.listByModel(model));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_SCHEMA_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalSchemaResponse>> create(@Valid @RequestBody FiscalSchemaCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(schemaService.create(request)));
    }

    @PostMapping("/{id}/import")
    @PreAuthorize("hasAuthority('FISCAL_SCHEMA_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalSchemaResponse>> importSchema(
            @PathVariable UUID id, @RequestParam(required = false) String source, @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(ApiResponse.of(schemaService.importSchema(id, source, notes)));
    }
}
