package br.com.systemcommerce.pos.cash.dto;

import br.com.systemcommerce.pos.cash.entity.CashMovement;
import java.math.BigDecimal;

public record CashMovementTypeTotal(CashMovement.MovementType type, BigDecimal amount) {}
