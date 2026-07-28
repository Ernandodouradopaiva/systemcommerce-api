package br.com.systemcommerce.pos.audit;

import br.com.systemcommerce.shared.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pos/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "POS Audit",
        description =
                """
                Trilha de auditoria do PDV: loja, terminal, sessão, operador, autorizador, venda, \
                resultado e correlation ID. Sem senhas/tokens/CVV/PAN.
                """)
public class PosAuditController {

    private final PosAuditQueryService posAuditQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('POS_AUDIT_READ') or hasAuthority('AUDIT_READ')")
    @Operation(summary = "Lista eventos de auditoria do PDV com filtros")
    public ResponseEntity<PageResponse<PosAuditLogResponse>> list(
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID terminalId,
            @RequestParam(required = false) UUID cashSessionId,
            @RequestParam(required = false) UUID saleId,
            @RequestParam(required = false) UUID operatorId,
            @RequestParam(required = false) String eventCode,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "performedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(posAuditQueryService.search(
                storeId, terminalId, cashSessionId, saleId, operatorId, eventCode, outcome, from, to, pageable)));
    }
}
