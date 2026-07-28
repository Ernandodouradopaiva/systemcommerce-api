package br.com.systemcommerce.integration.entity;

public enum IntegrationJobStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    FAILED_DEAD_LETTER,
    CANCELLED
}
