package br.com.systemcommerce.fiscal.taxation.repository;

import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog;
import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog.CatalogType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FiscalTaxCatalogRepository
        extends JpaRepository<FiscalTaxCatalog, UUID>, JpaSpecificationExecutor<FiscalTaxCatalog> {

    @Query(
            """
            select c from FiscalTaxCatalog c
            where c.catalogType = :catalogType
              and c.code = :code
              and c.active = true
              and (:uf is null or c.uf is null or c.uf = :uf)
              and c.validFrom <= :onDate
              and (c.validUntil is null or c.validUntil >= :onDate)
            order by c.catalogVersion desc
            """)
    List<FiscalTaxCatalog> findValidEntries(
            @Param("catalogType") CatalogType catalogType,
            @Param("code") String code,
            @Param("uf") String uf,
            @Param("onDate") java.time.LocalDate onDate);

    Optional<FiscalTaxCatalog> findByCatalogTypeAndCodeAndCatalogVersionAndUf(
            CatalogType catalogType, String code, String catalogVersion, String uf);
}
