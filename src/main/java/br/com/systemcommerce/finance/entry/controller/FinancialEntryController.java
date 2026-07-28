package br.com.systemcommerce.finance.entry.controller;

import br.com.systemcommerce.finance.entry.dto.FinancialEntryDtos.*;
import br.com.systemcommerce.finance.entry.entity.FinancialEntry;
import br.com.systemcommerce.finance.entry.service.FinancialEntryService;
import br.com.systemcommerce.shared.pagination.PageResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Financial Entries", description = "Lançamentos financeiros manuais (Prompt 108)")
public class FinancialEntryController {

    private final FinancialEntryService entryService;

    @GetMapping("/financial-entries")
    @PreAuthorize("hasAuthority('FINANCIAL_ENTRY_READ')")
    public ResponseEntity<PageResponse<Response>> list(
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) FinancialEntry.Status status,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(entryService.list(organizationId, status, pageable)));
    }

    @GetMapping("/financial-entries/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_ENTRY_READ')")
    public ResponseEntity<ApiResponse<Response>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(entryService.get(id)));
    }

    @PostMapping("/financial-entries")
    @PreAuthorize("hasAuthority('FINANCIAL_ENTRY_CREATE')")
    public ResponseEntity<ApiResponse<Response>> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(entryService.createDraft(request)));
    }

    @PutMapping("/financial-entries/{id}")
    @PreAuthorize("hasAuthority('FINANCIAL_ENTRY_CREATE')")
    public ResponseEntity<ApiResponse<Response>> update(
            @PathVariable UUID id, @Valid @RequestBody UpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.of(entryService.updateDraft(id, request)));
    }

    @PostMapping("/financial-entries/{id}/confirm")
    @PreAuthorize("hasAuthority('FINANCIAL_ENTRY_CONFIRM')")
    public ResponseEntity<ApiResponse<Response>> confirm(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(entryService.confirm(id)));
    }

    @PostMapping("/financial-entries/{id}/cancel")
    @PreAuthorize("hasAuthority('FINANCIAL_ENTRY_CANCEL')")
    public ResponseEntity<ApiResponse<Response>> cancel(
            @PathVariable UUID id, @Valid @RequestBody CancelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(entryService.cancelDraft(id, request)));
    }

    @PostMapping("/financial-entries/{id}/reverse")
    @PreAuthorize("hasAuthority('FINANCIAL_ENTRY_REVERSE')")
    public ResponseEntity<ApiResponse<Response>> reverse(
            @PathVariable UUID id, @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        return ResponseEntity.ok(ApiResponse.of(entryService.reverse(id, notes)));
    }
}
