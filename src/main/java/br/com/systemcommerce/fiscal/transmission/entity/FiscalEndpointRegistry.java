package br.com.systemcommerce.fiscal.transmission.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "fiscal_endpoint_registry")
public class FiscalEndpointRegistry extends AuditableEntity {

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "model", nullable = false, length = 10)
    private String model;

    @Column(name = "environment", nullable = false, length = 20)
    private String environment;

    @Column(name = "service_name", nullable = false, length = 60)
    private String serviceName;

    @Column(name = "url", nullable = false, length = 500)
    private String url;

    @Column(name = "timeout_ms", nullable = false)
    private Integer timeoutMs = 30000;
}
