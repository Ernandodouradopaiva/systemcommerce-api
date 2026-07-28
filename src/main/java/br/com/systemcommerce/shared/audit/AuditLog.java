package br.com.systemcommerce.shared.audit;

import br.com.systemcommerce.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    public enum AuditAction {
        CREATE,
        UPDATE,
        DELETE,
        ACTIVATE,
        DEACTIVATE,
        LOGIN,
        LOGIN_FAILURE,
        LOGOUT,
        STATUS_CHANGE,
        STOCK_MOVEMENT,
        OTHER
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "module", nullable = false, length = 40)
    private String module;

    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @Column(name = "entity_id")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction action;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;

    @Column(name = "details", length = 1000)
    private String details;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "event_code", length = 80)
    private String eventCode;

    @Column(name = "outcome", length = 20)
    private String outcome;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "warehouse_id")
    private UUID warehouseId;

    @Column(name = "seller_profile_id")
    private UUID sellerProfileId;

    @Column(name = "employee_id")
    private UUID employeeId;

    @Column(name = "terminal_id")
    private UUID terminalId;

    @Column(name = "cash_session_id")
    private UUID cashSessionId;

    @Column(name = "sale_id")
    private UUID saleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_id")
    private User operator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authorized_by_id")
    private User authorizedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    private User performedBy;

    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (performedAt == null) {
            performedAt = Instant.now();
        }
        if (module == null || module.isBlank()) {
            module = DomainAuditService.deriveModule(entityName);
        }
    }
}
