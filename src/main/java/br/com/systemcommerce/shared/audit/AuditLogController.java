package br.com.systemcommerce.shared.audit;

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
@RequestMapping("/api/v1/audit-logs")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(
        name = "Audit",
        description =
                "Consulta paginada da trilha de auditoria. Filtros: usuário, módulo, ação, entidade, "
                        + "loja, organização e período UTC [from, to). Usuários sem acesso global veem "
                        + "apenas eventos das lojas acessíveis (ou sem loja associada).")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    @Operation(summary = "Lista eventos de auditoria com filtros")
    public ResponseEntity<PageResponse<AuditLogResponse>> list(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) AuditLog.AuditAction action,
            @RequestParam(required = false) String entity,
            @RequestParam(required = false) UUID storeId,
            @RequestParam(required = false) UUID organizationId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @PageableDefault(size = 20, sort = "performedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(auditLogQueryService.search(
                userId, module, action, entity, storeId, organizationId, from, to, pageable)));
    }
}
