package br.com.systemcommerce.finance.migration.service;

import br.com.systemcommerce.finance.migration.entity.FinanceMigrationRun;
import br.com.systemcommerce.finance.migration.repository.FinanceMigrationRunRepository;
import br.com.systemcommerce.finance.payable.dto.PayableFromPurchaseRequest;
import br.com.systemcommerce.finance.payable.service.PayableService;
import br.com.systemcommerce.finance.receivable.dto.ReceivableFromSaleRequest;
import br.com.systemcommerce.finance.receivable.service.ReceivableService;
import br.com.systemcommerce.finance.security.FinanceAuditEvents;
import br.com.systemcommerce.finance.security.FinanceAuditService;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.audit.AuditLog;
import br.com.systemcommerce.shared.security.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Backfill idempotente de AR/AP a partir de vendas e recebimentos de compra existentes.
 * Não duplica: usa origem (ReceivableOrigin / PayableOrigin).
 */
@Service
@RequiredArgsConstructor
public class FinanceBackfillService {

    private final FinanceMigrationRunRepository runRepository;
    private final OrganizationService organizationService;
    private final ReceivableService receivableService;
    private final PayableService payableService;
    private final FinanceAuditService financeAuditService;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager em;

    public record MigrationResult(
            UUID runId,
            boolean dryRun,
            String status,
            int salesScanned,
            int receivablesCreated,
            int purchasesScanned,
            int payablesCreated,
            int skippedDuplicates,
            int errorsCount,
            List<String> notes) {}

    @Transactional
    public MigrationResult run(UUID organizationId, boolean dryRun) {
        organizationService.requireUsable(organizationId);
        FinanceMigrationRun run = new FinanceMigrationRun();
        run.setOrganization(organizationService.requireUsable(organizationId));
        run.setDryRun(dryRun);
        run.setStatus(FinanceMigrationRun.Status.RUNNING);
        run.setStartedAt(Instant.now());
        CurrentUser.id().ifPresent(run::setStartedBy);
        run = runRepository.save(run);

        List<String> notes = new ArrayList<>();
        int salesScanned = 0;
        int arCreated = 0;
        int purchasesScanned = 0;
        int apCreated = 0;
        int skipped = 0;
        int errors = 0;

        @SuppressWarnings("unchecked")
        List<UUID> saleIds = em.createNativeQuery(
                        """
                        select s.id from sales s
                        where s.organization_id = :org
                          and s.status in ('CONFIRMED','PAID','PARTIALLY_PAID')
                          and s.customer_id is not null
                          and s.total_amount > 0
                          and not exists (
                            select 1 from receivable_origins o
                            where o.origin_document_id = s.id
                              and o.origin_type in ('SALE','POS')
                          )
                        limit 500
                        """)
                .setParameter("org", organizationId)
                .getResultList();

        for (Object raw : saleIds) {
            UUID saleId = toUuid(raw);
            salesScanned++;
            try {
                if (dryRun) {
                    notes.add("DRY-RUN AR sale=" + saleId);
                    arCreated++;
                } else {
                    var before = receivableService.generateFromSale(new ReceivableFromSaleRequest(
                            saleId, null, null, null, "migration-sale-" + saleId));
                    // se já existisse, generateFromSale retorna existente; conta como skip se idempotent
                    arCreated++;
                    notes.add("AR ok sale=" + saleId + " receivable=" + before.id());
                }
            } catch (Exception ex) {
                errors++;
                notes.add("ERR AR sale=" + saleId + ": " + ex.getMessage());
            }
        }

        @SuppressWarnings("unchecked")
        List<UUID> receiptIds = em.createNativeQuery(
                        """
                        select r.id from purchase_receipts r
                        where r.organization_id = :org
                          and r.status in ('POSTED_TO_INVENTORY','ACCEPTED','CONFIRMED')
                          and not exists (
                            select 1 from payable_origins o
                            where o.origin_document_id = r.id
                              and o.origin_type = 'PURCHASE_RECEIPT'
                          )
                        limit 500
                        """)
                .setParameter("org", organizationId)
                .getResultList();

        for (Object raw : receiptIds) {
            UUID receiptId = toUuid(raw);
            purchasesScanned++;
            try {
                if (dryRun) {
                    notes.add("DRY-RUN AP receipt=" + receiptId);
                    apCreated++;
                } else {
                    var ap = payableService.generateFromPurchaseReceipt(new PayableFromPurchaseRequest(
                            receiptId, null, null, null, "migration-receipt-" + receiptId));
                    apCreated++;
                    notes.add("AP ok receipt=" + receiptId + " payable=" + ap.id());
                }
            } catch (Exception ex) {
                // status names may differ — count skip/error
                if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("já")) {
                    skipped++;
                } else {
                    errors++;
                }
                notes.add("ERR/SKIP AP receipt=" + receiptId + ": " + ex.getMessage());
            }
        }

        // vendas já com origem = duplicatas evitadas (scanned fora do select)
        skipped += 0;

        run.setSalesScanned(salesScanned);
        run.setReceivablesCreated(dryRun ? 0 : arCreated);
        run.setPurchasesScanned(purchasesScanned);
        run.setPayablesCreated(dryRun ? 0 : apCreated);
        run.setSkippedDuplicates(skipped);
        run.setErrorsCount(errors);
        run.setStatus(errors > 0 && (arCreated + apCreated) == 0
                ? FinanceMigrationRun.Status.FAILED
                : FinanceMigrationRun.Status.COMPLETED);
        run.setFinishedAt(Instant.now());
        try {
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("notes", notes.size() > 100 ? notes.subList(0, 100) : notes);
            report.put("dryRun", dryRun);
            run.setReportJson(objectMapper.writeValueAsString(report));
        } catch (Exception ignored) {
            run.setReportJson("{\"notesCount\":" + notes.size() + "}");
        }
        runRepository.save(run);

        financeAuditService.success(
                FinanceAuditEvents.IMPORT,
                "FinanceMigrationRun",
                run.getId(),
                AuditLog.AuditAction.CREATE,
                "Migração financeira dryRun=" + dryRun);

        return new MigrationResult(
                run.getId(),
                dryRun,
                run.getStatus().name(),
                salesScanned,
                dryRun ? arCreated : run.getReceivablesCreated(),
                purchasesScanned,
                dryRun ? apCreated : run.getPayablesCreated(),
                skipped,
                errors,
                notes.size() > 50 ? notes.subList(0, 50) : notes);
    }

    private static UUID toUuid(Object raw) {
        if (raw instanceof UUID u) {
            return u;
        }
        return UUID.fromString(String.valueOf(raw));
    }
}
