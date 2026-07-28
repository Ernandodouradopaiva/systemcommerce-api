package br.com.systemcommerce.pos.settings.repository;

import br.com.systemcommerce.pos.settings.entity.PosSetting;
import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PosSettingRepository extends JpaRepository<PosSetting, UUID>, JpaSpecificationExecutor<PosSetting> {

    @Query(
            """
            SELECT s FROM PosSetting s
            LEFT JOIN FETCH s.store
            LEFT JOIN FETCH s.terminal
            WHERE s.id = :id AND s.active = TRUE
            """)
    Optional<PosSetting> findActiveDetailedById(@Param("id") UUID id);

    List<PosSetting> findByScopeAndActiveTrue(PosSettingScope scope);

    @Query(
            """
            SELECT s FROM PosSetting s
            WHERE s.active = TRUE
              AND s.scope = br.com.systemcommerce.pos.settings.entity.PosSettingScope.GLOBAL
            """)
    List<PosSetting> findActiveGlobals();

    @Query(
            """
            SELECT s FROM PosSetting s
            WHERE s.active = TRUE
              AND s.scope = br.com.systemcommerce.pos.settings.entity.PosSettingScope.STORE
              AND s.store.id = :storeId
            """)
    List<PosSetting> findActiveByStore(@Param("storeId") UUID storeId);

    @Query(
            """
            SELECT s FROM PosSetting s
            WHERE s.active = TRUE
              AND s.scope = br.com.systemcommerce.pos.settings.entity.PosSettingScope.TERMINAL
              AND s.terminal.id = :terminalId
            """)
    List<PosSetting> findActiveByTerminal(@Param("terminalId") UUID terminalId);

    @Query(
            """
            SELECT s FROM PosSetting s
            WHERE s.active = TRUE
              AND s.settingKey = :key
              AND s.scope = :scope
              AND ((:storeId IS NULL AND s.store IS NULL) OR s.store.id = :storeId)
              AND ((:terminalId IS NULL AND s.terminal IS NULL) OR s.terminal.id = :terminalId)
            """)
    Optional<PosSetting> findActiveOverride(
            @Param("key") String key,
            @Param("scope") PosSettingScope scope,
            @Param("storeId") UUID storeId,
            @Param("terminalId") UUID terminalId);
}
