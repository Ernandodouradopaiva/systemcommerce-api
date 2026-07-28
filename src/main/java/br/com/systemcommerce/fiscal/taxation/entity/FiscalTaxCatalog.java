package br.com.systemcommerce.fiscal.taxation.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_tax_catalogs")
public class FiscalTaxCatalog extends AuditableEntity {

    public enum CatalogType {
        NCM,
        CEST,
        CFOP,
        CST_ICMS,
        CSOSN,
        CST_IPI,
        CST_PIS,
        CST_COFINS,
        ORIGEM_MERCADORIA,
        UNIDADE_TRIBUTAVEL,
        MODALIDADE_FRETE,
        BENEFICIO_FISCAL,
        MUNICIPIO_IBGE,
        PAIS,
        UF,
        MEIO_PAGAMENTO_FISCAL,
        FINALIDADE_EMISSAO,
        INDICADOR_PRESENCA,
        INDICADOR_INTERMEDIADOR
    }

    public enum CatalogStatus {
        ACTIVE,
        INACTIVE,
        DISCONTINUED
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "catalog_type", nullable = false, length = 40)
    private CatalogType catalogType;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "description", nullable = false, length = 500)
    private String description;

    @Column(name = "uf", length = 2)
    private String uf;

    @Column(name = "extra_json", columnDefinition = "TEXT")
    private String extraJson;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "catalog_version", nullable = false, length = 40)
    private String catalogVersion;

    @Column(name = "source", length = 100)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private CatalogStatus status = CatalogStatus.ACTIVE;

    public boolean isValidOn(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        if (validFrom != null && date.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && date.isAfter(validUntil)) {
            return false;
        }
        return Boolean.TRUE.equals(getActive()) && status == CatalogStatus.ACTIVE;
    }
}
