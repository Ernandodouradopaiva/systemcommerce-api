package br.com.systemcommerce.pos.settings.repository;

import br.com.systemcommerce.pos.settings.entity.PosSettingHistory;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosSettingHistoryRepository extends JpaRepository<PosSettingHistory, UUID> {

    @Query(
            """
            SELECT h FROM PosSettingHistory h
            WHERE (:key IS NULL OR h.settingKey = :key)
              AND (:storeId IS NULL OR h.storeId = :storeId)
              AND (:terminalId IS NULL OR h.terminalId = :terminalId)
            """)
    Page<PosSettingHistory> search(
            @Param("key") String key,
            @Param("storeId") UUID storeId,
            @Param("terminalId") UUID terminalId,
            Pageable pageable);
}
