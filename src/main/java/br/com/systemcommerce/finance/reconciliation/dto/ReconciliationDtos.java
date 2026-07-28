package br.com.systemcommerce.finance.reconciliation.dto;

import br.com.systemcommerce.finance.reconciliation.entity.BankStatement;
import br.com.systemcommerce.finance.reconciliation.entity.BankStatementEntry;
import br.com.systemcommerce.finance.reconciliation.entity.BankStatementImport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ReconciliationDtos {
    private ReconciliationDtos() {}

    public record ManualStatementRequest(
            @NotNull UUID organizationId,
            @NotNull UUID holderId,
            @NotNull LocalDate statementDate,
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            String notes,
            @NotBlank String idempotencyKey) {}

    public record ManualEntryRequest(
            @NotNull LocalDate entryDate,
            @NotBlank String description,
            String documentNumber,
            @NotNull BigDecimal amount,
            @NotNull BankStatementEntry.EntryType entryType,
            BigDecimal informedBalance,
            String externalId) {}

    public record ImportOfxRequest(
            @NotNull UUID organizationId,
            @NotNull UUID holderId,
            String fileName,
            @NotBlank String payload,
            @NotBlank String idempotencyKey) {}

    public record ImportCsvRequest(
            @NotNull UUID organizationId,
            @NotNull UUID holderId,
            String fileName,
            @NotBlank String payload,
            int dateColumn,
            int descriptionColumn,
            int amountColumn,
            Integer documentColumn,
            String delimiter,
            @NotBlank String idempotencyKey) {}

    public record RuleCreateRequest(
            @NotNull UUID organizationId,
            UUID holderId,
            @NotBlank String code,
            @NotBlank String name,
            Integer priority,
            Boolean matchByAmount,
            Boolean matchByDate,
            Integer dateToleranceDays,
            Boolean matchByDocument,
            String descriptionContains,
            Boolean autoConfirm,
            Boolean safeAuto) {}

    public record ReconciliationCreateRequest(
            @NotNull UUID organizationId,
            @NotNull UUID holderId,
            UUID statementId,
            @NotNull LocalDate reconciliationDate,
            String notes,
            @NotBlank String idempotencyKey) {}

    public record CreateMissingRequest(@NotBlank String description) {}

    public record StatementResponse(
            UUID id,
            UUID organizationId,
            UUID holderId,
            LocalDate statementDate,
            BankStatement.SourceType sourceType,
            BankStatement.Status status,
            String externalFileHash,
            int entryCount) {}

    public record EntryResponse(
            UUID id,
            UUID statementId,
            LocalDate entryDate,
            String description,
            String documentNumber,
            BigDecimal amount,
            BankStatementEntry.EntryType entryType,
            String externalId,
            BankStatementEntry.ReconciliationStatus reconciliationStatus) {}

    public record ImportResponse(
            UUID id,
            UUID statementId,
            BankStatementImport.Status status,
            int entriesImported,
            String errorMessage) {}
}
