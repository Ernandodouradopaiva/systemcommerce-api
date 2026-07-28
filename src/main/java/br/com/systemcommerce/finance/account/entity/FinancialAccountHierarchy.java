package br.com.systemcommerce.finance.account.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Closure table do plano de contas (Prompt 92). */
@Getter
@Setter
@Entity
@Table(name = "financial_account_hierarchy")
public class FinancialAccountHierarchy {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "ancestor_id", nullable = false)
    private UUID ancestorId;

    @Column(name = "descendant_id", nullable = false)
    private UUID descendantId;

    @Column(name = "depth", nullable = false)
    private Integer depth;

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
