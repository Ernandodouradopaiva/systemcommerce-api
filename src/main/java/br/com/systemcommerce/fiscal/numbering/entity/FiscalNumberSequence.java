package br.com.systemcommerce.fiscal.numbering.entity;

import br.com.systemcommerce.fiscal.establishment.entity.FiscalEstablishment;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "fiscal_number_sequences",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_fnsq_est_model_series_env",
                        columnNames = {"establishment_id", "model", "series", "environment"}))
public class FiscalNumberSequence extends AuditableEntity {

    public enum SequenceStatus {
        ACTIVE,
        LOCKED
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "establishment_id", nullable = false)
    private FiscalEstablishment establishment;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "series", nullable = false, length = 10)
    private String series;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment", nullable = false, length = 20)
    private FiscalEstablishment.FiscalEnvironment environment;

    @Column(name = "current_number", nullable = false)
    private Long currentNumber = 0L;

    @Column(name = "last_reserved_number")
    private Long lastReservedNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SequenceStatus status = SequenceStatus.ACTIVE;

    public boolean isLocked() {
        return status == SequenceStatus.LOCKED;
    }
}
