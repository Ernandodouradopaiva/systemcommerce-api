package br.com.systemcommerce.supplier.service;

import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.supplier.dto.SupplierContactRequest;
import br.com.systemcommerce.supplier.dto.SupplierContactResponse;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierContact;
import br.com.systemcommerce.supplier.mapper.SupplierContactMapper;
import br.com.systemcommerce.supplier.repository.SupplierContactRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierContactService {

    private final SupplierContactRepository contactRepository;
    private final SupplierContactMapper contactMapper;
    private final SupplierService supplierService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<SupplierContactResponse> list(UUID supplierId) {
        supplierService.getEntity(supplierId);
        return contactRepository.findBySupplierIdOrderByPrimaryDescCreatedAtAsc(supplierId).stream()
                .map(contactMapper::toResponse)
                .toList();
    }

    @Transactional
    public SupplierContactResponse create(UUID supplierId, SupplierContactRequest request) {
        Supplier supplier = supplierService.getEntity(supplierId);
        SupplierContact contact = new SupplierContact();
        contact.setSupplier(supplier);
        contactMapper.apply(contact, request);
        demoteOtherPrimaries(supplierId, contact);
        SupplierContact saved = contactRepository.save(contact);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierContact",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                saved.getName(),
                "Contato de fornecedor criado");
        return contactMapper.toResponse(saved);
    }

    @Transactional
    public SupplierContactResponse update(UUID supplierId, UUID contactId, SupplierContactRequest request) {
        SupplierContact contact = getOwned(supplierId, contactId);
        contactMapper.apply(contact, request);
        demoteOtherPrimaries(supplierId, contact);
        SupplierContact saved = contactRepository.save(contact);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierContact",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                null,
                saved.getName(),
                "Contato de fornecedor atualizado");
        return contactMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID supplierId, UUID contactId) {
        SupplierContact contact = getOwned(supplierId, contactId);
        contactRepository.delete(contact);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierContact",
                contactId,
                AuditLog.AuditAction.DELETE,
                contact.getName(),
                null,
                "Contato de fornecedor removido");
    }

    private void demoteOtherPrimaries(UUID supplierId, SupplierContact current) {
        if (!Boolean.TRUE.equals(current.getPrimary())) {
            return;
        }
        contactRepository.findBySupplierIdOrderByPrimaryDescCreatedAtAsc(supplierId).stream()
                .filter(c -> !c.getId().equals(current.getId()))
                .filter(c -> Boolean.TRUE.equals(c.getPrimary()))
                .forEach(c -> {
                    c.setPrimary(false);
                    contactRepository.save(c);
                });
    }

    private SupplierContact getOwned(UUID supplierId, UUID contactId) {
        SupplierContact contact = contactRepository
                .findById(contactId)
                .orElseThrow(() -> new ResourceNotFoundException("Contato do fornecedor", contactId));
        if (!contact.getSupplier().getId().equals(supplierId)) {
            throw new ResourceNotFoundException("Contato do fornecedor", contactId);
        }
        return contact;
    }
}
