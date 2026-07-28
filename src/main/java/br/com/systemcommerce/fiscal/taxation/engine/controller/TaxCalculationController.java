package br.com.systemcommerce.fiscal.taxation.engine.controller;

import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxCalculationResponse;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxRuleCreateRequest;
import br.com.systemcommerce.fiscal.taxation.engine.dto.TaxRuleResponse;
import br.com.systemcommerce.fiscal.taxation.engine.service.TaxEngineService;
import br.com.systemcommerce.fiscal.taxation.engine.service.TaxRuleService;
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
@RequestMapping("/api/v1/fiscal/tax-calculations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Tax Calculations", description = "Motor de cálculo tributário (Prompt 127)")
public class TaxCalculationController {

    private final TaxEngineService taxEngineService;

    @PostMapping("/simulate")
    @PreAuthorize("hasAuthority('FISCAL_TAX_ENGINE_CALCULATE')")
    public ResponseEntity<ApiResponse<TaxCalculationResponse>> simulate(
            @Valid @RequestBody TaxCalculationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(taxEngineService.simulate(request)));
    }

    @PostMapping("/calculate")
    @PreAuthorize("hasAuthority('FISCAL_TAX_ENGINE_CALCULATE')")
    public ResponseEntity<ApiResponse<TaxCalculationResponse>> calculate(
            @Valid @RequestBody TaxCalculationRequest request) {
        TaxCalculationRequest effective =
                new TaxCalculationRequest(
                        request.organizationId(),
                        request.storeId(),
                        request.establishmentId(),
                        false,
                        request.issuedOn(),
                        request.operationCode(),
                        request.channel(),
                        request.originUf(),
                        request.destinationUf(),
                        request.destinationIbge(),
                        request.purpose(),
                        request.finalConsumer(),
                        request.taxpayerIndicator(),
                        request.originDocumentType(),
                        request.originDocumentId(),
                        request.items());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(taxEngineService.calculate(effective, false)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_TAX_ENGINE_READ')")
    public ResponseEntity<ApiResponse<TaxCalculationResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(taxEngineService.getById(id)));
    }
}

@RestController
@RequestMapping("/api/v1/fiscal/tax-rules")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Tax Rules", description = "Regras tributárias (Prompt 127)")
class TaxRuleController {

    private final TaxRuleService taxRuleService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_TAX_ENGINE_READ')")
    public ResponseEntity<ApiResponse<List<TaxRuleResponse>>> list(@RequestParam UUID organizationId) {
        return ResponseEntity.ok(ApiResponse.of(taxRuleService.list(organizationId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FISCAL_TAX_ENGINE_READ')")
    public ResponseEntity<ApiResponse<TaxRuleResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(taxRuleService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_TAX_RULE_MANAGE')")
    public ResponseEntity<ApiResponse<TaxRuleResponse>> create(@Valid @RequestBody TaxRuleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(taxRuleService.create(request)));
    }
}
