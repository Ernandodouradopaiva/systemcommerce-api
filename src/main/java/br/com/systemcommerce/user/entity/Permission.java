package br.com.systemcommerce.user.entity;

import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "permissions")
public class Permission extends AuditableEntity {

    @Column(name = "code", nullable = false, unique = true, length = 80)
    private String code;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "module", nullable = false, length = 50)
    private String module;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "module_id")
    private java.util.UUID moduleId;

    @Column(name = "resource_id")
    private java.util.UUID resourceId;

    @Column(name = "action_id")
    private java.util.UUID actionId;

    @Column(name = "system_permission", nullable = false)
    private Boolean systemPermission = Boolean.TRUE;

    @Column(name = "code_immutable", nullable = false)
    private Boolean codeImmutable = Boolean.TRUE;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel = "MEDIUM";

    @Column(name = "requires_justification", nullable = false)
    private Boolean requiresJustification = Boolean.FALSE;

    @Column(name = "requires_dual_approval", nullable = false)
    private Boolean requiresDualApproval = Boolean.FALSE;

    @Column(name = "sensitive", nullable = false)
    private Boolean sensitive = Boolean.FALSE;
}
