package br.com.systemcommerce.security;

import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessVersionValidator {

    private final UserRepository userRepository;

    public boolean matches(Claims claims) {
        Object raw = claims.get(JwtService.CLAIM_ACCESS_VERSION);
        long tokenAv = raw instanceof Number n ? n.longValue() : 0L;
        try {
            UUID userId = UUID.fromString(claims.getSubject());
            return userRepository
                    .findById(userId)
                    .map(User::getAccessVersion)
                    .map(v -> v == null || v == tokenAv)
                    .orElse(false);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
