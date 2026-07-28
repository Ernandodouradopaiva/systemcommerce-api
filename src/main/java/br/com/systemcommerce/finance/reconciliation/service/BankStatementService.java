package br.com.systemcommerce.finance.reconciliation.service;

import br.com.systemcommerce.finance.bank.service.BankFinanceService;
import br.com.systemcommerce.finance.reconciliation.dto.ReconciliationDtos.*;
import br.com.systemcommerce.finance.reconciliation.entity.*;
import br.com.systemcommerce.finance.reconciliation.importing.StatementParsers;
import br.com.systemcommerce.finance.reconciliation.importing.StatementParsers.ParsedEntry;
import br.com.systemcommerce.finance.reconciliation.repository.*;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.product.validation.MoneyAndQuantityUtils;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.audit.DomainAuditService;
import br.com.systemcommerce.shared.exception.BusinessRuleException;
import br.com.systemcommerce.shared.exception.ConflictException;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BankStatementService {

    private final BankStatementRepository statementRepository;
    private final BankStatementEntryRepository entryRepository;
    private final BankStatementImportRepository importRepository;
    private final OrganizationService organizationService;
    private final BankFinanceService bankFinanceService;
    private final DomainAuditService domainAuditService;

    @Transactional(readOnly = true)
    public Page<StatementResponse> list(UUID organizationId, Pageable pageable) {
        Specification<BankStatement> spec = (root, q, cb) -> organizationId == null
                ? cb.conjunction()
                : cb.equal(root.get("organization").get("id"), organizationId);
        return statementRepository.findAll(spec, pageable).map(this::toStatement);
    }

    @Transactional(readOnly = true)
    public StatementResponse get(UUID id) {
        return toStatement(require(id));
    }

    @Transactional(readOnly = true)
    public List<EntryResponse> listEntries(UUID statementId) {
        require(statementId);
        return entryRepository.findByStatementIdOrderByEntryDateAsc(statementId).stream()
                .map(this::toEntry)
                .toList();
    }

    @Transactional
    public StatementResponse createManual(ManualStatementRequest request) {
        if (StringUtils.hasText(request.idempotencyKey())) {
            var existing = statementRepository.findByOrganizationIdAndIdempotencyKey(
                    request.organizationId(), request.idempotencyKey());
            if (existing.isPresent()) {
                return toStatement(existing.get());
            }
        }
        BankStatement statement = new BankStatement();
        statement.setOrganization(organizationService.requireUsable(request.organizationId()));
        statement.setHolder(bankFinanceService.requireUsableHolder(request.holderId()));
        statement.setStatementDate(request.statementDate());
        statement.setPeriodStart(request.periodStart());
        statement.setPeriodEnd(request.periodEnd());
        statement.setOpeningBalance(request.openingBalance());
        statement.setClosingBalance(request.closingBalance());
        statement.setSourceType(BankStatement.SourceType.MANUAL);
        statement.setNotes(MoneyAndQuantityUtils.blankToNull(request.notes()));
        statement.setIdempotencyKey(request.idempotencyKey());
        BankStatement saved = statementRepository.save(statement);
        domainAuditService.record(
                "FINANCE", "BankStatement", saved.getId(), AuditLog.AuditAction.CREATE, null, null, "Extrato manual");
        return toStatement(saved);
    }

    @Transactional
    public EntryResponse addManualEntry(UUID statementId, ManualEntryRequest request) {
        BankStatement statement = require(statementId);
        if (statement.getSourceType() != BankStatement.SourceType.MANUAL) {
            throw new BusinessRuleException("Somente extrato manual aceita lançamento avulso");
        }
        BankStatementEntry entry = buildEntry(
                statement,
                request.entryDate(),
                request.description(),
                request.documentNumber(),
                request.amount().abs().setScale(2, RoundingMode.HALF_UP),
                request.entryType(),
                request.externalId() != null ? request.externalId() : UUID.randomUUID().toString(),
                request.informedBalance(),
                null);
        entryRepository.save(entry);
        domainAuditService.record(
                "FINANCE", "BankStatementEntry", entry.getId(), AuditLog.AuditAction.CREATE, null, null, "Lançamento manual de extrato");
        return toEntry(entry);
    }

    @Transactional
    public ImportResponse importOfx(ImportOfxRequest request) {
        return importParsed(
                request.organizationId(),
                request.holderId(),
                request.fileName(),
                request.payload(),
                request.idempotencyKey(),
                BankStatementImport.ImportFormat.OFX,
                BankStatement.SourceType.OFX,
                StatementParsers.parseOfx(request.payload()));
    }

    @Transactional
    public ImportResponse importCsv(ImportCsvRequest request) {
        char delimiter = StringUtils.hasText(request.delimiter()) ? request.delimiter().charAt(0) : ';';
        return importParsed(
                request.organizationId(),
                request.holderId(),
                request.fileName(),
                request.payload(),
                request.idempotencyKey(),
                BankStatementImport.ImportFormat.CSV,
                BankStatement.SourceType.CSV,
                StatementParsers.parseCsv(
                        request.payload(),
                        request.dateColumn(),
                        request.descriptionColumn(),
                        request.amountColumn(),
                        request.documentColumn(),
                        delimiter));
    }

    private ImportResponse importParsed(
            UUID organizationId,
            UUID holderId,
            String fileName,
            String payload,
            String idempotencyKey,
            BankStatementImport.ImportFormat format,
            BankStatement.SourceType sourceType,
            List<ParsedEntry> parsed) {
        if (StringUtils.hasText(idempotencyKey)) {
            var existing = importRepository.findByOrganizationIdAndIdempotencyKey(organizationId, idempotencyKey);
            if (existing.isPresent()) {
                BankStatementImport imp = existing.get();
                return new ImportResponse(
                        imp.getId(),
                        imp.getStatement() != null ? imp.getStatement().getId() : null,
                        imp.getStatus(),
                        imp.getEntriesImported(),
                        imp.getErrorMessage());
            }
        }
        String hash = StatementParsers.sha256(payload);
        var holder = bankFinanceService.requireUsableHolder(holderId);
        BankStatementImport imp = new BankStatementImport();
        imp.setOrganization(organizationService.requireUsable(organizationId));
        imp.setHolder(holder);
        imp.setImportFormat(format);
        imp.setFileName(MoneyAndQuantityUtils.blankToNull(fileName));
        imp.setFileHash(hash);
        imp.setOriginalPayload(payload);
        imp.setIdempotencyKey(idempotencyKey);

        if (importRepository.existsByHolderIdAndFileHash(holderId, hash)
                || statementRepository.existsByHolderIdAndExternalFileHash(holderId, hash)) {
            imp.setStatus(BankStatementImport.Status.DUPLICATE);
            imp.setErrorMessage("Importação duplicada para esta conta (hash do arquivo)");
            BankStatementImport saved = importRepository.save(imp);
            domainAuditService.record(
                    "FINANCE",
                    "BankStatementImport",
                    saved.getId(),
                    AuditLog.AuditAction.OTHER,
                    null,
                    null,
                    "Importação duplicada rejeitada");
            return new ImportResponse(saved.getId(), null, saved.getStatus(), 0, saved.getErrorMessage());
        }

        try {
            BankStatement statement = new BankStatement();
            statement.setOrganization(imp.getOrganization());
            statement.setHolder(holder);
            statement.setStatementDate(LocalDate.now());
            statement.setSourceType(sourceType);
            statement.setExternalFileHash(hash);
            statement.setOriginalPayload(payload);
            statement.setIdempotencyKey("stmt-" + idempotencyKey);
            statement = statementRepository.save(statement);

            int count = 0;
            for (ParsedEntry p : parsed) {
                BankStatementEntry entry = buildEntry(
                        statement,
                        p.date(),
                        p.description(),
                        p.document(),
                        p.amount().setScale(2, RoundingMode.HALF_UP),
                        p.type(),
                        p.externalId(),
                        null,
                        p.raw());
                entryRepository.save(entry);
                count++;
            }
            imp.setStatement(statement);
            imp.setEntriesImported(count);
            imp.setStatus(BankStatementImport.Status.COMPLETED);
            BankStatementImport saved = importRepository.save(imp);
            domainAuditService.record(
                    "FINANCE",
                    "BankStatementImport",
                    saved.getId(),
                    AuditLog.AuditAction.CREATE,
                    null,
                    null,
                    "Importação " + format + " com " + count + " lançamentos");
            return new ImportResponse(saved.getId(), statement.getId(), saved.getStatus(), count, null);
        } catch (RuntimeException ex) {
            imp.setStatus(BankStatementImport.Status.FAILED);
            imp.setErrorMessage(ex.getMessage());
            importRepository.save(imp);
            throw ex;
        }
    }

    private BankStatementEntry buildEntry(
            BankStatement statement,
            LocalDate date,
            String description,
            String document,
            BigDecimal amount,
            BankStatementEntry.EntryType type,
            String externalId,
            BigDecimal informedBalance,
            String raw) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("Valor do lançamento do extrato deve ser positivo");
        }
        BankStatementEntry entry = new BankStatementEntry();
        entry.setStatement(statement);
        entry.setHolder(statement.getHolder());
        entry.setEntryDate(date);
        entry.setDescription(MoneyAndQuantityUtils.requireText(description, "Descrição"));
        entry.setDocumentNumber(MoneyAndQuantityUtils.blankToNull(document));
        entry.setAmount(amount);
        entry.setEntryType(type);
        entry.setExternalId(externalId);
        entry.setInformedBalance(informedBalance);
        entry.setFitId(externalId);
        entry.setRawLine(raw);
        entry.setReconciliationStatus(BankStatementEntry.ReconciliationStatus.UNMATCHED);
        return entry;
    }

    private BankStatement require(UUID id) {
        return statementRepository
                .findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Extrato bancário não encontrado"));
    }

    private StatementResponse toStatement(BankStatement s) {
        return new StatementResponse(
                s.getId(),
                s.getOrganization().getId(),
                s.getHolder().getId(),
                s.getStatementDate(),
                s.getSourceType(),
                s.getStatus(),
                s.getExternalFileHash(),
                s.getEntries() != null ? s.getEntries().size() : entryRepository.findByStatementIdOrderByEntryDateAsc(s.getId()).size());
    }

    private EntryResponse toEntry(BankStatementEntry e) {
        return new EntryResponse(
                e.getId(),
                e.getStatement().getId(),
                e.getEntryDate(),
                e.getDescription(),
                e.getDocumentNumber(),
                e.getAmount(),
                e.getEntryType(),
                e.getExternalId(),
                e.getReconciliationStatus());
    }
}
