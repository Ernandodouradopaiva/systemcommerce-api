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
@Table(name = "access_review_decisions")
public class AccessReviewDecision {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "review_item_id", nullable = false)
    private UUID reviewItemId;

    @Column(name = "decision", nullable = false, length = 20)
    private String decision;

    @Column(name = "decided_by", nullable = false)
    private UUID decidedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (decidedAt == null) {
            decidedAt = Instant.now();
        }
    }
}
