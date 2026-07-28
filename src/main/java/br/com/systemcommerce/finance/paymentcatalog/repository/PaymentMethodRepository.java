package br.com.systemcommerce.finance.paymentcatalog.repository;

import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentMethod;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID>, JpaSpecificationExecutor<PaymentMethod> {
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);
    @Query("select m from PaymentMethod m left join fetch m.organization where m.id = :id")
    Optional<PaymentMethod> findDetailedById(@Param("id") UUID id);
}
