package br.com.systemcommerce.webhook.repository;

import br.com.systemcommerce.webhook.entity.WebhookSecret;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookSecretRepository extends JpaRepository<WebhookSecret, UUID> {}
