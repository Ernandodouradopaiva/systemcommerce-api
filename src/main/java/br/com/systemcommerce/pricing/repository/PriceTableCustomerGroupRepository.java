package br.com.systemcommerce.pricing.repository;

import br.com.systemcommerce.pricing.entity.PriceTableCustomerGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceTableCustomerGroupRepository extends JpaRepository<PriceTableCustomerGroup, UUID> {

    List<PriceTableCustomerGroup> findByPriceTable_IdAndActiveTrue(UUID priceTableId);

    boolean existsByPriceTable_IdAndCustomerGroupCodeIgnoreCase(UUID priceTableId, String customerGroupCode);

    boolean existsByPriceTable_IdAndActiveTrue(UUID priceTableId);
}
