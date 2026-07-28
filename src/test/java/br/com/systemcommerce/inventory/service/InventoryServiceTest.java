package br.com.systemcommerce.inventory.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.inventory.dto.InventoryExitRequest;
import br.com.systemcommerce.inventory.entity.Inventory;
import br.com.systemcommerce.inventory.mapper.InventoryMapper;
import br.com.systemcommerce.inventory.repository.InventoryAdjustmentReasonRepository;
import br.com.systemcommerce.inventory.repository.InventoryMovementRepository;
import br.com.systemcommerce.inventory.repository.InventoryRepository;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.service.WarehouseService;
import br.com.systemcommerce.product.entity.Product;
import br.com.systemcommerce.product.repository.ProductRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryMovementRepository movementRepository;

    @Mock
    private InventoryAdjustmentReasonRepository reasonRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private InventoryMapper inventoryMapper;

    @Mock
    private DomainAuditService domainAuditService;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void shouldRejectExitThatWouldGoNegative() {
        UUID productId = UUID.randomUUID();
        UUID warehouseId = UUID.randomUUID();
        Product product = new Product();
        product.setId(productId);
        product.setAllowNegativeStock(false);
        product.setMinStock(BigDecimal.ZERO);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);

        Inventory inventory = new Inventory();
        inventory.setProduct(product);
        inventory.setWarehouse(warehouse);
        inventory.setQuantity(new BigDecimal("5"));

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(warehouseService.requireDefaultWarehouse()).thenReturn(warehouse);
        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId))
                .thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> inventoryService.registerExit(
                        new InventoryExitRequest(productId, null, new BigDecimal("6"), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negativo");

        verify(movementRepository, never()).save(any());
    }

    @Test
    void shouldRejectZeroQuantity() {
        UUID productId = UUID.randomUUID();
        assertThatThrownBy(() -> inventoryService.registerExit(
                        new InventoryExitRequest(productId, null, BigDecimal.ZERO, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("maior que zero");
    }
}
