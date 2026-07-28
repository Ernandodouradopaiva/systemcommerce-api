package br.com.systemcommerce.pricing.entity;



import br.com.systemcommerce.product.entity.Category;

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

@Table(name = "discount_policies")

public class DiscountPolicy extends AuditableEntity {



    public enum AppliesTo {

        GLOBAL,

        PRODUCT,

        CATEGORY

    }



    public enum Status {

        ACTIVE,

        INACTIVE

    }



    @Column(name = "code", nullable = false, unique = true, length = 40)

    private String code;



    @Column(name = "name", nullable = false, length = 200)

    private String name;



    @Column(name = "description", length = 1000)

    private String description;



    @Enumerated(EnumType.STRING)

    @Column(name = "applies_to", nullable = false, length = 20)

    private AppliesTo appliesTo;



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "product_id")

    private Product product;



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "category_id")

    private Category category;



    @Column(name = "max_percent", nullable = false, precision = 7, scale = 4)

    private BigDecimal maxPercent;



    @Column(name = "max_amount", precision = 19, scale = 2)

    private BigDecimal maxAmount;



    @Column(name = "priority", nullable = false)

    private Integer priority = 0;



    @Enumerated(EnumType.STRING)

    @Column(name = "status", nullable = false, length = 20)

    private Status status = Status.ACTIVE;



    @Column(name = "valid_from")

    private Instant validFrom;



    @Column(name = "valid_to")

    private Instant validTo;



    public boolean isUsable() {

        return Boolean.TRUE.equals(getActive()) && status == Status.ACTIVE;

    }



    public boolean isValidAt(Instant at) {

        if (validFrom != null && at.isBefore(validFrom)) {

            return false;

        }

        return validTo == null || !at.isAfter(validTo);

    }

}


