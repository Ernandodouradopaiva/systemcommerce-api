package br.com.systemcommerce.storecontext;

import java.util.Objects;
import java.util.UUID;

/**
 * Contexto imutável da loja ativa na requisição.
 * Resolvido pelo backend (header, default, terminal/sessão ou recurso) — nunca confiar cegamente no cliente.
 */
public final class CurrentStoreContext {

    public enum Source {
        HEADER,
        DEFAULT,
        TERMINAL,
        RESOURCE,
        EXPLICIT,
        NONE
    }

    private static final ThreadLocal<CurrentStoreContext> HOLDER = new ThreadLocal<>();

    private final UUID storeId;
    private final UUID organizationId;
    private final Source source;
    private final boolean requiredValidated;

    private CurrentStoreContext(UUID storeId, UUID organizationId, Source source, boolean requiredValidated) {
        this.storeId = storeId;
        this.organizationId = organizationId;
        this.source = source == null ? Source.NONE : source;
        this.requiredValidated = requiredValidated;
    }

    public static CurrentStoreContext empty() {
        return new CurrentStoreContext(null, null, Source.NONE, false);
    }

    public static CurrentStoreContext of(UUID storeId, UUID organizationId, Source source) {
        return new CurrentStoreContext(storeId, organizationId, source, false);
    }

    public CurrentStoreContext validated() {
        return new CurrentStoreContext(storeId, organizationId, source, true);
    }

    public static void set(CurrentStoreContext context) {
        HOLDER.set(context != null ? context : empty());
    }

    public static CurrentStoreContext get() {
        CurrentStoreContext ctx = HOLDER.get();
        return ctx != null ? ctx : empty();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public UUID storeId() {
        return storeId;
    }

    public UUID organizationId() {
        return organizationId;
    }

    public Source source() {
        return source;
    }

    public boolean hasStore() {
        return storeId != null;
    }

    public boolean isRequiredValidated() {
        return requiredValidated;
    }

    public UUID requireStoreId() {
        if (storeId == null) {
            throw new br.com.systemcommerce.shared.exception.BusinessException(
                    br.com.systemcommerce.shared.exception.ErrorCode.STORE_CONTEXT_REQUIRED,
                    "Contexto de loja obrigatório para esta operação");
        }
        return storeId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CurrentStoreContext that)) {
            return false;
        }
        return Objects.equals(storeId, that.storeId)
                && Objects.equals(organizationId, that.organizationId)
                && source == that.source;
    }

    @Override
    public int hashCode() {
        return Objects.hash(storeId, organizationId, source);
    }
}
