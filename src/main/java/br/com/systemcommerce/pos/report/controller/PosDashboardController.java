package br.com.systemcommerce.pos.report.controller;

import br.com.systemcommerce.pos.report.dto.PosDashboardResponse;
import br.com.systemcommerce.pos.report.service.PosDashboardService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "POS Dashboard", description = "Indicadores operacionais do dia (UTC) para o PDV")
public class PosDashboardController {

    private final PosDashboardService posDashboardService;

    @GetMapping
    @PreAuthorize("hasAuthority('POS_DASHBOARD_READ') or hasAuthority('POS_REPORT_READ')")
    @Operation(summary = "Dashboard do PDV (vendas do dia, caixas, ticket, hora, pagamentos)")
    public ResponseEntity<ApiResponse<PosDashboardResponse>> summary(
            @RequestParam(required = false) UUID storeId, @RequestParam(required = false) UUID terminalId) {
        return ResponseEntity.ok(ApiResponse.of(posDashboardService.summary(storeId, terminalId)));
    }
}
