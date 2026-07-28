package br.com.systemcommerce.pricing.entity;



import br.com.systemcommerce.shared.audit.AuditableEntity;

import br.com.systemcommerce.user.entity.Role;

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

@Table(name = "operator_discount_limits")

public class OperatorDiscountLimit extends AuditableEntity {



    @ManyToOne(fetch = FetchType.LAZY, optional = false)

    @JoinColumn(name = "role_id", nullable = false, unique = true)

    private Role role;



    @Column(name = "max_percent", nullable = false, precision = 7, scale = 4)

    private BigDecimal maxPercent;



    @Column(name = "max_amount", precision = 19, scale = 2)

    private BigDecimal maxAmount;

}


