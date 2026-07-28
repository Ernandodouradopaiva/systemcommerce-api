package br.com.systemcommerce.pos.cash.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PosCashProperties.class)
public class PosCashConfig {}
