package br.com.systemcommerce.report.support;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Filtro efetivo de loja aplicado às consultas de relatório/dashboard. */
public record ReportStoreFilter(UUID storeId, Collection<UUID> allowedStoreIds) {

    /**
     * UUID placeholder só para tipagem JDBC em {@code IN (:allowedStoreIds)} quando não há filtro
     * multi-loja (PostgreSQL exige tipo mesmo com {@code restrict=false}).
     */
    private static final UUID NATIVE_IN_PLACEHOLDER =
            UUID.fromString("00000000-0000-0000-0000-000000000000");

    public static ReportStoreFilter unrestricted() {
        return new ReportStoreFilter(null, null);
    }

    public static ReportStoreFilter single(UUID storeId) {
        return new ReportStoreFilter(storeId, null);
    }

    public static ReportStoreFilter multi(Collection<UUID> storeIds) {
        return new ReportStoreFilter(null, storeIds);
    }

    public boolean isEmpty() {
        return allowedStoreIds != null && allowedStoreIds.isEmpty();
    }

    /**
     * Parâmetros seguros para SQL nativo PostgreSQL.
     * <p>
     * Evita {@code ERROR: could not determine data type of parameter} quando a coleção seria
     * {@code null} em {@code (:allowedStoreIds IS NULL OR ... IN (:allowedStoreIds))}.
     */
    public NativeAllowedStores nativeAllowedStores() {
        if (allowedStoreIds == null) {
            return new NativeAllowedStores(false, List.of(NATIVE_IN_PLACEHOLDER));
        }
        return new NativeAllowedStores(true, List.copyOf(allowedStoreIds));
    }

    /** @param restrict quando {@code false}, o {@code IN} não restringe (visão global / loja única). */
    public record NativeAllowedStores(boolean restrict, Collection<UUID> ids) {}
}
