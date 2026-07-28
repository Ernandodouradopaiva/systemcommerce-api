package br.com.systemcommerce.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiErrorResponse", description = "Envelope padrão de erro da API")
public class ApiErrorResponse {

    private final Instant timestamp;
    private final int status;
    private final String error;
    private final String code;
    private final String message;
    private final String path;
    private final String correlationId;
    private final List<FieldErrorDetail> details;

    private ApiErrorResponse(Builder builder) {
        this.timestamp = builder.timestamp;
        this.status = builder.status;
        this.error = builder.error;
        this.code = builder.code;
        this.message = builder.message;
        this.path = builder.path;
        this.correlationId = builder.correlationId;
        this.details = builder.details;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getPath() {
        return path;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public List<FieldErrorDetail> getDetails() {
        return details;
    }

    @Schema(name = "FieldErrorDetail", description = "Erro de validação de um campo")
    public static class FieldErrorDetail {
        private final String field;
        private final String message;
        private final String rejectedValue;

        private FieldErrorDetail(Builder builder) {
            this.field = builder.field;
            this.message = builder.message;
            this.rejectedValue = builder.rejectedValue;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        public String getRejectedValue() {
            return rejectedValue;
        }

        public static class Builder {
            private String field;
            private String message;
            private String rejectedValue;

            public Builder field(String field) {
                this.field = field;
                return this;
            }

            public Builder message(String message) {
                this.message = message;
                return this;
            }

            public Builder rejectedValue(String rejectedValue) {
                this.rejectedValue = rejectedValue;
                return this;
            }

            public FieldErrorDetail build() {
                return new FieldErrorDetail(this);
            }
        }
    }

    public static class Builder {
        private Instant timestamp;
        private int status;
        private String error;
        private String code;
        private String message;
        private String path;
        private String correlationId;
        private List<FieldErrorDetail> details;

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder error(String error) {
            this.error = error;
            return this;
        }

        public Builder code(String code) {
            this.code = code;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder correlationId(String correlationId) {
            this.correlationId = correlationId;
            return this;
        }

        public Builder details(List<FieldErrorDetail> details) {
            this.details = details;
            return this;
        }

        public ApiErrorResponse build() {
            return new ApiErrorResponse(this);
        }
    }
}
