package br.com.systemcommerce.finance.reconciliation.entity;

import br.com.systemcommerce.finance.bank.entity.FinancialAccountHolder;
import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bank_statement_imports")
public class BankStatementImport extends AuditableEntity {
    public enum ImportFormat { OFX, CSV }
    public enum Status { PENDING, COMPLETED, FAILED, DUPLICATE }

    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "holder_id", nullable = false)
    private FinancialAccountHolder holder;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "statement_id") private BankStatement statement;
    @Enumerated(EnumType.STRING) @Column(name = "import_format", nullable = false, length = 20) private ImportFormat importFormat;
    @Column(name = "file_name", length = 255) private String fileName;
    @Column(name = "file_hash", nullable = false, length = 128) private String fileHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private Status status = Status.PENDING;
    @Column(name = "entries_imported", nullable = false) private Integer entriesImported = 0;
    @Column(name = "error_message", length = 2000) private String errorMessage;
    @Column(name = "original_payload", columnDefinition = "TEXT") private String originalPayload;
    @Column(name = "idempotency_key", length = 100) private String idempotencyKey;
}
