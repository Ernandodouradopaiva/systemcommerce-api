package br.com.systemcommerce.finance.card.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "acquirers")
public class Acquirer extends AuditableEntity {
    public enum Status { ACTIVE, INACTIVE }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Column(length = 20) private String document;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.ACTIVE;
}
