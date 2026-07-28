package br.com.systemcommerce.inventory.service;

import br.com.systemcommerce.inventory.entity.Inventory;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Fórmulas oficiais de saldo de estoque (fonte da verdade na API).
 *
 * <ul>
 *   <li>saldo físico = quantity (on-hand)
 *   <li>saldo reservado = quantityReserved
 *   <li>saldo em trânsito = quantityInTransit
 *   <li>saldo disponível = físico − reservado − bloqueado
 * </ul>
 */
public final class InventoryBalanceFormulas {

    private InventoryBalanceFormulas() {}

    public static BigDecimal physical(Inventory inventory) {
        return nz(inventory.getQuantity());
    }

    public static BigDecimal reserved(Inventory inventory) {
        return nz(inventory.getQuantityReserved());
    }

    public static BigDecimal blocked(Inventory inventory) {
        return nz(inventory.getQuantityBlocked());
    }

    public static BigDecimal inTransit(Inventory inventory) {
        return nz(inventory.getQuantityInTransit());
    }

    /** Quantidade liberada para venda/expedição neste depósito. */
    public static BigDecimal available(Inventory inventory) {
        return physical(inventory).subtract(reserved(inventory)).subtract(blocked(inventory));
    }

    public static BigDecimal scale(BigDecimal value) {
        return nz(value).setScale(3, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
