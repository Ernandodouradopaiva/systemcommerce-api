package br.com.systemcommerce.supplier.service;

import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.supplier.dto.SupplierAddressRequest;
import br.com.systemcommerce.supplier.dto.SupplierAddressResponse;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierAddress;
import br.com.systemcommerce.supplier.mapper.SupplierAddressMapper;
import br.com.systemcommerce.supplier.repository.SupplierAddressRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierAddressService {

    private final SupplierAddressRepository addressRepository;
    private final SupplierAddressMapper addressMapper;
    private final SupplierService supplierService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<SupplierAddressResponse> list(UUID supplierId) {
        supplierService.getEntity(supplierId);
        return addressRepository.findBySupplierIdOrderByPrimaryDescCreatedAtAsc(supplierId).stream()
                .map(addressMapper::toResponse)
                .toList();
    }

    @Transactional
    public SupplierAddressResponse create(UUID supplierId, SupplierAddressRequest request) {
        Supplier supplier = supplierService.getEntity(supplierId);
        SupplierAddress address = new SupplierAddress();
        address.setSupplier(supplier);
        addressMapper.apply(address, request);
        demoteOtherPrimaries(supplierId, address);
        SupplierAddress saved = addressRepository.save(address);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierAddress",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                saved.getType(),
                "Endereço de fornecedor criado");
        return addressMapper.toResponse(saved);
    }

    @Transactional
    public SupplierAddressResponse update(UUID supplierId, UUID addressId, SupplierAddressRequest request) {
        SupplierAddress address = getOwned(supplierId, addressId);
        addressMapper.apply(address, request);
        demoteOtherPrimaries(supplierId, address);
        SupplierAddress saved = addressRepository.save(address);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierAddress",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                saved.getType(),
                "Endereço de fornecedor atualizado");
        return addressMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID supplierId, UUID addressId) {
        SupplierAddress address = getOwned(supplierId, addressId);
        addressRepository.delete(address);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierAddress",
                addressId,
                AuditLog.AuditAction.DELETE,
                address.getType(),
                null,
                "Endereço de fornecedor removido");
    }

    private void demoteOtherPrimaries(UUID supplierId, SupplierAddress current) {
        if (!Boolean.TRUE.equals(current.getPrimary())) {
            return;
        }
        addressRepository.findBySupplierIdOrderByPrimaryDescCreatedAtAsc(supplierId).stream()
                .filter(a -> !a.getId().equals(current.getId()))
                .filter(a -> Boolean.TRUE.equals(a.getPrimary()))
                .forEach(a -> {
                    a.setPrimary(false);
                    addressRepository.save(a);
                });
    }

    private SupplierAddress getOwned(UUID supplierId, UUID addressId) {
        SupplierAddress address = addressRepository
                .findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Endereço do fornecedor", addressId));
        if (!address.getSupplier().getId().equals(supplierId)) {
            throw new ResourceNotFoundException("Endereço do fornecedor", addressId);
        }
        return address;
    }
}
