package br.com.systemcommerce.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.auth")
public class AuthProperties {

    private int maxFailedAttempts = 5;
    private long lockDurationMinutes = 30;
    private long passwordResetExpirationMinutes = 60;

    public int getMaxFailedAttempts() {
        return maxFailedAttempts;
    }

    public void setMaxFailedAttempts(int maxFailedAttempts) {
        if (maxFailedAttempts > 0) {
            this.maxFailedAttempts = maxFailedAttempts;
        }
    }

    public long getLockDurationMinutes() {
        return lockDurationMinutes;
    }

    public void setLockDurationMinutes(long lockDurationMinutes) {
        if (lockDurationMinutes > 0) {
            this.lockDurationMinutes = lockDurationMinutes;
        }
    }

    public long getPasswordResetExpirationMinutes() {
        return passwordResetExpirationMinutes;
    }

    public void setPasswordResetExpirationMinutes(long passwordResetExpirationMinutes) {
        if (passwordResetExpirationMinutes > 0) {
            this.passwordResetExpirationMinutes = passwordResetExpirationMinutes;
        }
    }
}
