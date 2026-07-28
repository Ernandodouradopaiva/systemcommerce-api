package br.com.systemcommerce.config;

import br.com.systemcommerce.user.entity.Role;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Order(100)
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminSeedProperties.class)
public class AdminUserSeedRunner implements ApplicationRunner {

    private final AdminSeedProperties properties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            log.debug("Seed do administrador desabilitado");
            return;
        }

        if (!StringUtils.hasText(properties.getPassword())) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD / app.seed.admin.password não configurado. "
                            + "Defina uma senha inicial para o usuário administrador.");
        }

        Role adminRole = roleRepository
                .findWithPermissionsByCode(properties.getRoleCode())
                .or(() -> roleRepository.findByCode(properties.getRoleCode()))
                .orElseThrow(() -> new IllegalStateException(
                        "Perfil " + properties.getRoleCode() + " não encontrado. Verifique as migrations de seed."));

        User admin = userRepository
                .findByEmailIgnoreCase(properties.getEmail())
                .orElseGet(this::newAdminUser);

        admin.setName(properties.getName());
        admin.setEmail(properties.getEmail().trim().toLowerCase());
        admin.setLogin(properties.getLogin().trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(properties.getPassword()));
        admin.setActive(Boolean.TRUE);
        admin.setStatus(User.UserStatus.ACTIVE);
        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        admin.getRoles().clear();
        admin.getRoles().add(adminRole);

        userRepository.save(admin);
        log.info("Usuario administrador garantido: {} / login={}", admin.getEmail(), admin.getLogin());
    }

    private User newAdminUser() {
        User user = new User();
        user.setId(UUID.fromString(properties.getUserId()));
        return user;
    }
}
