package br.com.systemcommerce.pos.warehouse.service;

import br.com.systemcommerce.pos.warehouse.dto.StorageLocationRequest;
import br.com.systemcommerce.pos.warehouse.dto.StorageLocationResponse;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseAisleRequest;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseAisleResponse;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseRackRequest;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseRackResponse;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseShelfRequest;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseShelfResponse;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseZoneRequest;
import br.com.systemcommerce.pos.warehouse.dto.WarehouseZoneResponse;
import br.com.systemcommerce.pos.warehouse.entity.StorageLocation;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.pos.warehouse.entity.WarehouseAisle;
import br.com.systemcommerce.pos.warehouse.entity.WarehouseRack;
import br.com.systemcommerce.pos.warehouse.entity.WarehouseShelf;
import br.com.systemcommerce.pos.warehouse.entity.WarehouseZone;
import br.com.systemcommerce.pos.warehouse.repository.StorageLocationRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseAisleRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseRackRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseShelfRepository;
import br.com.systemcommerce.pos.warehouse.repository.WarehouseZoneRepository;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hierarquia de endereçamento físico do depósito (Prompt 67): zona → corredor → rack → prateleira → localização de
 * estoque. Regras de negócio (bloqueio de exclusão com filhos, unicidade de código) residem apenas na API.
 */
@Service
@RequiredArgsConstructor
public class WarehouseLocationService {

    private final WarehouseZoneRepository zoneRepository;
    private final WarehouseAisleRepository aisleRepository;
    private final WarehouseRackRepository rackRepository;
    private final WarehouseShelfRepository shelfRepository;
    private final StorageLocationRepository storageLocationRepository;
    private final WarehouseService warehouseService;
    private final DomainAuditService domainAuditService;

    @Transactional
    public WarehouseZoneResponse createZone(UUID warehouseId, WarehouseZoneRequest request) {
        Warehouse warehouse = warehouseService.getEntity(warehouseId);
        String code = MoneyAndQuantityUtils.requireText(request.code(), "Código da zona");
        if (zoneRepository.existsByWarehouse_IdAndCodeIgnoreCase(warehouseId, code)) {
            throw new ConflictException("Código de zona já cadastrado neste depósito");
        }
        WarehouseZone zone = new WarehouseZone();
        zone.setWarehouse(warehouse);
        zone.setCode(code);
        zone.setName(MoneyAndQuantityUtils.requireText(request.name(), "Nome da zona"));
        WarehouseZone saved = zoneRepository.save(zone);
        domainAuditService.record(
                "WAREHOUSE", "WarehouseZone", saved.getId(), AuditLog.AuditAction.CREATE, null, zoneSnapshot(saved),
                "Zona de depósito criada");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WarehouseZoneResponse> listZones(UUID warehouseId) {
        return zoneRepository.findByWarehouse_IdOrderByCodeAsc(warehouseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteZone(UUID zoneId) {
        WarehouseZone zone = requireZone(zoneId);
        if (!aisleRepository.findByZone_IdOrderByCodeAsc(zoneId).isEmpty()
                || storageLocationRepository.existsByZone_Id(zoneId)) {
            throw new ConflictException("Zona possui corredores ou localizações vinculadas");
        }
        zoneRepository.delete(zone);
        domainAuditService.record(
                "WAREHOUSE", "WarehouseZone", zoneId, AuditLog.AuditAction.DELETE, zoneSnapshot(zone), null,
                "Zona de depósito removida");
    }

    @Transactional
    public WarehouseAisleResponse createAisle(UUID zoneId, WarehouseAisleRequest request) {
        WarehouseZone zone = requireZone(zoneId);
        String code = MoneyAndQuantityUtils.requireText(request.code(), "Código do corredor");
        if (aisleRepository.existsByZone_IdAndCodeIgnoreCase(zoneId, code)) {
            throw new ConflictException("Código de corredor já cadastrado nesta zona");
        }
        WarehouseAisle aisle = new WarehouseAisle();
        aisle.setZone(zone);
        aisle.setCode(code);
        aisle.setName(request.name());
        WarehouseAisle saved = aisleRepository.save(aisle);
        domainAuditService.record(
                "WAREHOUSE", "WarehouseAisle", saved.getId(), AuditLog.AuditAction.CREATE, null, null,
                "Corredor de depósito criado");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WarehouseAisleResponse> listAisles(UUID zoneId) {
        return aisleRepository.findByZone_IdOrderByCodeAsc(zoneId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteAisle(UUID aisleId) {
        WarehouseAisle aisle = requireAisle(aisleId);
        if (!rackRepository.findByAisle_IdOrderByCodeAsc(aisleId).isEmpty()
                || storageLocationRepository.existsByAisle_Id(aisleId)) {
            throw new ConflictException("Corredor possui racks ou localizações vinculadas");
        }
        aisleRepository.delete(aisle);
        domainAuditService.record(
                "WAREHOUSE", "WarehouseAisle", aisleId, AuditLog.AuditAction.DELETE, null, null,
                "Corredor de depósito removido");
    }

    @Transactional
    public WarehouseRackResponse createRack(UUID aisleId, WarehouseRackRequest request) {
        WarehouseAisle aisle = requireAisle(aisleId);
        String code = MoneyAndQuantityUtils.requireText(request.code(), "Código do rack");
        if (rackRepository.existsByAisle_IdAndCodeIgnoreCase(aisleId, code)) {
            throw new ConflictException("Código de rack já cadastrado neste corredor");
        }
        WarehouseRack rack = new WarehouseRack();
        rack.setAisle(aisle);
        rack.setCode(code);
        rack.setName(request.name());
        WarehouseRack saved = rackRepository.save(rack);
        domainAuditService.record(
                "WAREHOUSE", "WarehouseRack", saved.getId(), AuditLog.AuditAction.CREATE, null, null,
                "Rack de depósito criado");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WarehouseRackResponse> listRacks(UUID aisleId) {
        return rackRepository.findByAisle_IdOrderByCodeAsc(aisleId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteRack(UUID rackId) {
        WarehouseRack rack = requireRack(rackId);
        if (!shelfRepository.findByRack_IdOrderByCodeAsc(rackId).isEmpty()
                || storageLocationRepository.existsByRack_Id(rackId)) {
            throw new ConflictException("Rack possui prateleiras ou localizações vinculadas");
        }
        rackRepository.delete(rack);
        domainAuditService.record(
                "WAREHOUSE", "WarehouseRack", rackId, AuditLog.AuditAction.DELETE, null, null,
                "Rack de depósito removido");
    }

    @Transactional
    public WarehouseShelfResponse createShelf(UUID rackId, WarehouseShelfRequest request) {
        WarehouseRack rack = requireRack(rackId);
        String code = MoneyAndQuantityUtils.requireText(request.code(), "Código da prateleira");
        if (shelfRepository.existsByRack_IdAndCodeIgnoreCase(rackId, code)) {
            throw new ConflictException("Código de prateleira já cadastrado neste rack");
        }
        WarehouseShelf shelf = new WarehouseShelf();
        shelf.setRack(rack);
        shelf.setCode(code);
        shelf.setName(request.name());
        WarehouseShelf saved = shelfRepository.save(shelf);
        domainAuditService.record(
                "WAREHOUSE", "WarehouseShelf", saved.getId(), AuditLog.AuditAction.CREATE, null, null,
                "Prateleira de depósito criada");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WarehouseShelfResponse> listShelves(UUID rackId) {
        return shelfRepository.findByRack_IdOrderByCodeAsc(rackId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public void deleteShelf(UUID shelfId) {
        WarehouseShelf shelf = requireShelf(shelfId);
        if (storageLocationRepository.existsByShelf_Id(shelfId)) {
            throw new ConflictException("Prateleira possui localizações de estoque vinculadas");
        }
        shelfRepository.delete(shelf);
        domainAuditService.record(
                "WAREHOUSE", "WarehouseShelf", shelfId, AuditLog.AuditAction.DELETE, null, null,
                "Prateleira de depósito removida");
    }

    @Transactional
    public StorageLocationResponse createStorageLocation(UUID warehouseId, StorageLocationRequest request) {
        Warehouse warehouse = warehouseService.getEntity(warehouseId);
        String code = MoneyAndQuantityUtils.requireText(request.code(), "Código da localização");
        if (storageLocationRepository.existsByWarehouse_IdAndCodeIgnoreCase(warehouseId, code)) {
            throw new ConflictException("Código de localização já cadastrado neste depósito");
        }
        StorageLocation location = new StorageLocation();
        location.setWarehouse(warehouse);
        location.setCode(code);
        location.setBarcode(request.barcode());
        location.setTrackBalance(Boolean.TRUE.equals(request.trackBalance()));
        if (request.zoneId() != null) {
            location.setZone(requireZone(request.zoneId()));
        }
        if (request.aisleId() != null) {
            location.setAisle(requireAisle(request.aisleId()));
        }
        if (request.rackId() != null) {
            location.setRack(requireRack(request.rackId()));
        }
        if (request.shelfId() != null) {
            location.setShelf(requireShelf(request.shelfId()));
        }
        StorageLocation saved = storageLocationRepository.save(location);
        domainAuditService.record(
                "WAREHOUSE", "StorageLocation", saved.getId(), AuditLog.AuditAction.CREATE, null,
                storageLocationSnapshot(saved), "Localização de estoque criada");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<StorageLocationResponse> listStorageLocations(UUID warehouseId) {
        return storageLocationRepository.findByWarehouse_IdOrderByCodeAsc(warehouseId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StorageLocationResponse getStorageLocation(UUID id) {
        return toResponse(requireStorageLocation(id));
    }

    @Transactional
    public StorageLocationResponse activateStorageLocation(UUID id) {
        StorageLocation location = requireStorageLocation(id);
        Map<String, Object> before = storageLocationSnapshot(location);
        location.markActive();
        StorageLocation saved = storageLocationRepository.save(location);
        domainAuditService.record(
                "WAREHOUSE", "StorageLocation", id, AuditLog.AuditAction.ACTIVATE, before,
                storageLocationSnapshot(saved), "Localização de estoque ativada");
        return toResponse(saved);
    }

    @Transactional
    public StorageLocationResponse inactivateStorageLocation(UUID id) {
        StorageLocation location = requireStorageLocation(id);
        Map<String, Object> before = storageLocationSnapshot(location);
        location.markInactive();
        StorageLocation saved = storageLocationRepository.save(location);
        domainAuditService.record(
                "WAREHOUSE", "StorageLocation", id, AuditLog.AuditAction.DEACTIVATE, before,
                storageLocationSnapshot(saved), "Localização de estoque inativada");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public StorageLocation requireStorageLocation(UUID id) {
        return storageLocationRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Localização de estoque", id));
    }

    private WarehouseZone requireZone(UUID id) {
        return zoneRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Zona de depósito", id));
    }

    private WarehouseAisle requireAisle(UUID id) {
        return aisleRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Corredor de depósito", id));
    }

    private WarehouseRack requireRack(UUID id) {
        return rackRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Rack de depósito", id));
    }

    private WarehouseShelf requireShelf(UUID id) {
        return shelfRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prateleira de depósito", id));
    }

    private WarehouseZoneResponse toResponse(WarehouseZone zone) {
        return new WarehouseZoneResponse(
                zone.getId(), zone.getWarehouse().getId(), zone.getCode(), zone.getName(), zone.getStatus(),
                zone.getActive());
    }

    private WarehouseAisleResponse toResponse(WarehouseAisle aisle) {
        return new WarehouseAisleResponse(
                aisle.getId(), aisle.getZone().getId(), aisle.getCode(), aisle.getName(), aisle.getActive());
    }

    private WarehouseRackResponse toResponse(WarehouseRack rack) {
        return new WarehouseRackResponse(
                rack.getId(), rack.getAisle().getId(), rack.getCode(), rack.getName(), rack.getActive());
    }

    private WarehouseShelfResponse toResponse(WarehouseShelf shelf) {
        return new WarehouseShelfResponse(
                shelf.getId(), shelf.getRack().getId(), shelf.getCode(), shelf.getName(), shelf.getActive());
    }

    private StorageLocationResponse toResponse(StorageLocation location) {
        return new StorageLocationResponse(
                location.getId(),
                location.getWarehouse().getId(),
                location.getZone() != null ? location.getZone().getId() : null,
                location.getAisle() != null ? location.getAisle().getId() : null,
                location.getRack() != null ? location.getRack().getId() : null,
                location.getShelf() != null ? location.getShelf().getId() : null,
                location.getCode(),
                location.getBarcode(),
                location.getStatus(),
                location.getTrackBalance(),
                location.getActive());
    }

    private Map<String, Object> zoneSnapshot(WarehouseZone zone) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", zone.getId());
        map.put("warehouseId", zone.getWarehouse().getId());
        map.put("code", zone.getCode());
        map.put("name", zone.getName());
        map.put("status", zone.getStatus());
        return map;
    }

    private Map<String, Object> storageLocationSnapshot(StorageLocation location) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", location.getId());
        map.put("warehouseId", location.getWarehouse().getId());
        map.put("code", location.getCode());
        map.put("status", location.getStatus());
        map.put("trackBalance", location.getTrackBalance());
        return map;
    }
}
