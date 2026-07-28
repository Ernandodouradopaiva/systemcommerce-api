package br.com.systemcommerce.pos.settings.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.terminal.entity.PosTerminal;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "pos_settings")
public class PosSetting extends AuditableEntity {

    @Column(name = "setting_key", nullable = false, length = 80)
    private String settingKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private PosSettingScope scope;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id")
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terminal_id")
    private PosTerminal terminal;

    @Column(name = "value_text", nullable = false, columnDefinition = "TEXT")
    private String valueText;
}
