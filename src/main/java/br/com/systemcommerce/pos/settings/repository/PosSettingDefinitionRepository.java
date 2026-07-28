package br.com.systemcommerce.pos.settings.repository;

import br.com.systemcommerce.pos.settings.entity.PosSettingDefinition;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PosSettingDefinitionRepository extends JpaRepository<PosSettingDefinition, UUID> {

    Optional<PosSettingDefinition> findBySettingKeyAndActiveTrue(String settingKey);

    List<PosSettingDefinition> findByActiveTrueOrderBySortOrderAsc();

    boolean existsBySettingKey(String settingKey);
}
