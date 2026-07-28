package br.com.systemcommerce.access.controller;

import br.com.systemcommerce.access.entity.AccessAuditEvent;
import br.com.systemcommerce.access.entity.AccessReview;
import br.com.systemcommerce.access.entity.AccessReviewItem;
import br.com.systemcommerce.access.entity.PrivilegedAccessRequest;
import br.com.systemcommerce.access.entity.UserSession;
import br.com.systemcommerce.access.repository.AccessAuditEventRepository;
import br.com.systemcommerce.access.service.AccessReportService;
import br.com.systemcommerce.access.service.AccessReviewService;
import br.com.systemcommerce.access.service.PrivilegedAccessService;
import br.com.systemcommerce.access.service.UserSessionService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/access-governance")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Access Governance", description = "Acesso privilegiado, auditoria, revisões e relatórios")
public class AccessGovernanceController {

    private final PrivilegedAccessService privilegedAccessService;
    private final AccessAuditEventRepository accessAuditEventRepository;
    private final AccessReviewService accessReviewService;
    private final AccessReportService accessReportService;
    private final UserSessionService userSessionService;

    public record PrivilegedRequestBody(UUID groupId, UUID permissionId, String justification, Instant validTo) {}

    public record DecideBody(boolean approve, String reason) {}

    public record ReviewCreateBody(String title, String notes) {}

    public record ReviewDecideBody(AccessReviewItem.Decision decision, String notes) {}

    @GetMapping("/privileged-requests")
    @PreAuthorize("hasAuthority('PRIVILEGED_ACCESS_APPROVE') or hasAuthority('PRIVILEGED_ACCESS_REQUEST')")
    @Operation(summary = "Lista solicitações privilegiadas pendentes")
    public ResponseEntity<ApiResponse<List<PrivilegedAccessRequest>>> pending() {
        return ResponseEntity.ok(ApiResponse.of(privilegedAccessService.listPending()));
    }

    @PostMapping("/privileged-requests")
    @PreAuthorize("hasAuthority('PRIVILEGED_ACCESS_REQUEST') or hasAuthority('ACCESS_GROUP_PERMISSION_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Solicita concessão privilegiada")
    public ResponseEntity<ApiResponse<PrivilegedAccessRequest>> request(@RequestBody PrivilegedRequestBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(privilegedAccessService.requestGrant(
                        body.groupId(), body.permissionId(), body.justification(), body.validTo())));
    }

    @PostMapping("/privileged-requests/{id}/decide")
    @PreAuthorize("hasAuthority('PRIVILEGED_ACCESS_APPROVE')")
    @Operation(summary = "Aprova ou rejeita solicitação privilegiada")
    public ResponseEntity<ApiResponse<PrivilegedAccessRequest>> decide(
            @PathVariable UUID id, @RequestBody DecideBody body) {
        return ResponseEntity.ok(ApiResponse.of(privilegedAccessService.decide(id, body.approve(), body.reason())));
    }

    @GetMapping("/audit")
    @PreAuthorize("hasAuthority('ACCESS_AUDIT_READ') or hasAuthority('AUDIT_READ')")
    @Operation(summary = "Auditoria de controle de acesso")
    public ResponseEntity<ApiResponse<Page<AccessAuditEvent>>> audit(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID actorId,
            @RequestParam(required = false) UUID targetId,
            Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.of(accessAuditEventRepository.search(eventType, actorId, targetId, pageable)));
    }

    @GetMapping("/reports/summary")
    @PreAuthorize("hasAuthority('ACCESS_REPORT_READ')")
    @Operation(summary = "Relatório consolidado de acessos")
    public ResponseEntity<ApiResponse<Map<String, Object>>> report() {
        return ResponseEntity.ok(ApiResponse.of(accessReportService.summary()));
    }

    @GetMapping("/reviews")
    @PreAuthorize("hasAuthority('ACCESS_REVIEW_MANAGE')")
    @Operation(summary = "Lista revisões periódicas")
    public ResponseEntity<ApiResponse<List<AccessReview>>> reviews() {
        return ResponseEntity.ok(ApiResponse.of(accessReviewService.list()));
    }

    @PostMapping("/reviews")
    @PreAuthorize("hasAuthority('ACCESS_REVIEW_MANAGE')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cria revisão periódica")
    public ResponseEntity<ApiResponse<AccessReview>> createReview(@RequestBody ReviewCreateBody body) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(accessReviewService.create(body.title(), body.notes())));
    }

    @GetMapping("/reviews/{id}/items")
    @PreAuthorize("hasAuthority('ACCESS_REVIEW_MANAGE')")
    public ResponseEntity<ApiResponse<List<AccessReviewItem>>> reviewItems(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(accessReviewService.items(id)));
    }

    @PostMapping("/reviews/items/{itemId}/decide")
    @PreAuthorize("hasAuthority('ACCESS_REVIEW_MANAGE')")
    public ResponseEntity<ApiResponse<AccessReviewItem>> decideItem(
            @PathVariable UUID itemId, @RequestBody ReviewDecideBody body) {
        return ResponseEntity.ok(ApiResponse.of(accessReviewService.decideItem(itemId, body.decision(), body.notes())));
    }

    @PostMapping("/reviews/{id}/complete")
    @PreAuthorize("hasAuthority('ACCESS_REVIEW_MANAGE')")
    public ResponseEntity<ApiResponse<AccessReview>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(accessReviewService.complete(id)));
    }

    @GetMapping("/users/{userId}/sessions")
    @PreAuthorize("hasAuthority('SESSION_MANAGE') or hasAuthority('USER_READ')")
    @Operation(summary = "Histórico de sessões do usuário")
    public ResponseEntity<ApiResponse<List<UserSession>>> sessions(@PathVariable UUID userId) {
        return ResponseEntity.ok(ApiResponse.of(userSessionService.listByUser(userId)));
    }

    @PostMapping("/users/{userId}/sessions/revoke-all")
    @PreAuthorize("hasAuthority('SESSION_MANAGE')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokeSessions(@PathVariable UUID userId) {
        userSessionService.revokeAllForUser(userId, "admin_revoke");
    }
}
