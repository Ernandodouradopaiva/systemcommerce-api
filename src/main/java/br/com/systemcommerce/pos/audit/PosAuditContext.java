package br.com.systemcommerce.pos.audit;

import br.com.systemcommerce.shared.audit.AuditLog;
import java.util.UUID;

/** Contexto operacional do evento PDV (sem dados sensíveis). */
public record PosAuditContext(
        UUID storeId,
        UUID terminalId,
        UUID cashSessionId,
        UUID saleId,
        UUID operatorId,
        UUID authorizedById,
        String entityName,
        UUID entityId,
        AuditLog.AuditAction action,
        Object before,
        Object after,
        String details,
        String errorCode) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID storeId;
        private UUID terminalId;
        private UUID cashSessionId;
        private UUID saleId;
        private UUID operatorId;
        private UUID authorizedById;
        private String entityName = "Pos";
        private UUID entityId;
        private AuditLog.AuditAction action = AuditLog.AuditAction.OTHER;
        private Object before;
        private Object after;
        private String details;
        private String errorCode;

        public Builder storeId(UUID v) {
            this.storeId = v;
            return this;
        }

        public Builder terminalId(UUID v) {
            this.terminalId = v;
            return this;
        }

        public Builder cashSessionId(UUID v) {
            this.cashSessionId = v;
            return this;
        }

        public Builder saleId(UUID v) {
            this.saleId = v;
            return this;
        }

        public Builder operatorId(UUID v) {
            this.operatorId = v;
            return this;
        }

        public Builder authorizedById(UUID v) {
            this.authorizedById = v;
            return this;
        }

        public Builder entity(String name, UUID id) {
            this.entityName = name;
            this.entityId = id;
            return this;
        }

        public Builder action(AuditLog.AuditAction v) {
            this.action = v;
            return this;
        }

        public Builder before(Object v) {
            this.before = v;
            return this;
        }

        public Builder after(Object v) {
            this.after = v;
            return this;
        }

        public Builder details(String v) {
            this.details = v;
            return this;
        }

        public Builder errorCode(String v) {
            this.errorCode = v;
            return this;
        }

        public PosAuditContext build() {
            return new PosAuditContext(
                    storeId,
                    terminalId,
                    cashSessionId,
                    saleId,
                    operatorId,
                    authorizedById,
                    entityName,
                    entityId,
                    action,
                    before,
                    after,
                    details,
                    errorCode);
        }
    }
}
