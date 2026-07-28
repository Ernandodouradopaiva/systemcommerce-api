package br.com.systemcommerce.dashboard.controller;

import br.com.systemcommerce.dashboard.dto.DashboardSummaryResponse;
import br.com.systemcommerce.dashboard.service.DashboardService;
import br.com.systemcommerce.report.support.ReportScope;
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
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "Dashboard",
        description =
                "Indicadores agregados calculados no backend (UTC). "
                        + "Vendas efetivas: CONFIRMED, PAID, PARTIALLY_PAID. Recebimentos: pagamentos CONFIRMED. "
                        + "Filtro opcional: storeId e scope=STORE|MULTI|GLOBAL.")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('DASHBOARD_READ','DASHBOARD_STORE_READ','DASHBOARD_GLOBAL_READ')")
    @Operation(
            summary = "Resumo do dashboard",
            description =
                    "Vendas do dia/mês, ticket médio, top produtos/clientes, estoque baixo, "
                            + "vendas por status/período e recebimentos por forma de pagamento.")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
            @RequestParam(required = false) Integer topLimit,
            @RequestParam(required = false) Integer periodDays,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) ReportScope scope) {
        return ResponseEntity.ok(ApiResponse.of(dashboardService.summary(topLimit, periodDays, storeId, scope)));
    }
}
