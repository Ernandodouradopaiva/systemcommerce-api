package br.com.systemcommerce.fiscal.taxation.repository;

import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalog.CatalogType;
import br.com.systemcommerce.fiscal.taxation.entity.FiscalTaxCatalogVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalTaxCatalogVersionRepository extends JpaRepository<FiscalTaxCatalogVersion, UUID> {

    List<FiscalTaxCatalogVersion> findByCatalogTypeOrderByImportedAtDesc(CatalogType catalogType);
}
