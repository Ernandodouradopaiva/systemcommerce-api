package br.com.systemcommerce.production.repository;

import br.com.systemcommerce.production.entity.BillOfMaterialsItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillOfMaterialsItemRepository extends JpaRepository<BillOfMaterialsItem, UUID> {

    @Query(
            """
            SELECT i FROM BillOfMaterialsItem i
            JOIN FETCH i.componentProduct
            WHERE i.billOfMaterials.id = :bomId AND i.active = true
            ORDER BY i.lineNumber ASC
            """)
    List<BillOfMaterialsItem> findActiveByBillOfMaterialsId(@Param("bomId") UUID bomId);
}
