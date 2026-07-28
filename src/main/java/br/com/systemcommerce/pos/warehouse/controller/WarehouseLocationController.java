package br.com.systemcommerce.pos.warehouse.controller;

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
import br.com.systemcommerce.pos.warehouse.service.WarehouseLocationService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Endereçamento físico do depósito: zona → corredor → rack → prateleira → localização (Prompt 67). */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Warehouse Locations", description = "Endereçamento físico de depósitos (zonas/corredores/racks/prateleiras/localizações)")
public class WarehouseLocationController {

    private final WarehouseLocationService service;

    @PostMapping("/api/v1/warehouses/{warehouseId}/zones")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Cadastra zona de depósito")
    public ResponseEntity<ApiResponse<WarehouseZoneResponse>> createZone(
            @PathVariable UUID warehouseId, @Valid @RequestBody WarehouseZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(service.createZone(warehouseId, request)));
    }

    @GetMapping("/api/v1/warehouses/{warehouseId}/zones")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_READ') or hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Lista zonas do depósito")
    public ResponseEntity<ApiResponse<List<WarehouseZoneResponse>>> listZones(@PathVariable UUID warehouseId) {
        return ResponseEntity.ok(ApiResponse.of(service.listZones(warehouseId)));
    }

    @DeleteMapping("/api/v1/warehouse-zones/{zoneId}")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Remove zona de depósito (bloqueado se houver filhos)")
    public ResponseEntity<Void> deleteZone(@PathVariable UUID zoneId) {
        service.deleteZone(zoneId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/warehouse-zones/{zoneId}/aisles")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Cadastra corredor na zona")
    public ResponseEntity<ApiResponse<WarehouseAisleResponse>> createAisle(
            @PathVariable UUID zoneId, @Valid @RequestBody WarehouseAisleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.createAisle(zoneId, request)));
    }

    @GetMapping("/api/v1/warehouse-zones/{zoneId}/aisles")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_READ') or hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Lista corredores da zona")
    public ResponseEntity<ApiResponse<List<WarehouseAisleResponse>>> listAisles(@PathVariable UUID zoneId) {
        return ResponseEntity.ok(ApiResponse.of(service.listAisles(zoneId)));
    }

    @DeleteMapping("/api/v1/warehouse-aisles/{aisleId}")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Remove corredor (bloqueado se houver filhos)")
    public ResponseEntity<Void> deleteAisle(@PathVariable UUID aisleId) {
        service.deleteAisle(aisleId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/warehouse-aisles/{aisleId}/racks")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Cadastra rack no corredor")
    public ResponseEntity<ApiResponse<WarehouseRackResponse>> createRack(
            @PathVariable UUID aisleId, @Valid @RequestBody WarehouseRackRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.createRack(aisleId, request)));
    }

    @GetMapping("/api/v1/warehouse-aisles/{aisleId}/racks")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_READ') or hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Lista racks do corredor")
    public ResponseEntity<ApiResponse<List<WarehouseRackResponse>>> listRacks(@PathVariable UUID aisleId) {
        return ResponseEntity.ok(ApiResponse.of(service.listRacks(aisleId)));
    }

    @DeleteMapping("/api/v1/warehouse-racks/{rackId}")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Remove rack (bloqueado se houver filhos)")
    public ResponseEntity<Void> deleteRack(@PathVariable UUID rackId) {
        service.deleteRack(rackId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/warehouse-racks/{rackId}/shelves")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Cadastra prateleira no rack")
    public ResponseEntity<ApiResponse<WarehouseShelfResponse>> createShelf(
            @PathVariable UUID rackId, @Valid @RequestBody WarehouseShelfRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(service.createShelf(rackId, request)));
    }

    @GetMapping("/api/v1/warehouse-racks/{rackId}/shelves")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_READ') or hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Lista prateleiras do rack")
    public ResponseEntity<ApiResponse<List<WarehouseShelfResponse>>> listShelves(@PathVariable UUID rackId) {
        return ResponseEntity.ok(ApiResponse.of(service.listShelves(rackId)));
    }

    @DeleteMapping("/api/v1/warehouse-shelves/{shelfId}")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Remove prateleira (bloqueado se houver localizações)")
    public ResponseEntity<Void> deleteShelf(@PathVariable UUID shelfId) {
        service.deleteShelf(shelfId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/warehouses/{warehouseId}/storage-locations")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Cadastra localização de estoque no depósito")
    public ResponseEntity<ApiResponse<StorageLocationResponse>> createStorageLocation(
            @PathVariable UUID warehouseId, @Valid @RequestBody StorageLocationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(service.createStorageLocation(warehouseId, request)));
    }

    @GetMapping("/api/v1/warehouses/{warehouseId}/storage-locations")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_READ') or hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Lista localizações de estoque do depósito")
    public ResponseEntity<ApiResponse<List<StorageLocationResponse>>> listStorageLocations(
            @PathVariable UUID warehouseId) {
        return ResponseEntity.ok(ApiResponse.of(service.listStorageLocations(warehouseId)));
    }

    @GetMapping("/api/v1/storage-locations/{id}")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_READ') or hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Consulta localização de estoque por ID")
    public ResponseEntity<ApiResponse<StorageLocationResponse>> getStorageLocation(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(service.getStorageLocation(id)));
    }

    @PostMapping("/api/v1/storage-locations/{id}/activate")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Ativa localização de estoque")
    public ResponseEntity<ApiResponse<StorageLocationResponse>> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(service.activateStorageLocation(id)));
    }

    @PostMapping("/api/v1/storage-locations/{id}/inactivate")
    @PreAuthorize("hasAuthority('STORAGE_LOCATION_MANAGE')")
    @Operation(summary = "Inativa localização de estoque")
    public ResponseEntity<ApiResponse<StorageLocationResponse>> inactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(service.inactivateStorageLocation(id)));
    }
}
