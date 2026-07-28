package br.com.systemcommerce.fiscal.monitoring.controller;

import br.com.systemcommerce.fiscal.document.FiscalDocumentStatus;
import br.com.systemcommerce.fiscal.monitoring.dto.FiscalMonitorDocumentResponse;
import br.com.systemcommerce.fiscal.monitoring.dto.FiscalMonitorFilter;
import br.com.systemcommerce.fiscal.monitoring.entity.FiscalDeadLetterItem;
import br.com.systemcommerce.fiscal.monitoring.entity.FiscalEmissionQueueItem;
import br.com.systemcommerce.fiscal.monitoring.service.FiscalMonitorService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/v1/fiscal/monitor")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Fiscal Monitor", description = "Monitor e fila de emissão (Prompt 144)")
public class FiscalMonitorController {

    private final FiscalMonitorService monitorService;

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('FISCAL_MONITOR_READ')")
    public ResponseEntity<ApiResponse<Page<FiscalMonitorDocumentResponse>>> search(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID establishmentId,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String series,
            @RequestParam(required = false) Long number,
            @RequestParam(required = false) String accessKey,
            @RequestParam(required = false) FiscalDocumentStatus status,
            @RequestParam(required = false) String sefazCstat,
            @RequestParam(required = false) String environment,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant periodTo,
            Pageable pageable) {
        FiscalMonitorFilter filter = new FiscalMonitorFilter(
                organizationId,
                storeId,
                establishmentId,
                model,
                series,
                number,
                accessKey,
                periodFrom,
                periodTo,
                status,
                sefazCstat,
                environment,
                null);
        return ResponseEntity.ok(ApiResponse.of(monitorService.search(filter, pageable)));
    }

    @GetMapping("/status-counts")
    @PreAuthorize("hasAuthority('FISCAL_MONITOR_READ')")
    public ResponseEntity<ApiResponse<Map<FiscalDocumentStatus, Long>>> statusCounts(
            @RequestParam UUID organizationId, @RequestParam(required = false) UUID storeId) {
        return ResponseEntity.ok(ApiResponse.of(monitorService.statusCounts(organizationId, storeId)));
    }

    @PostMapping("/documents/{id}/consult-status")
    @PreAuthorize("hasAuthority('FISCAL_MONITOR_READ')")
    public ResponseEntity<ApiResponse<FiscalMonitorDocumentResponse>> consult(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(monitorService.consultStatus(id)));
    }

    @PostMapping("/documents/{id}/retransmit")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_TRANSMIT')")
    public ResponseEntity<ApiResponse<FiscalEmissionQueueItem>> retransmit(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(monitorService.retransmitSafely(id)));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('FISCAL_MONITOR_READ')")
    public ResponseEntity<ApiResponse<List<FiscalEmissionQueueItem>>> queue() {
        return ResponseEntity.ok(ApiResponse.of(monitorService.listQueue()));
    }

    @GetMapping("/dead-letters")
    @PreAuthorize("hasAuthority('FISCAL_MONITOR_READ')")
    public ResponseEntity<ApiResponse<List<FiscalDeadLetterItem>>> deadLetters() {
        return ResponseEntity.ok(ApiResponse.of(monitorService.listDeadLetters()));
    }

    @PostMapping("/dead-letters/{id}/resolve")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_TRANSMIT')")
    public ResponseEntity<ApiResponse<FiscalDeadLetterItem>> resolve(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(monitorService.resolveDeadLetter(id)));
    }

    @PostMapping("/queue/{id}/dead-letter")
    @PreAuthorize("hasAuthority('FISCAL_DOCUMENT_TRANSMIT')")
    public ResponseEntity<ApiResponse<FiscalDeadLetterItem>> toDeadLetter(
            @PathVariable UUID id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(
                ApiResponse.of(monitorService.moveToDeadLetter(id, body.getOrDefault("reason", "Falha persistente"))));
    }
}
