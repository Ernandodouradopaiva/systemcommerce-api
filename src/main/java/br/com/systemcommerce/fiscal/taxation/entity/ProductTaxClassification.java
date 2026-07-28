package br.com.systemcommerce.fiscal.taxation.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "product_tax_classifications")
public class ProductTaxClassification extends AuditableEntity {

    public enum ClassificationStatus {
        ACTIVE,
        INACTIVE
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProductFiscalProfile profile;

    @Column(name = "tax_type", length = 40)
    private String taxType;

    @Column(name = "cst_or_csosn", length = 10)
    private String cstOrCsosn;

    @Column(name = "cfop_code", length = 10)
    private String cfopCode;

    @Column(name = "extra_json", columnDefinition = "TEXT")
    private String extraJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ClassificationStatus status = ClassificationStatus.ACTIVE;
}
