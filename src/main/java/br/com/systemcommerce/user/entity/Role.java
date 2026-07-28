package br.com.systemcommerce.user.entity;

import br.com.systemcommerce.organization.entity.Organization;
import br.com.systemcommerce.shared.audit.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Grupo de usuários (AccessGroup). Nome técnico da tabela permanece {@code roles}
 * conforme decisão documentada em ACCESS_CONTROL_ARCHITECTURE.md.
 */
@Getter
@Setter
@Entity
@Table(name = "roles")
public class Role extends AuditableEntity {

    public enum GroupType {
        SYSTEM,
        ADMINISTRATIVE,
        MANAGERIAL,
        OPERATIONAL,
        CUSTOM
    }

    public enum DefaultScope {
        GLOBAL_SYSTEM,
        ORGANIZATION,
        STORE_GROUP,
        STORE,
        OWN_RECORDS,
        TEAM_RECORDS
    }

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "group_type", nullable = false, length = 20)
    private GroupType groupType = GroupType.OPERATIONAL;

    @Column(name = "system_group", nullable = false)
    private Boolean systemGroup = Boolean.FALSE;

    @Column(name = "default_group", nullable = false)
    private Boolean defaultGroup = Boolean.FALSE;

    @Enumerated(EnumType.STRING)
    @Column(name = "default_scope", nullable = false, length = 30)
    private DefaultScope defaultScope = DefaultScope.ORGANIZATION;

    @Column(name = "allows_administration", nullable = false)
    private Boolean allowsAdministration = Boolean.FALSE;

    @Column(name = "visual_priority", nullable = false)
    private Integer visualPriority = 100;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions = new HashSet<>();
}
