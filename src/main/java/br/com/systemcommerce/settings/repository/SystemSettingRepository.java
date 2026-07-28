package br.com.systemcommerce.settings.repository;

import br.com.systemcommerce.settings.entity.SystemSetting;
import br.com.systemcommerce.settings.entity.SystemSettingScope;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SystemSettingRepository extends JpaRepository<SystemSetting, UUID> {

    @Query(
            """
            SELECT s FROM SystemSetting s
            WHERE s.settingKey = :key
              AND s.scope = :scope
              AND s.active = TRUE
              AND (:organizationId IS NULL OR s.organization.id = :organizationId)
              AND (:storeGroupId IS NULL OR s.storeGroup.id = :storeGroupId)
              AND (:storeId IS NULL OR s.store.id = :storeId)
              AND (:terminalId IS NULL OR s.terminal.id = :terminalId)
              AND (:userId IS NULL OR s.user.id = :userId)
            """)
    Optional<SystemSetting> findActiveOverride(
            @Param("key") String key,
            @Param("scope") SystemSettingScope scope,
            @Param("organizationId") UUID organizationId,
            @Param("storeGroupId") UUID storeGroupId,
            @Param("storeId") UUID storeId,
            @Param("terminalId") UUID terminalId,
            @Param("userId") UUID userId);

    @Query(
            """
            SELECT s FROM SystemSetting s
            WHERE s.organization.id = :organizationId
              AND s.scope = 'ORGANIZATION'
              AND s.active = TRUE
            """)
    List<SystemSetting> findActiveByOrganization(@Param("organizationId") UUID organizationId);

    @Query(
            """
            SELECT s FROM SystemSetting s
            WHERE s.storeGroup.id = :storeGroupId
              AND s.scope = 'STORE_GROUP'
              AND s.active = TRUE
            """)
    List<SystemSetting> findActiveByStoreGroup(@Param("storeGroupId") UUID storeGroupId);

    @Query(
            """
            SELECT s FROM SystemSetting s
            WHERE s.store.id = :storeId
              AND s.scope = 'STORE'
              AND s.active = TRUE
            """)
    List<SystemSetting> findActiveByStore(@Param("storeId") UUID storeId);

    @Query(
            """
            SELECT s FROM SystemSetting s
            WHERE s.terminal.id = :terminalId
              AND s.scope = 'TERMINAL'
              AND s.active = TRUE
            """)
    List<SystemSetting> findActiveByTerminal(@Param("terminalId") UUID terminalId);

    @Query(
            """
            SELECT s FROM SystemSetting s
            WHERE s.user.id = :userId
              AND s.scope = 'USER'
              AND s.active = TRUE
            """)
    List<SystemSetting> findActiveByUser(@Param("userId") UUID userId);
}
