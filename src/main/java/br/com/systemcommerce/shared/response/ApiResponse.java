package br.com.systemcommerce.shared.response;

import br.com.systemcommerce.shared.web.CorrelationIdContext;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ApiResponse", description = "Envelope padrão de sucesso da API")
public class ApiResponse<T> {

    @Schema(description = "Payload da resposta")
    private final T data;

    @Schema(description = "Momento da resposta em UTC")
    private final Instant timestamp;

    @Schema(description = "Identificador de correlação da requisição")
    private final String correlationId;

    private ApiResponse(T data, Instant timestamp, String correlationId) {
        this.data = data;
        this.timestamp = timestamp;
        this.correlationId = correlationId;
    }

    public static <T> ApiResponse<T> of(T data) {
        return new ApiResponse<>(data, Instant.now(), CorrelationIdContext.current());
    }

    public T getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCorrelationId() {
        return correlationId;
    }
}
