package br.com.systemcommerce.webhook.entity;

public enum WebhookDeliveryStatus {
    PENDING,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED,
    DEAD_LETTER
}
