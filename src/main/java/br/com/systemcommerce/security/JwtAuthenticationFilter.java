package br.com.systemcommerce.security;

import br.com.systemcommerce.access.service.UserSessionService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AccessVersionValidator accessVersionValidator;
    private final UserSessionService userSessionService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Claims claims = jwtService.parseClaims(token);
            if (!jwtService.isAccessToken(claims)) {
                request.setAttribute(SecurityErrorWriter.JWT_ERROR_ATTR, SecurityErrorWriter.JWT_INVALID);
                filterChain.doFilter(request, response);
                return;
            }
            if (!accessVersionValidator.matches(claims)) {
                request.setAttribute(SecurityErrorWriter.JWT_ERROR_ATTR, SecurityErrorWriter.JWT_INVALID);
                filterChain.doFilter(request, response);
                return;
            }
            Object sidRaw = claims.get(JwtService.CLAIM_SESSION_ID);
            if (sidRaw != null) {
                try {
                    UUID sid = UUID.fromString(String.valueOf(sidRaw));
                    if (!userSessionService.isSessionActive(sid)) {
                        request.setAttribute(SecurityErrorWriter.JWT_ERROR_ATTR, SecurityErrorWriter.JWT_INVALID);
                        filterChain.doFilter(request, response);
                        return;
                    }
                } catch (IllegalArgumentException ignored) {
                    request.setAttribute(SecurityErrorWriter.JWT_ERROR_ATTR, SecurityErrorWriter.JWT_INVALID);
                    filterChain.doFilter(request, response);
                    return;
                }
            }

            Collection<SimpleGrantedAuthority> authorities = extractAuthorities(claims);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException ex) {
            request.setAttribute(SecurityErrorWriter.JWT_ERROR_ATTR, SecurityErrorWriter.JWT_EXPIRED);
        } catch (JwtException | IllegalArgumentException ex) {
            request.setAttribute(SecurityErrorWriter.JWT_ERROR_ATTR, SecurityErrorWriter.JWT_INVALID);
        }

        filterChain.doFilter(request, response);
    }

    private Collection<SimpleGrantedAuthority> extractAuthorities(Claims claims) {
        Object raw = claims.get(JwtService.CLAIM_AUTHORITIES);
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .map(String::valueOf)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
