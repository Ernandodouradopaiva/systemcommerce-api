package br.com.systemcommerce.finance.paymentcatalog.repository;

import br.com.systemcommerce.finance.paymentcatalog.entity.PaymentCondition;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentConditionRepository extends JpaRepository<PaymentCondition, UUID>, JpaSpecificationExecutor<PaymentCondition> {
    boolean existsByOrganizationIdAndCodeIgnoreCase(UUID organizationId, String code);
    boolean existsByOrganizationIdAndCodeIgnoreCaseAndIdNot(UUID organizationId, String code, UUID id);
    @Query("select c from PaymentCondition c left join fetch c.installments left join fetch c.organization where c.id = :id")
    Optional<PaymentCondition> findDetailedById(@Param("id") UUID id);
}
