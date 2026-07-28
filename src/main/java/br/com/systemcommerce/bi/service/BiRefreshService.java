package br.com.systemcommerce.bi.service;

import br.com.systemcommerce.bi.entity.BiRefreshLog;
import br.com.systemcommerce.bi.repository.BiRefreshLogRepository;
import br.com.systemcommerce.dashboard.executive.support.ExecutiveDashboardCache;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BiRefreshService {

    private static final List<String> MATERIALIZED_VIEWS = List.of(
            "bi_fact_sales_daily",
            "bi_fact_inventory_snapshot",
            "bi_fact_purchases_daily");

    private final JdbcTemplate jdbc;
    private final BiRefreshLogRepository refreshLogRepository;
    private final ExecutiveDashboardCache executiveDashboardCache;

    @Scheduled(cron = "${systemcommerce.bi.refresh-cron:0 0 2 * * *}")
    @Transactional
    public void scheduledRefresh() {
        refreshAll();
    }

    @Transactional
    public List<BiRefreshLog> refreshAll() {
        return MATERIALIZED_VIEWS.stream().map(this::refreshObject).toList();
    }

    @Transactional
    public BiRefreshLog refreshObject(String objectName) {
        BiRefreshLog entry = new BiRefreshLog();
        entry.setObjectName(objectName);
        entry.setRefreshType("MATERIALIZED_VIEW");
        entry.setStartedAt(Instant.now());
        entry.setStatus("RUNNING");
        refreshLogRepository.save(entry);
        try {
            jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY " + objectName);
            Long rows = jdbc.queryForObject("SELECT COUNT(*) FROM " + objectName, Long.class);
            entry.setRowsAffected(rows);
            entry.setStatus("SUCCEEDED");
            entry.setFinishedAt(Instant.now());
            executiveDashboardCache.invalidateAll();
        } catch (Exception ex) {
            entry.setStatus("FAILED");
            entry.setErrorMessage(ex.getMessage() != null && ex.getMessage().length() > 2000
                    ? ex.getMessage().substring(0, 2000)
                    : ex.getMessage());
            entry.setFinishedAt(Instant.now());
            log.warn("Falha ao refresh BI {}: {}", objectName, ex.getMessage());
        }
        return refreshLogRepository.save(entry);
    }

    @Transactional(readOnly = true)
    public List<BiRefreshLog> recentLogs() {
        return refreshLogRepository.findTop20ByOrderByStartedAtDesc();
    }
}
