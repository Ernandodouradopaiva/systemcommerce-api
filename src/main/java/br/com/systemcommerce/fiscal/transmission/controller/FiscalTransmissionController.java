package br.com.systemcommerce.fiscal.transmission.controller;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.fiscal.transmission.dto.ServiceStatusResult;
import br.com.systemcommerce.fiscal.transmission.entity.FiscalTransmission;
import br.com.systemcommerce.fiscal.transmission.service.FiscalTransmissionService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/transmissions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Transmissions", description = "Transmissões SEFAZ (Prompt 133)")
public class FiscalTransmissionController {

    private final FiscalTransmissionService transmissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('FISCAL_TRANSMISSION_READ')")
    public ResponseEntity<List<FiscalTransmission>> listByDocument(@RequestParam UUID documentId) {
        return ResponseEntity.ok(transmissionService.listByDocument(documentId));
    }

    @PostMapping("/status-servico")
    @PreAuthorize("hasAuthority('FISCAL_TRANSMISSION_EXECUTE')")
    public ResponseEntity<ApiResponse<ServiceStatusResult>> statusServico(
            @RequestParam String uf,
            @RequestParam String model,
            @RequestParam FiscalEstablishment.FiscalEnvironment environment) {
        return ResponseEntity.ok(ApiResponse.of(transmissionService.statusServico(uf, model, environment)));
    }
}
