package br.com.systemcommerce.pos.cash.support;

import java.util.Collection;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityAuthorities {

    private SecurityAuthorities() {}

    public static boolean hasAuthority(String code) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null) {
            return false;
        }
        return authorities.stream().anyMatch(a -> code.equals(a.getAuthority()));
    }

    public static boolean hasAnyAuthority(String... codes) {
        if (codes == null || codes.length == 0) {
            return false;
        }
        for (String code : codes) {
            if (hasAuthority(code)) {
                return true;
            }
        }
        return false;
    }
}
