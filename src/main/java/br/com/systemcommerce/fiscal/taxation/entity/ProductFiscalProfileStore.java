package br.com.systemcommerce.fiscal.taxation.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Entity;
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
        name = "product_fiscal_profile_stores",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_fiscal_profile_store", columnNames = {"profile_id", "store_id"}))
public class ProductFiscalProfileStore extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "profile_id", nullable = false)
    private ProductFiscalProfile profile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
}
