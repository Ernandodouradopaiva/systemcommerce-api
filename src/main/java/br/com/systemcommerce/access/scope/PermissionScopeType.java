package br.com.systemcommerce.access.scope;

/**
 * Escopos de autorização (Prompt 156).
 * Quanto maior {@link #breadthRank()}, mais amplo — absorve escopos mais restritos
 * da mesma permissão na consolidação.
 */
public enum PermissionScopeType {
    GLOBAL_SYSTEM(100),
    ORGANIZATION(80),
    STORE_GROUP(60),
    STORE(40),
    TEAM_RECORDS(20),
    OWN_RECORDS(10);

    private final int breadthRank;

    PermissionScopeType(int breadthRank) {
        this.breadthRank = breadthRank;
    }

    public int breadthRank() {
        return breadthRank;
    }

    public boolean absorbs(PermissionScopeType other) {
        return other != null && this.breadthRank >= other.breadthRank;
    }

    public static PermissionScopeType fromLegacy(String raw) {
        if (raw == null) {
            return ORGANIZATION;
        }
        if ("GLOBAL".equals(raw)) {
            return GLOBAL_SYSTEM;
        }
        return PermissionScopeType.valueOf(raw);
    }
}
