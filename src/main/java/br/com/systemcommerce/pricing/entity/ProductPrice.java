package br.com.systemcommerce.pricing.entity;



import br.com.systemcommerce.customer.entity.Customer;

import br.com.systemcommerce.product.entity.Product;

import br.com.systemcommerce.shared.audit.AuditableEntity;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.FetchType;

import jakarta.persistence.JoinColumn;

import jakarta.persistence.ManyToOne;

import jakarta.persistence.Table;

import java.math.BigDecimal;

import java.time.Instant;

import lombok.Getter;

import lombok.Setter;



@Getter

@Setter

@Entity

@Table(name = "product_prices")

public class ProductPrice extends AuditableEntity {



    public enum PriceType {

        STANDARD,

        PROMOTIONAL

    }



    public enum Status {

        ACTIVE,

        INACTIVE

    }



    @ManyToOne(fetch = FetchType.LAZY, optional = false)

    @JoinColumn(name = "price_table_id", nullable = false)

    private PriceTable priceTable;



    @ManyToOne(fetch = FetchType.LAZY, optional = false)

    @JoinColumn(name = "product_id", nullable = false)

    private Product product;



    @Enumerated(EnumType.STRING)

    @Column(name = "price_type", nullable = false, length = 20)

    private PriceType priceType = PriceType.STANDARD;



    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)

    private BigDecimal unitPrice;



    @Column(name = "min_quantity", nullable = false, precision = 19, scale = 3)

    private BigDecimal minQuantity = BigDecimal.ZERO;



    @Column(name = "priority", nullable = false)

    private Integer priority = 0;



    @Enumerated(EnumType.STRING)

    @Column(name = "status", nullable = false, length = 20)

    private Status status = Status.ACTIVE;



    @Column(name = "valid_from")

    private Instant validFrom;



    @Column(name = "valid_to")

    private Instant validTo;

    /** Preço específico de cliente (Prompt 68) — quando informado, tem prioridade máxima na resolução. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public boolean isUsable() {

        return Boolean.TRUE.equals(getActive()) && status == Status.ACTIVE;

    }



    public boolean isValidAt(Instant at) {

        if (validFrom != null && at.isBefore(validFrom)) {

            return false;

        }

        return validTo == null || !at.isAfter(validTo);

    }



    public boolean meetsMinQuantity(BigDecimal quantity) {

        BigDecimal min = minQuantity != null ? minQuantity : BigDecimal.ZERO;

        return quantity != null && quantity.compareTo(min) >= 0;

    }

}


