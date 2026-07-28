package br.com.systemcommerce.pos.warehouse.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.systemcommerce.pos.warehouse.dto.WarehouseZoneRequest;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseZoneResponse;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.entity.WarehouseZone;
import br.com.systemcommerce.pos.warehouse.repository.StorageLocationRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseAisleRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseRackRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseShelfRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseZoneRepository;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WarehouseLocationServiceTest {

    @Mock
    private WarehouseZoneRepository zoneRepository;

    @Mock
    private WarehouseAisleRepository aisleRepository;

    @Mock
    private WarehouseRackRepository rackRepository;

    @Mock
    private WarehouseShelfRepository shelfRepository;

    @Mock
    private StorageLocationRepository storageLocationRepository;

    @Mock
    private WarehouseService warehouseService;

    @Mock
    private DomainAuditService domainAuditService;

    private WarehouseLocationService service;

    private final UUID warehouseId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new WarehouseLocationService(
                zoneRepository, aisleRepository, rackRepository, shelfRepository, storageLocationRepository,
                warehouseService, domainAuditService);
    }

    @Test
    void shouldCreateZoneWhenCodeIsUniqueInWarehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        when(warehouseService.getEntity(warehouseId)).thenReturn(warehouse);
        when(zoneRepository.existsByWarehouse_IdAndCodeIgnoreCase(warehouseId, "A")).thenReturn(false);
        when(zoneRepository.save(org.mockito.ArgumentMatchers.any(WarehouseZone.class)))
                .thenAnswer(invocation -> {
                    WarehouseZone zone = invocation.getArgument(0);
                    zone.setId(UUID.randomUUID());
                    return zone;
                });

        WarehouseZoneResponse response = service.createZone(warehouseId, new WarehouseZoneRequest("A", "Zona A"));

        assertThat(response.code()).isEqualTo("A");
        assertThat(response.warehouseId()).isEqualTo(warehouseId);
    }

    @Test
    void shouldRejectDuplicatedZoneCodeInSameWarehouse() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        when(warehouseService.getEntity(warehouseId)).thenReturn(warehouse);
        when(zoneRepository.existsByWarehouse_IdAndCodeIgnoreCase(warehouseId, "A")).thenReturn(true);

        assertThatThrownBy(() -> service.createZone(warehouseId, new WarehouseZoneRequest("A", "Zona A")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldBlockZoneDeletionWhenAislesExist() {
        UUID zoneId = UUID.randomUUID();
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        WarehouseZone zone = new WarehouseZone();
        zone.setId(zoneId);
        zone.setWarehouse(warehouse);
        zone.setCode("A");
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));
        when(aisleRepository.findByZone_IdOrderByCodeAsc(zoneId))
                .thenReturn(List.of(new br.com.systemcommerce.pos.warehouse.entity.WarehouseAisle()));

        assertThatThrownBy(() -> service.deleteZone(zoneId)).isInstanceOf(ConflictException.class);
    }

    @Test
    void shouldDeleteZoneWhenNoChildrenExist() {
        UUID zoneId = UUID.randomUUID();
        Warehouse warehouse = new Warehouse();
        warehouse.setId(warehouseId);
        WarehouseZone zone = new WarehouseZone();
        zone.setId(zoneId);
        zone.setWarehouse(warehouse);
        zone.setCode("A");
        when(zoneRepository.findById(zoneId)).thenReturn(Optional.of(zone));
        when(aisleRepository.findByZone_IdOrderByCodeAsc(zoneId)).thenReturn(List.of());
        when(storageLocationRepository.existsByZone_Id(zoneId)).thenReturn(false);

        service.deleteZone(zoneId);

        org.mockito.Mockito.verify(zoneRepository).delete(zone);
    }
}
