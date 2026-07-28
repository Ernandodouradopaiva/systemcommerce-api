package br.com.systemcommerce.fiscal.taxation.repository;

import br.com.systemcommerce.fiscal.taxation.entity.ProductTaxClassification;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductTaxClassificationRepository extends JpaRepository<ProductTaxClassification, UUID> {

    List<ProductTaxClassification> findByProfileId(UUID profileId);
}
