package br.com.systemcommerce.finance.card.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "card_fee_plans")
public class CardFeePlan extends AuditableEntity {
    public enum Modality { DEBIT, CREDIT }
    public enum Status { ACTIVE, INACTIVE }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "acquirer_id", nullable = false)
    private Acquirer acquirer;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "card_brand_id") private CardBrand cardBrand;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 120) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Modality modality;
    @Column(name = "installment_from", nullable = false) private Integer installmentFrom = 1;
    @Column(name = "installment_to", nullable = false) private Integer installmentTo = 1;
    @Column(name = "fee_percent", nullable = false, precision = 18, scale = 6) private BigDecimal feePercent = BigDecimal.ZERO;
    @Column(name = "fee_fixed", nullable = false, precision = 18, scale = 2) private BigDecimal feeFixed = BigDecimal.ZERO;
    @Column(name = "settlement_days", nullable = false) private Integer settlementDays = 1;
    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Column(name = "valid_to") private LocalDate validTo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.ACTIVE;
}
