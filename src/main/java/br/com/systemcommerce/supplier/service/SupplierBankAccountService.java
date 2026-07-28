package br.com.systemcommerce.supplier.service;

import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import br.com.systemcommerce.supplier.dto.SupplierBankAccountRequest;
import br.com.systemcommerce.supplier.dto.SupplierBankAccountResponse;
import br.com.systemcommerce.supplier.entity.Supplier;
import br.com.systemcommerce.supplier.entity.SupplierBankAccount;
import br.com.systemcommerce.supplier.mapper.SupplierBankAccountMapper;
import br.com.systemcommerce.supplier.repository.SupplierBankAccountRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dados bancários — leitura/gestão restritas via SUPPLIER_BANK_DATA_READ/MANAGE no controller. */
@Service
@RequiredArgsConstructor
public class SupplierBankAccountService {

    private final SupplierBankAccountRepository bankAccountRepository;
    private final SupplierBankAccountMapper bankAccountMapper;
    private final SupplierService supplierService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public List<SupplierBankAccountResponse> list(UUID supplierId) {
        supplierService.getEntity(supplierId);
        return bankAccountRepository.findBySupplierIdOrderByCreatedAtAsc(supplierId).stream()
                .map(bankAccountMapper::toResponse)
                .toList();
    }

    @Transactional
    public SupplierBankAccountResponse create(UUID supplierId, SupplierBankAccountRequest request) {
        Supplier supplier = supplierService.getEntity(supplierId);
        SupplierBankAccount account = new SupplierBankAccount();
        account.setSupplier(supplier);
        bankAccountMapper.apply(account, request);
        SupplierBankAccount saved = bankAccountRepository.save(account);
        auditMasked(saved.getId(), AuditLog.AuditAction.CREATE, "Conta bancária de fornecedor criada");
        return bankAccountMapper.toResponse(saved);
    }

    @Transactional
    public SupplierBankAccountResponse update(UUID supplierId, UUID accountId, SupplierBankAccountRequest request) {
        SupplierBankAccount account = getOwned(supplierId, accountId);
        bankAccountMapper.apply(account, request);
        SupplierBankAccount saved = bankAccountRepository.save(account);
        auditMasked(saved.getId(), AuditLog.AuditAction.UPDATE, "Conta bancária de fornecedor atualizada");
        return bankAccountMapper.toResponse(saved);
    }

    @Transactional
    public void delete(UUID supplierId, UUID accountId) {
        SupplierBankAccount account = getOwned(supplierId, accountId);
        bankAccountRepository.delete(account);
        auditMasked(accountId, AuditLog.AuditAction.DELETE, "Conta bancária de fornecedor removida");
    }

    /** Auditoria sem expor número de conta/PIX completos. */
    private void auditMasked(UUID accountId, AuditLog.AuditAction action, String details) {
        domainAuditService.record("SUPPLIER", "SupplierBankAccount", accountId, action, null, null, details);
    }

    private SupplierBankAccount getOwned(UUID supplierId, UUID accountId) {
        SupplierBankAccount account = bankAccountRepository
                .findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta bancária do fornecedor", accountId));
        if (!account.getSupplier().getId().equals(supplierId)) {
            throw new ResourceNotFoundException("Conta bancária do fornecedor", accountId);
        }
        return account;
    }
}
