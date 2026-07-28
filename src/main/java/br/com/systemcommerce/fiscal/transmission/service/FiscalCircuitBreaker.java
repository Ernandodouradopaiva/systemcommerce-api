package br.com.systemcommerce.fiscal.transmission.service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class FiscalCircuitBreaker {

    private final ConcurrentHashMap<String, CircuitState> states = new ConcurrentHashMap<>();

    public boolean isOpen(String uf, String serviceName, int openMinutes) {
        CircuitState state = states.get(key(uf, serviceName));
        if (state == null || state.openUntil == null) {
            return false;
        }
        if (Instant.now().isAfter(state.openUntil)) {
            states.remove(key(uf, serviceName));
            return false;
        }
        return true;
    }

    public void recordSuccess(String uf, String serviceName) {
        states.remove(key(uf, serviceName));
    }

    public void recordFailure(String uf, String serviceName, int threshold, int openMinutes) {
        String k = key(uf, serviceName);
        states.compute(k, (key, existing) -> {
            CircuitState state = existing != null ? existing : new CircuitState();
            state.consecutiveFailures++;
            if (state.consecutiveFailures >= threshold) {
                state.openUntil = Instant.now().plusSeconds(openMinutes * 60L);
            }
            return state;
        });
    }

    private static String key(String uf, String serviceName) {
        return uf + ":" + serviceName;
    }

    private static final class CircuitState {
        int consecutiveFailures;
        Instant openUntil;
    }
}
