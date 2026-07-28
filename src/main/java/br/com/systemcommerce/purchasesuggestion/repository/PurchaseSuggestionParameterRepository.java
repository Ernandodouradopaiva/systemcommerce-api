package br.com.systemcommerce.purchasesuggestion.repository;

import br.com.systemcommerce.purchasesuggestion.entity.PurchaseSuggestionParameter;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseSuggestionParameterRepository extends JpaRepository<PurchaseSuggestionParameter, UUID> {

    Optional<PurchaseSuggestionParameter> findFirstByOrganizationIdAndStoreIdAndProductIdAndActiveTrue(
            UUID organizationId, UUID storeId, UUID productId);

    Optional<PurchaseSuggestionParameter> findFirstByOrganizationIdAndStoreIdIsNullAndProductIdIsNullAndActiveTrue(
            UUID organizationId);
}
