package br.com.systemcommerce.integration.service;

import br.com.systemcommerce.integration.adapter.MarketplaceAdapter;
import br.com.systemcommerce.integration.dto.IntegrationJobCreateRequest;
import br.com.systemcommerce.integration.dto.IntegrationJobResponse;
import br.com.systemcommerce.integration.entity.IntegrationError;
import br.com.systemcommerce.integration.entity.IntegrationJob;
import br.com.systemcommerce.integration.entity.IntegrationJobStatus;
import br.com.systemcommerce.integration.entity.MarketplaceAccount;
import br.com.systemcommerce.integration.entity.SynchronizationCheckpoint;
import br.com.systemcommerce.integration.repository.IntegrationErrorRepository;
import br.com.systemcommerce.integration.repository.IntegrationJobRepository;
import br.com.systemcommerce.integration.repository.SynchronizationCheckpointRepository;
import br.com.systemcommerce.organization.service.OrganizationService;
import br.com.systemcommerce.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntegrationJobService {

    private final IntegrationJobRepository jobRepository;
    private final IntegrationErrorRepository errorRepository;
    private final SynchronizationCheckpointRepository checkpointRepository;
    private final OrganizationService organizationService;
    private final IntegrationHubService hubService;
    private final ChannelOrderIngestionService orderIngestionService;

    @Transactional(readOnly = true)
    public Page<IntegrationJobResponse> list(UUID organizationId, Pageable pageable) {
        return jobRepository
                .findAll(
                        (root, q, cb) ->
                                organizationId == null
                                        ? cb.conjunction()
                                        : cb.equal(root.get("organization").get("id"), organizationId),
                        pageable)
                .map(this::toResponse);
    }

    @Transactional
    public IntegrationJobResponse enqueue(IntegrationJobCreateRequest request) {
        IntegrationJob job = new IntegrationJob();
        job.setOrganization(organizationService.resolveForStoreCreate(request.organizationId()));
        if (request.marketplaceAccountId() != null) {
            job.setMarketplaceAccount(hubService.getAccountEntity(request.marketplaceAccountId()));
        }
        job.setJobType(request.jobType().trim().toUpperCase());
        job.setPayloadJson(request.payloadJson());
        job.setStatus(IntegrationJobStatus.PENDING);
        job.setAttemptCount(0);
        job.setMaxAttempts(request.maxAttempts() != null && request.maxAttempts() > 0 ? request.maxAttempts() : 5);
        job.setNextAttemptAt(Instant.now());
        return toResponse(jobRepository.save(job));
    }

    @Scheduled(fixedDelayString = "${systemcommerce.integration.job-poll-ms:15000}")
    @Transactional
    public void processDueJobs() {
        List<IntegrationJob> due = jobRepository.findDueJobs(
                EnumSet.of(IntegrationJobStatus.PENDING, IntegrationJobStatus.FAILED), Instant.now());
        for (IntegrationJob job : due) {
            try {
                runJob(job);
            } catch (Exception ex) {
                log.warn("Falha no job de integração {}: {}", job.getId(), ex.getMessage());
                failJob(job, ex.getMessage());
            }
        }
    }

    @Transactional
    public void runJob(IntegrationJob job) {
        job.setStatus(IntegrationJobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        job.setAttemptCount(job.getAttemptCount() + 1);
        jobRepository.save(job);

        MarketplaceAccount account = job.getMarketplaceAccount();
        if (account == null) {
            throw new IllegalStateException("Job sem conta de marketplace");
        }
        MarketplaceAdapter adapter = hubService.resolveAdapter(account);

        switch (job.getJobType()) {
            case "SYNC_ORDERS" -> {
                String cursor = checkpointRepository
                        .findByMarketplaceAccountIdAndSyncType(account.getId(), "ORDERS")
                        .map(SynchronizationCheckpoint::getCursorValue)
                        .orElse(null);
                List<MarketplaceAdapter.ExternalOrder> orders = adapter.fetchOrders(account, cursor);
                for (MarketplaceAdapter.ExternalOrder external : orders) {
                    var channelOrder = orderIngestionService.ingestExternalOrder(
                            account.getId(), external, "job:" + job.getId() + ":" + external.externalOrderId());
                    if (channelOrder.status() != br.com.systemcommerce.integration.entity.ChannelOrderStatus.CONVERTED
                            && channelOrder.salesOrderId() == null) {
                        orderIngestionService.convertToSalesOrder(channelOrder.id());
                    }
                }
                upsertCheckpoint(account, "ORDERS", Instant.now().toString());
            }
            case "SYNC_LISTINGS" -> adapter.fetchListings(account, null);
            case "AUTH_REFRESH" -> {
                String creds = hubService.decryptCredentials(account);
                adapter.authenticate(account, creds != null ? creds : "{}");
            }
            default -> throw new IllegalStateException("Tipo de job não suportado: " + job.getJobType());
        }

        job.setStatus(IntegrationJobStatus.SUCCEEDED);
        job.setFinishedAt(Instant.now());
        job.setLastError(null);
        jobRepository.save(job);
        account.setLastSyncAt(Instant.now());
    }

    void failJob(IntegrationJob job, String message) {
        job.setLastError(message != null && message.length() > 2000 ? message.substring(0, 2000) : message);
        IntegrationError error = new IntegrationError();
        error.setOrganization(job.getOrganization());
        error.setMarketplaceAccount(job.getMarketplaceAccount());
        error.setIntegrationJob(job);
        error.setErrorCode("JOB_FAILED");
        error.setMessage(job.getLastError() != null ? job.getLastError() : "Erro desconhecido");
        errorRepository.save(error);

        if (job.getAttemptCount() >= job.getMaxAttempts()) {
            job.setStatus(IntegrationJobStatus.FAILED_DEAD_LETTER);
            job.setFinishedAt(Instant.now());
        } else {
            job.setStatus(IntegrationJobStatus.FAILED);
            long backoffSeconds = (long) Math.pow(2, Math.min(job.getAttemptCount(), 6)) * 30L;
            job.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds));
        }
        jobRepository.save(job);
    }

    private void upsertCheckpoint(MarketplaceAccount account, String syncType, String cursor) {
        SynchronizationCheckpoint cp = checkpointRepository
                .findByMarketplaceAccountIdAndSyncType(account.getId(), syncType)
                .orElseGet(() -> {
                    SynchronizationCheckpoint n = new SynchronizationCheckpoint();
                    n.setOrganization(account.getOrganization());
                    n.setMarketplaceAccount(account);
                    n.setSyncType(syncType);
                    return n;
                });
        cp.setCursorValue(cursor);
        cp.setLastSuccessAt(Instant.now());
        cp.setUpdatedAt(Instant.now());
        checkpointRepository.save(cp);
    }

    private IntegrationJobResponse toResponse(IntegrationJob j) {
        return new IntegrationJobResponse(
                j.getId(),
                j.getOrganization().getId(),
                j.getMarketplaceAccount() != null ? j.getMarketplaceAccount().getId() : null,
                j.getJobType(),
                j.getStatus(),
                j.getAttemptCount(),
                j.getMaxAttempts(),
                j.getNextAttemptAt(),
                j.getLastError(),
                j.getStartedAt(),
                j.getFinishedAt());
    }

    @Transactional(readOnly = true)
    public IntegrationJobResponse getById(UUID id) {
        return toResponse(jobRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Job de integração não encontrado")));
    }
}
