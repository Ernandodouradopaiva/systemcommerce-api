package br.com.systemcommerce.serial.repository;

import br.com.systemcommerce.serial.entity.ProductSerialNumber;
import br.com.systemcommerce.serial.entity.ProductSerialStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductSerialNumberRepository
        extends JpaRepository<ProductSerialNumber, UUID>, JpaSpecificationExecutor<ProductSerialNumber> {

    @Query(
            """
            SELECT psn FROM ProductSerialNumber psn
            LEFT JOIN FETCH psn.product
            LEFT JOIN FETCH psn.organization
            WHERE psn.id = :id
            """)
    Optional<ProductSerialNumber> findDetailedById(@Param("id") UUID id);

    Optional<ProductSerialNumber> findByOrganizationIdAndSerialNumberAndActiveTrue(
            UUID organizationId, String serialNumber);

    boolean existsByOrganizationIdAndSerialNumberAndActiveTrue(UUID organizationId, String serialNumber);
}
