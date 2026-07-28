package br.com.systemcommerce.supplier.service;

import br.com.systemcommerce.customer.validation.BrazilianDocumentUtils;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.purchase.repository.PurchaseOrderRepository;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.shared.security.CurrentUser;
import br.com.systemcommerce.stockentry.repository.StockEntryRepository;
import br.com.systemcommerce.supplier.dto.SupplierCreateRequest;
import br.com.systemcommerce.supplier.dto.SupplierResponse;
import br.com.systemcommerce.supplier.dto.SupplierStatusHistoryResponse;
import br.com.systemcommerce.supplier.dto.SupplierUpdateRequest;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierStatusHistory;
import br.com.systemcommerce.supplier.mapper.SupplierMapper;
import br.com.systemcommerce.supplier.mapper.SupplierStatusHistoryMapper;
import br.com.systemcommerce.supplier.repository.SupplierDocumentRepository;
import br.com.systemcommerce.supplier.repository.SupplierRepository;
import br.com.systemcommerce.supplier.repository.SupplierStatusHistoryRepository;
import br.com.systemcommerce.supplier.specification.SupplierSpecifications;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final StockEntryRepository stockEntryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierDocumentRepository supplierDocumentRepository;
    private final SupplierStatusHistoryRepository statusHistoryRepository;
    private final SupplierMapper supplierMapper;
    private final SupplierStatusHistoryMapper statusHistoryMapper;
    private final OrganizationService organizationService;
    private final DomainAuditService domainAuditService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<SupplierResponse> list(
            String code,
            String name,
            String document,
            Supplier.SupplierStatus status,
            String search,
            Pageable pageable) {
        return supplierRepository
                .findAll(SupplierSpecifications.withFilters(code, name, document, status, search), pageable)
                .map(supplierMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getById(UUID id) {
        return supplierMapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Supplier getEntity(UUID id) {
        return supplierRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornecedor", id));
    }

    @Transactional(readOnly = true)
    public List<SupplierStatusHistoryResponse> statusHistory(UUID supplierId) {
        requireExists(supplierId);
        return statusHistoryRepository.findBySupplierIdOrderByChangedAtDesc(supplierId).stream()
                .map(statusHistoryMapper::toResponse)
                .toList();
    }

    @Transactional
    public SupplierResponse create(SupplierCreateRequest request) {
        String document = normalizeDocument(request.type(), request.document());
        BrazilianDocumentUtils.assertValidEmail(request.email());
        var organization = organizationService.requireDefault();
        BrazilianDocumentUtils.assertUniqueDocument(
                supplierRepository.existsByOrganizationIdAndDocument(organization.getId(), document));
        assertUniqueCode(request.code(), null);

        Supplier supplier = new Supplier();
        supplierMapper.applyCreate(supplier, request, document);
        supplier.setOrganization(organization);
        Supplier saved = supplierRepository.save(supplier);
        appendHistory(saved, null, saved.getStatus(), "Fornecedor criado");

        domainAuditService.record(
                "SUPPLIER",
                "Supplier",
                saved.getId(),
                AuditLog.AuditAction.CREATE,
                null,
                snapshot(saved),
                "Fornecedor criado");
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse update(UUID id, SupplierUpdateRequest request) {
        Supplier supplier = getEntity(id);
        Map<String, Object> before = snapshot(supplier);

        String document = normalizeDocument(request.type(), request.document());
        BrazilianDocumentUtils.assertValidEmail(request.email());
        UUID organizationId = supplier.getOrganization().getId();
        BrazilianDocumentUtils.assertUniqueDocument(
                supplierRepository.existsByOrganizationIdAndDocumentAndIdNot(organizationId, document, id));
        assertUniqueCode(request.code(), id);

        supplierMapper.applyUpdate(supplier, request, document);
        Supplier saved = supplierRepository.save(supplier);

        domainAuditService.record(
                "SUPPLIER",
                "Supplier",
                saved.getId(),
                AuditLog.AuditAction.UPDATE,
                before,
                snapshot(saved),
                "Fornecedor atualizado");
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse activate(UUID id) {
        Supplier supplier = getEntity(id);
        Supplier.SupplierStatus from = supplier.getStatus();
        Map<String, Object> before = snapshot(supplier);
        supplier.markActive();
        Supplier saved = supplierRepository.save(supplier);
        appendHistory(saved, from, saved.getStatus(), "Fornecedor ativado");
        domainAuditService.record(
                "SUPPLIER",
                "Supplier",
                saved.getId(),
                AuditLog.AuditAction.ACTIVATE,
                before,
                snapshot(saved),
                "Fornecedor ativado");
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse deactivate(UUID id) {
        Supplier supplier = getEntity(id);
        Supplier.SupplierStatus from = supplier.getStatus();
        Map<String, Object> before = snapshot(supplier);
        supplier.markInactive();
        Supplier saved = supplierRepository.save(supplier);
        appendHistory(saved, from, saved.getStatus(), "Fornecedor inativado");
        domainAuditService.record(
                "SUPPLIER",
                "Supplier",
                saved.getId(),
                AuditLog.AuditAction.DEACTIVATE,
                before,
                snapshot(saved),
                "Fornecedor inativado");
        return supplierMapper.toResponse(saved);
    }

    /** Fornecedor bloqueado não participa de novas compras até ser desbloqueado. */
    @Transactional
    public SupplierResponse block(UUID id, String reason) {
        Supplier supplier = getEntity(id);
        Supplier.SupplierStatus from = supplier.getStatus();
        if (from == Supplier.SupplierStatus.BLOCKED) {
            return supplierMapper.toResponse(supplier);
        }
        Map<String, Object> before = snapshot(supplier);
        supplier.markBlocked(reason);
        Supplier saved = supplierRepository.save(supplier);
        appendHistory(saved, from, saved.getStatus(), reason);
        domainAuditService.record(
                "SUPPLIER",
                "Supplier",
                saved.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(saved),
                "Fornecedor bloqueado: " + reason);
        return supplierMapper.toResponse(saved);
    }

    @Transactional
    public SupplierResponse unblock(UUID id) {
        Supplier supplier = getEntity(id);
        Supplier.SupplierStatus from = supplier.getStatus();
        if (from != Supplier.SupplierStatus.BLOCKED) {
            throw new BusinessRuleException("Fornecedor não está bloqueado");
        }
        Map<String, Object> before = snapshot(supplier);
        supplier.markUnblocked();
        Supplier saved = supplierRepository.save(supplier);
        appendHistory(saved, from, saved.getStatus(), "Fornecedor desbloqueado");
        domainAuditService.record(
                "SUPPLIER",
                "Supplier",
                saved.getId(),
                AuditLog.AuditAction.STATUS_CHANGE,
                before,
                snapshot(saved),
                "Fornecedor desbloqueado");
        return supplierMapper.toResponse(saved);
    }

    /**
     * Exclusão lógica (inativação) quando houver vínculo com entrada de estoque, pedido de compra ou
     * documento cadastrado; sem vínculo remove fisicamente. Nunca remove fisicamente fornecedor vinculado.
     */
    @Transactional
    public void delete(UUID id) {
        Supplier supplier = getEntity(id);
        Map<String, Object> before = snapshot(supplier);
        boolean hasStockMovement = stockEntryRepository.existsBySupplierNameIgnoreCase(supplier.getLegalName());
        boolean hasPurchaseOrder = purchaseOrderRepository.existsBySupplierId(id);
        boolean hasDocument = supplierDocumentRepository.existsBySupplierId(id);

        if (hasStockMovement || hasPurchaseOrder || hasDocument) {
            Supplier.SupplierStatus from = supplier.getStatus();
            supplier.markInactive();
            supplierRepository.save(supplier);
            appendHistory(supplier, from, supplier.getStatus(), "Exclusão lógica por vínculo com movimentação/pedido/documento");
            domainAuditService.record(
                    "SUPPLIER",
                    "Supplier",
                    id,
                    AuditLog.AuditAction.DELETE,
                    before,
                    snapshot(supplier),
                    "Exclusão lógica: fornecedor possui vínculo com estoque, pedido de compra ou documento");
            return;
        }

        supplierRepository.delete(supplier);
        domainAuditService.record(
                "SUPPLIER",
                "Supplier",
                id,
                AuditLog.AuditAction.DELETE,
                before,
                null,
                "Fornecedor removido fisicamente (sem movimentos)");
    }

    /** Fornecedor inativo ou bloqueado não pode ser utilizado em compra/entrada. */
    @Transactional(readOnly = true)
    public Supplier requireUsableForPurchase(UUID supplierId) {
        Supplier supplier = getEntity(supplierId);
        if (!supplier.isUsableForPurchase()) {
            String reason = supplier.isBlocked() ? "bloqueado" : "inativo";
            throw new BusinessRuleException("Fornecedor " + reason + " não pode ser utilizado em compra");
        }
        return supplier;
    }

    private void requireExists(UUID id) {
        if (!supplierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Fornecedor", id);
        }
    }

    private void appendHistory(
            Supplier supplier, Supplier.SupplierStatus from, Supplier.SupplierStatus to, String notes) {
        SupplierStatusHistory history = new SupplierStatusHistory();
        history.setSupplier(supplier);
        history.setFromStatus(from);
        history.setToStatus(to);
        history.setNotes(notes);
        CurrentUser.id().flatMap(userRepository::findById).ifPresent(history::setChangedBy);
        statusHistoryRepository.save(history);
    }

    private String normalizeDocument(Supplier.PersonType type, String rawDocument) {
        return BrazilianDocumentUtils.normalizeAndValidatePfOrPj(type == Supplier.PersonType.PF, rawDocument);
    }

    private void assertUniqueCode(String code, UUID excludeId) {
        String normalized = code == null ? null : code.trim();
        boolean exists = excludeId == null
                ? supplierRepository.existsByCode(normalized)
                : supplierRepository.existsByCodeAndIdNot(normalized, excludeId);
        if (exists) {
            throw new ConflictException("Código interno já está em uso");
        }
    }

    private Map<String, Object> snapshot(Supplier supplier) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("code", supplier.getCode());
        map.put("type", supplier.getType());
        map.put("legalName", supplier.getLegalName());
        map.put("tradeName", supplier.getTradeName());
        map.put("document", supplier.getDocument());
        map.put("email", supplier.getEmail());
        map.put("category", supplier.getCategory());
        map.put("status", supplier.getStatus());
        map.put("blockedReason", supplier.getBlockedReason());
        map.put("active", supplier.getActive());
        map.put("city", supplier.getCity());
        map.put("state", supplier.getState());
        return map;
    }
}
