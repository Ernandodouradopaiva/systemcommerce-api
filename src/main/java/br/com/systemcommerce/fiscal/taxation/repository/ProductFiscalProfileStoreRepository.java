package br.com.systemcommerce.fiscal.taxation.repository;

import br.com.systemcommerce.fiscal.taxation.entity.ProductFiscalProfileStore;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductFiscalProfileStoreRepository extends JpaRepository<ProductFiscalProfileStore, UUID> {}
