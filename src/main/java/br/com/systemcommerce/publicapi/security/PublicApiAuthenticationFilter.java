package br.com.systemcommerce.publicapi.security;

import br.com.systemcommerce.publicapi.entity.PublicApiCredential;
import br.com.systemcommerce.publicapi.service.PublicApiCredentialService;
import br.com.systemcommerce.security.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class PublicApiAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final PublicApiCredentialService credentialService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/public/v1/")
                || path.equals("/api/public/v1/oauth/token")
                || path.equals("/api/public/v1/oauth/token/");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtService.parseClaims(token);
                if (jwtService.isPublicAccessToken(claims)) {
                    String clientId = claims.getSubject();
                    PublicApiCredential cred = credentialService.requireActive(clientId);
                    credentialService.assertRateLimit(cred);
                    String scopes = claims.get(JwtService.CLAIM_SCOPES, String.class);
                    List<SimpleGrantedAuthority> authorities = Arrays.stream(
                                    scopes != null ? scopes.split("[,\\s]+") : new String[0])
                            .filter(s -> !s.isBlank())
                            .map(s -> new SimpleGrantedAuthority("SCOPE_" + s.trim().toUpperCase().replace('.', '_')))
                            .collect(Collectors.toList());
                    var auth = new UsernamePasswordAuthenticationToken(clientId, null, authorities);
                    auth.setDetails(MapDetails.of(
                            cred.getOrganization().getId(),
                            scopes,
                            cred.getId()));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    request.setAttribute("publicApiCredential", cred);
                    request.setAttribute("publicApiScopes", scopes);
                    request.setAttribute("publicApiOrganizationId", cred.getOrganization().getId());
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    public record MapDetails(UUID organizationId, String scopes, UUID credentialId) {
        static MapDetails of(UUID organizationId, String scopes, UUID credentialId) {
            return new MapDetails(organizationId, scopes, credentialId);
        }
    }
}
