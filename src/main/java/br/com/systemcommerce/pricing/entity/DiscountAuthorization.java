package br.com.systemcommerce.pricing.entity;



import br.com.systemcommerce.sale.entity.Sale;

import br.com.systemcommerce.sale.entity.SaleItem;

import br.com.systemcommerce.shared.audit.AuditableEntity;

import br.com.systemcommerce.user.entity.User;

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

@Table(name = "discount_authorizations")

public class DiscountAuthorization extends AuditableEntity {



    public enum Status {

        PENDING,

        APPROVED,

        DENIED

    }



    @ManyToOne(fetch = FetchType.LAZY, optional = false)

    @JoinColumn(name = "sale_id", nullable = false)

    private Sale sale;



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "sale_item_id")

    private SaleItem saleItem;



    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 2)

    private BigDecimal requestedAmount;



    @Column(name = "requested_percent", precision = 7, scale = 4)

    private BigDecimal requestedPercent;



    @Enumerated(EnumType.STRING)

    @Column(name = "status", nullable = false, length = 20)

    private Status status = Status.PENDING;



    @Column(name = "request_reason", length = 500)

    private String requestReason;



    @Column(name = "decision_notes", length = 500)

    private String decisionNotes;



    @ManyToOne(fetch = FetchType.LAZY, optional = false)

    @JoinColumn(name = "requested_by_id", nullable = false)

    private User requestedBy;



    @ManyToOne(fetch = FetchType.LAZY)

    @JoinColumn(name = "decided_by_id")

    private User decidedBy;



    @Column(name = "decided_at")

    private Instant decidedAt;

}


