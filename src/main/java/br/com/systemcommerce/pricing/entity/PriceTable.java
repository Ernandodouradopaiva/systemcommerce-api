package br.com.systemcommerce.pricing.entity;



import br.com.systemcommerce.pos.store.entity.Store;

import br.com.systemcommerce.shared.audit.AuditableEntity;

import jakarta.persistence.Column;

import jakarta.persistence.Entity;

import jakarta.persistence.EnumType;

import jakarta.persistence.Enumerated;

import jakarta.persistence.FetchType;

import jakarta.persistence.JoinColumn;

import jakarta.persistence.JoinTable;

import jakarta.persistence.ManyToMany;

import jakarta.persistence.ManyToOne;

import jakarta.persistence.OneToMany;

import jakarta.persistence.Table;

import java.time.Instant;

import java.util.HashSet;

import java.util.Set;

import lombok.Getter;

import lombok.Setter;



@Getter

@Setter

@Entity

@Table(name = "price_tables")

public class PriceTable extends AuditableEntity {



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

    @Column(name = "status", nullable = false, length = 20)

    private Status status = Status.ACTIVE;



    @Column(name = "priority", nullable = false)

    private Integer priority = 0;



    @Column(name = "valid_from")

    private Instant validFrom;



    @Column(name = "valid_to")

    private Instant validTo;



    @Enumerated(EnumType.STRING)

    @Column(name = "channel", nullable = false, length = 20)

    private PriceChannel channel = PriceChannel.ERP;



    @Enumerated(EnumType.STRING)

    @Column(name = "scope_type", nullable = false, length = 20)

    private PriceTableScopeType scopeType = PriceTableScopeType.GLOBAL;



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "store_group_id")

    private StoreGroup storeGroup;



    @ManyToMany(fetch = FetchType.LAZY)

    @JoinTable(

            name = "price_table_stores",

            joinColumns = @JoinColumn(name = "price_table_id"),

            inverseJoinColumns = @JoinColumn(name = "store_id"))

    private Set<Store> stores = new HashSet<>();



    @OneToMany(mappedBy = "priceTable", fetch = FetchType.LAZY)

    private Set<ProductPrice> productPrices = new HashSet<>();



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


