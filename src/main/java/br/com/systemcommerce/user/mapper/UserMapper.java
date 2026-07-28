package br.com.systemcommerce.user.mapper;

import br.com.systemcommerce.user.dto.UserResponse;
import br.com.systemcommerce.user.entity.User;
import br.com.systemcommerce.user.service.PermissionResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final PermissionResolver permissionResolver;

    @Transactional(readOnly = true)
    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getLogin(),
                user.getStatus(),
                user.getActive(),
                user.getFailedLoginAttempts(),
                user.getLastLoginAt(),
                user.getLockedUntil(),
                permissionResolver.resolveRoleCodes(user),
                permissionResolver.resolvePermissionCodes(user),
                user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
