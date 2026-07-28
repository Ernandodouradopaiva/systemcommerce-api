package br.com.systemcommerce.fiscal.numbering.controller;

import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberGapResponse;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberReservationResponse;
import br.com.systemcommerce.fiscal.numbering.dto.FiscalNumberVoidingResponse;
import br.com.systemcommerce.fiscal.numbering.dto.ReserveNumberRequest;
import br.com.systemcommerce.fiscal.numbering.dto.VoidingRequestCreateDto;
import br.com.systemcommerce.fiscal.numbering.entity.FiscalNumberGap.GapStatus;
import br.com.systemcommerce.fiscal.numbering.service.FiscalNumberingService;
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
@RequestMapping("/api/v1/fiscal/numbering")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Numbering", description = "Numeração, reservas e inutilização (Prompt 130)")
public class FiscalNumberingController {

    private final FiscalNumberingService numberingService;

    @PostMapping("/reserve")
    @PreAuthorize("hasAuthority('FISCAL_NUMBERING_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalNumberReservationResponse>> reserve(
            @Valid @RequestBody ReserveNumberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(numberingService.reserveNext(
                        request.establishmentId(),
                        request.model(),
                        request.series(),
                        request.environment(),
                        request.documentId(),
                        request.idempotencyKey())));
    }

    @PostMapping("/reservations/{id}/consume")
    @PreAuthorize("hasAuthority('FISCAL_NUMBERING_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalNumberReservationResponse>> consume(
            @PathVariable UUID id, @RequestParam UUID documentId) {
        return ResponseEntity.ok(ApiResponse.of(numberingService.consumeReservation(id, documentId)));
    }

    @PostMapping("/reservations/{id}/release")
    @PreAuthorize("hasAuthority('FISCAL_NUMBERING_MANAGE')")
    public ResponseEntity<ApiResponse<FiscalNumberReservationResponse>> release(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(numberingService.releaseReservation(id)));
    }

    @GetMapping("/sequences/{sequenceId}/gaps")
    @PreAuthorize("hasAuthority('FISCAL_NUMBERING_READ')")
    public ResponseEntity<List<FiscalNumberGapResponse>> listGaps(
            @PathVariable UUID sequenceId, @RequestParam(required = false) GapStatus status) {
        return ResponseEntity.ok(numberingService.listGaps(sequenceId, status));
    }

    @PostMapping("/voiding")
    @PreAuthorize("hasAuthority('FISCAL_INUTILIZE')")
    public ResponseEntity<ApiResponse<FiscalNumberVoidingResponse>> createVoiding(
            @Valid @RequestBody VoidingRequestCreateDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(numberingService.createVoidingRequest(request)));
    }

    @PostMapping("/voiding/{id}/transmit")
    @PreAuthorize("hasAuthority('FISCAL_INUTILIZE')")
    public ResponseEntity<ApiResponse<FiscalNumberVoidingResponse>> transmitVoiding(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(numberingService.transmitVoiding(id)));
    }
}
