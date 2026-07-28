package br.com.systemcommerce.access.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "access_audit_events")
public class AccessAuditEvent {

    public enum Result {
        SUCCESS,
        DENIED,
        FAILURE
    }

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "store_id")
    private UUID storeId;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "permission_id")
    private UUID permissionId;

    @Column(name = "permission_code", length = 80)
    private String permissionCode;

    @Column(name = "scope", length = 30)
    private String scope;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(name = "result", nullable = false, length = 20)
    private String result = Result.SUCCESS.name();

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "before_data", columnDefinition = "TEXT")
    private String beforeData;

    @Column(name = "after_data", columnDefinition = "TEXT")
    private String afterData;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
