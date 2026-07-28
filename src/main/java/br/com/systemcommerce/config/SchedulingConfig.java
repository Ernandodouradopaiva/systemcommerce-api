package br.com.systemcommerce.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Habilita {@code @Scheduled} — usado pela expiração automática de reservas de estoque (Prompt 70). */
@Configuration
@EnableScheduling
public class SchedulingConfig {}
