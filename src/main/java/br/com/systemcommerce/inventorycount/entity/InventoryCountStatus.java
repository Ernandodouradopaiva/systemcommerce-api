package br.com.systemcommerce.inventorycount.entity;

public enum InventoryCountStatus {
    PLANNED,
    OPEN,
    COUNTING,
    RECOUNTING,
    UNDER_ANALYSIS,
    APPROVED,
    POSTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == POSTED || this == CANCELLED;
    }
}
