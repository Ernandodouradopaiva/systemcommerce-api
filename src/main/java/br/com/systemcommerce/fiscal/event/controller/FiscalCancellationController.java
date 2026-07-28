package br.com.systemcommerce.fiscal.event.controller;

import br.com.systemcommerce.fiscal.event.dto.CancellationRequestCreateDto;
import br.com.systemcommerce.fiscal.event.dto.CancellationRequestResponse;
import br.com.systemcommerce.fiscal.event.service.FiscalCancellationService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/fiscal/cancellations")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Cancellations", description = "Cancelamento de DFe (Prompt 137)")
public class FiscalCancellationController {

    private final FiscalCancellationService cancellationService;

    @PostMapping
    @PreAuthorize("hasAuthority('FISCAL_CANCEL_REQUEST')")
    public ResponseEntity<ApiResponse<CancellationRequestResponse>> request(
            @Valid @RequestBody CancellationRequestCreateDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(cancellationService.requestCancellation(dto)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('FISCAL_CANCEL_APPROVE')")
    public ResponseEntity<ApiResponse<CancellationRequestResponse>> approve(
            @PathVariable UUID id,
            @RequestParam UUID approverUserId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(ApiResponse.of(cancellationService.approve(id, approverUserId, approved, notes)));
    }

    @PostMapping("/{id}/transmit")
    @PreAuthorize("hasAuthority('FISCAL_CANCEL_REQUEST')")
    public ResponseEntity<ApiResponse<CancellationRequestResponse>> transmit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(cancellationService.transmit(id)));
    }
}
