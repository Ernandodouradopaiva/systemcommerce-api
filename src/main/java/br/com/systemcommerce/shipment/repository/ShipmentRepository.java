package br.com.systemcommerce.shipment.repository;

import br.com.systemcommerce.shipment.entity.Shipment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID>, JpaSpecificationExecutor<Shipment> {

    @Query(
            """
            SELECT DISTINCT s FROM Shipment s
            LEFT JOIN FETCH s.items
            LEFT JOIN FETCH s.packages
            LEFT JOIN FETCH s.trackingEvents
            LEFT JOIN FETCH s.deliveryProofs
            WHERE s.id = :id
            """)
    Optional<Shipment> findDetailedById(@Param("id") UUID id);

    List<Shipment> findBySalesOrderId(UUID salesOrderId);

    @Query("SELECT COUNT(s) FROM Shipment s WHERE s.pickingOrder.id = :pickingOrderId")
    long countByPickingOrderId(@Param("pickingOrderId") UUID pickingOrderId);
}
