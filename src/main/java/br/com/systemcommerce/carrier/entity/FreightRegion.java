package br.com.systemcommerce.carrier.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "freight_regions")
public class FreightRegion extends AuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "freight_table_id", nullable = false)
    private FreightTable freightTable;

    @Column(name = "region_code", nullable = false, length = 40)
    private String regionCode;

    @Column(name = "region_name", length = 120)
    private String regionName;

    @Column(name = "zip_from", length = 10)
    private String zipFrom;

    @Column(name = "zip_to", length = 10)
    private String zipTo;

    @Column(name = "min_weight", precision = 18, scale = 4)
    private BigDecimal minWeight;

    @Column(name = "max_weight", precision = 18, scale = 4)
    private BigDecimal maxWeight;

    @Column(name = "min_volume", precision = 18, scale = 4)
    private BigDecimal minVolume;

    @Column(name = "max_volume", precision = 18, scale = 4)
    private BigDecimal maxVolume;

    @Column(name = "min_order_amount", precision = 18, scale = 2)
    private BigDecimal minOrderAmount;

    @Column(name = "freight_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal freightAmount;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    /** Verifica se a região atende ao CEP informado (comparação lexicográfica de prefixo/faixa simples). */
    public boolean matchesZip(String zip) {
        if (zip == null) {
            return zipFrom == null && zipTo == null;
        }
        String normalized = zip.replaceAll("\\D", "");
        if (zipFrom == null && zipTo == null) {
            return true;
        }
        String from = zipFrom != null ? zipFrom.replaceAll("\\D", "") : "0";
        String to = zipTo != null ? zipTo.replaceAll("\\D", "") : "99999999";
        String padded = padZip(normalized);
        return padZip(from).compareTo(padded) <= 0 && padded.compareTo(padZip(to)) <= 0;
    }

    private String padZip(String value) {
        StringBuilder sb = new StringBuilder(value);
        while (sb.length() < 8) {
            sb.append('0');
        }
        return sb.toString();
    }

    public boolean matchesWeight(BigDecimal weight) {
        if (weight == null) {
            return minWeight == null && maxWeight == null;
        }
        if (minWeight != null && weight.compareTo(minWeight) < 0) {
            return false;
        }
        return maxWeight == null || weight.compareTo(maxWeight) <= 0;
    }

    public boolean matchesOrderAmount(BigDecimal orderAmount) {
        if (minOrderAmount == null) {
            return true;
        }
        return orderAmount != null && orderAmount.compareTo(minOrderAmount) >= 0;
    }
}
