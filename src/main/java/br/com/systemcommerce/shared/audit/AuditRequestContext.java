package br.com.systemcommerce.shared.audit;

/** Contexto de request para IP (correlation ID vem de CorrelationIdContext). */
public final class AuditRequestContext {

    private static final ThreadLocal<String> IP = new ThreadLocal<>();

    private AuditRequestContext() {}

    public static void setIpAddress(String ipAddress) {
        IP.set(ipAddress);
    }

    public static String ipAddress() {
        return IP.get();
    }

    public static void clear() {
        IP.remove();
    }
}
