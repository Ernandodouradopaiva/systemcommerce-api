package br.com.systemcommerce.shipment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/** Volume/pacote de uma expedição — tabela simples sem auditoria completa (Prompt 72). */
@Getter
@Setter
@Entity
@Table(name = "shipment_packages")
public class ShipmentPackage {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @Column(name = "package_number", nullable = false)
    private Integer packageNumber;

    @Column(name = "weight", precision = 18, scale = 4)
    private BigDecimal weight;

    @Column(name = "length_cm", precision = 18, scale = 4)
    private BigDecimal lengthCm;

    @Column(name = "width_cm", precision = 18, scale = 4)
    private BigDecimal widthCm;

    @Column(name = "height_cm", precision = 18, scale = 4)
    private BigDecimal heightCm;

    @Column(name = "tracking_code", length = 80)
    private String trackingCode;

    @PrePersist
    void onPrePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
    }
}
