package br.com.systemcommerce.access.auth;

import br.com.systemcommerce.access.auth.ResourceAccessResolver.ResourceSnapshot;
import br.com.systemcommerce.salesorder.entity.SalesOrder;
import br.com.systemcommerce.salesorder.repository.SalesOrderRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OwnershipResolver {

    private final SalesOrderRepository salesOrderRepository;

    public ResourceSnapshot resolve(String resourceType, UUID resourceId) {
        if (resourceType == null || resourceId == null) {
            return null;
        }
        return switch (resourceType.toUpperCase()) {
            case "SALES_ORDER", "SALESORDER" -> salesOrderRepository
                    .findById(resourceId)
                    .map(this::fromSalesOrder)
                    .orElse(null);
            default -> null;
        };
    }

    public boolean isOwner(UUID userId, ResourceSnapshot snapshot) {
        return snapshot != null && userId != null && userId.equals(snapshot.ownerUserId());
    }

    private ResourceSnapshot fromSalesOrder(SalesOrder order) {
        UUID owner = order.getSeller() != null
                ? order.getSeller().getId()
                : order.getCreatedBy();
        UUID storeId = order.getStore() != null ? order.getStore().getId() : null;
        return new ResourceSnapshot(order.getId(), storeId, owner, null);
    }
}
