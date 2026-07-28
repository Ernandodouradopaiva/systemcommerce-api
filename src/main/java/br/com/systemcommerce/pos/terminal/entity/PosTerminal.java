package br.com.systemcommerce.pos.terminal.entity;

import br.com.systemcommerce.pos.store.entity.Store;
import br.com.systemcommerce.pos.warehouse.entity.Warehouse;
import br.com.systemcommerce.seller.entity.SellerProfile;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "pos_terminals",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_pos_terminals_store_code", columnNames = {"store_id", "code"}),
            @UniqueConstraint(name = "uk_pos_terminals_store_number", columnNames = {"store_id", "terminal_number"})
        })
public class PosTerminal extends AuditableEntity {

    public enum TerminalStatus {
        ACTIVE,
        INACTIVE
    }

    public enum PrintModel {
        NONE,
        THERMAL_58,
        THERMAL_80,
        A4
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "terminal_number", nullable = false)
    private Integer terminalNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TerminalStatus status = TerminalStatus.ACTIVE;

    @Column(name = "station_identifier", length = 100)
    private String stationIdentifier;

    @Column(name = "printer_name", length = 200)
    private String printerName;

    @Enumerated(EnumType.STRING)
    @Column(name = "print_model", nullable = false, length = 40)
    private PrintModel printModel = PrintModel.NONE;

    @Column(name = "last_communication_at")
    private Instant lastCommunicationAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_seller_profile_id")
    private SellerProfile defaultSellerProfile;

    public boolean isUsable() {
        return Boolean.TRUE.equals(getActive()) && status == TerminalStatus.ACTIVE;
    }

    /**
     * Regras para abertura de caixa (sessão será criada em prompt seguinte).
     * Terminal ativo + loja ativa + depósito ativo autorizado para venda.
     */
    public boolean isEligibleToOpenCashSession() {
        return isUsable()
                && store != null
                && store.isUsable()
                && warehouse != null
                && warehouse.isEligibleForPosSale();
    }

    public void markActive() {
        this.status = TerminalStatus.ACTIVE;
        setActive(true);
    }

    public void markInactive() {
        this.status = TerminalStatus.INACTIVE;
        setActive(false);
    }
}
