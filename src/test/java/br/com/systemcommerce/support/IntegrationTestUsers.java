package br.com.systemcommerce.support;

import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.RoleRepository;
import br.com.systemcommerce.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Helpers compartilhados para testes de integração (PostgreSQL real). */
public final class IntegrationTestUsers {

    private IntegrationTestUsers() {}

    public static User createUser(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            String roleCode) {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return createUser(
                userRepository,
                roleRepository,
                passwordEncoder,
                "u_" + suffix,
                "u_" + suffix + "@test.local",
                "Test@1234",
                roleCode);
    }

    public static User createUser(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            String login,
            String email,
            String rawPassword,
            String roleCode) {
        User user = new User();
        user.setName(login);
        user.setLogin(login);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setActive(true);
        user.setFailedLoginAttempts(0);
        user.getRoles().add(roleRepository.findByCode(roleCode).orElseThrow());
        return userRepository.saveAndFlush(user);
    }
}
