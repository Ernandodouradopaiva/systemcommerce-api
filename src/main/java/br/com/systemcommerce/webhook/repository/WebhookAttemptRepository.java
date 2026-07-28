package br.com.systemcommerce.webhook.repository;

import br.com.systemcommerce.webhook.entity.WebhookAttempt;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebhookAttemptRepository extends JpaRepository<WebhookAttempt, UUID> {}
