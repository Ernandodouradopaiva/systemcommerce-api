package br.com.systemcommerce.fiscal.establishment.entity;

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
        name = "fiscal_numbering_series",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_fiscal_numbering_series",
                        columnNames = {"establishment_id", "model", "series", "environment"}))
public class FiscalNumberingSeries extends AuditableEntity {

    public enum SeriesStatus {
        ACTIVE,
        INACTIVE
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

    @Column(name = "next_number", nullable = false)
    private Long nextNumber = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private SeriesStatus status = SeriesStatus.ACTIVE;
}
