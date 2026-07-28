package br.com.systemcommerce.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.rate-limit")
public class RateLimitProperties {

    private boolean enabled = true;
    private int loginPerMinute = 20;
    private int refreshPerMinute = 30;
    private int passwordForgotPerMinute = 10;
    private int passwordResetPerMinute = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLoginPerMinute() {
        return loginPerMinute;
    }

    public void setLoginPerMinute(int loginPerMinute) {
        this.loginPerMinute = loginPerMinute;
    }

    public int getRefreshPerMinute() {
        return refreshPerMinute;
    }

    public void setRefreshPerMinute(int refreshPerMinute) {
        this.refreshPerMinute = refreshPerMinute;
    }

    public int getPasswordForgotPerMinute() {
        return passwordForgotPerMinute;
    }

    public void setPasswordForgotPerMinute(int passwordForgotPerMinute) {
        this.passwordForgotPerMinute = passwordForgotPerMinute;
    }

    public int getPasswordResetPerMinute() {
        return passwordResetPerMinute;
    }

    public void setPasswordResetPerMinute(int passwordResetPerMinute) {
        this.passwordResetPerMinute = passwordResetPerMinute;
    }

    /** Contador em janela deslizante simples (minuto corrente UTC). */
    public static final class MinuteWindowCounter {
        private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

        public boolean tryConsume(String key, int limit) {
            long minute = System.currentTimeMillis() / 60_000L;
            Window window = windows.compute(key, (k, existing) -> {
                if (existing == null || existing.minute != minute) {
                    return new Window(minute);
                }
                return existing;
            });
            return window.count.incrementAndGet() <= limit;
        }

        public void clear() {
            windows.clear();
        }

        private static final class Window {
            private final long minute;
            private final AtomicInteger count = new AtomicInteger();

            private Window(long minute) {
                this.minute = minute;
            }
        }
    }
}
