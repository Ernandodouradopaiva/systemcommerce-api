package br.com.systemcommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.seed.admin")
public class AdminSeedProperties {

    private static final String DEFAULT_PASSWORD = "Admin@123";

    private boolean enabled = true;
    private String email = "admin@systemcommerce.local";
    private String login = "admin";
    private String name = "Administrador";
    private String password = DEFAULT_PASSWORD;
    private String roleCode = "ADMIN";
    private String userId = "a0000000-0000-4000-8000-000000000001";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (StringUtils.hasText(email)) {
            this.email = email.trim();
        }
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        if (StringUtils.hasText(login)) {
            this.login = login.trim();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (StringUtils.hasText(name)) {
            this.name = name.trim();
        }
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = StringUtils.hasText(password) ? password : DEFAULT_PASSWORD;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        if (StringUtils.hasText(roleCode)) {
            this.roleCode = roleCode.trim();
        }
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        if (StringUtils.hasText(userId)) {
            this.userId = userId.trim();
        }
    }
}
