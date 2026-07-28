package br.com.systemcommerce.finance.migration.controller;

import br.com.systemcommerce.finance.migration.service.FinanceBackfillService;
import br.com.systemcommerce.finance.migration.service.FinanceBackfillService.MigrationResult;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/finance/migration")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Finance Migration", description = "Backfill financeiro legado (Prompt 120)")
public class FinanceMigrationController {

    private final FinanceBackfillService backfillService;

    @PostMapping("/backfill")
    @PreAuthorize("hasAuthority('FINANCE_MIGRATION_RUN')")
    public ResponseEntity<ApiResponse<MigrationResult>> backfill(
            @RequestParam UUID organizationId, @RequestParam(defaultValue = "true") boolean dryRun) {
        return ResponseEntity.ok(ApiResponse.of(backfillService.run(organizationId, dryRun)));
    }
}
