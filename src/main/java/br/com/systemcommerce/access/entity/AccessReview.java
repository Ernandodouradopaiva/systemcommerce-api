package br.com.systemcommerce.access.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "access_reviews")
public class AccessReview extends AuditableEntity {

    public enum Status {
        DRAFT,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "code", nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "reviewer_user_id")
    private UUID reviewerUserId;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "next_review_at")
    private Instant nextReviewAt;

    @Column(name = "notes", length = 1000)
    private String notes;
}
