package br.com.systemcommerce.finance.closing.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "financial_periods")
public class FinancialPeriod extends AuditableEntity {
    public enum Status { OPEN, UNDER_REVIEW, CLOSED, REOPENED }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "store_id") private Store store;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 200) private String name;
    @Column(name = "start_date", nullable = false) private LocalDate startDate;
    @Column(name = "end_date", nullable = false) private LocalDate endDate;
    @Column(nullable = false, length = 60) private String timezone = "America/Sao_Paulo";
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.OPEN;
    @Column(length = 2000) private String notes;
    @OneToMany(mappedBy = "period", cascade = CascadeType.ALL)
    @OrderBy("closedAt DESC")
    private List<FinancialClosing> closings = new ArrayList<>();
}
