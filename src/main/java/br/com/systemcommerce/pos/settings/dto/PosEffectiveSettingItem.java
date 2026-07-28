package br.com.systemcommerce.pos.settings.dto;

import br.com.systemcommerce.pos.settings.entity.PosSettingScope;
import br.com.systemcommerce.pos.settings.entity.PosSettingValueType;
import java.util.UUID;

/** Item da configuração efetiva resolvida (terminal > loja > global > default). */
public record PosEffectiveSettingItem(
        String settingKey,
        PosSettingValueType valueType,
        String category,
        String label,
        boolean critical,
        String value,
        PosSettingScope resolvedFrom,
        UUID overrideId,
        String defaultValue) {}
