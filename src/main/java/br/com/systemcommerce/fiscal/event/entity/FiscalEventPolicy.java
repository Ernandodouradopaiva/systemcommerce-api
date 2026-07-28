package br.com.systemcommerce.fiscal.event.entity;

import br.com.systemcommerce.fiscal.document.entity.FiscalDocument;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_event_policies")
public class FiscalEventPolicy extends AuditableEntity {

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "deadline_hours", nullable = false)
    private Integer deadlineHours = 24;

    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = Boolean.FALSE;
}
