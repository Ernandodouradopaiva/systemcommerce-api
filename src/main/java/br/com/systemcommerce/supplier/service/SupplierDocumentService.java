package br.com.systemcommerce.supplier.service;

import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.supplier.dto.SupplierDocumentRequest;
import br.com.systemcommerce.supplier.dto.SupplierDocumentResponse;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierDocument;
import br.com.systemcommerce.supplier.mapper.SupplierDocumentMapper;
import br.com.systemcommerce.supplier.repository.SupplierDocumentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Metadados de documentos do fornecedor — sem upload binário (apenas referência externa). */
@Service
@RequiredArgsConstructor
public class SupplierDocumentService {

    private final SupplierDocumentRepository documentRepository;
    private final SupplierDocumentMapper documentMapper;
    private final SupplierService supplierService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<SupplierDocumentResponse> list(UUID supplierId) {
        supplierService.getEntity(supplierId);
        return documentRepository.findBySupplierIdOrderByUploadedAtDesc(supplierId).stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    @Transactional
    public SupplierDocumentResponse create(UUID supplierId, SupplierDocumentRequest request) {
        Supplier supplier = supplierService.getEntity(supplierId);
        SupplierDocument document = new SupplierDocument();
        document.setSupplier(supplier);
        documentMapper.apply(document, request);
        SupplierDocument saved = documentRepository.save(document);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierDocument",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                saved.getName(),
                "Documento registrado para o fornecedor");
        return documentMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID supplierId, UUID documentId) {
        SupplierDocument document = getOwned(supplierId, documentId);
        documentRepository.delete(document);
        domainAuditService.record(
                "SUPPLIER",
                "SupplierDocument",
                documentId,
                AuditLog.AuditAction.DELETE,
                document.getName(),
                null,
                "Documento removido do fornecedor");
    }

    private SupplierDocument getOwned(UUID supplierId, UUID documentId) {
        SupplierDocument document = documentRepository
                .findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento do fornecedor", documentId));
        if (!document.getSupplier().getId().equals(supplierId)) {
            throw new ResourceNotFoundException("Documento do fornecedor", documentId);
        }
        return document;
    }
}
