package br.com.systemcommerce.security;

import br.com.systemcommerce.shared.exception.ErrorCode;
import br.com.systemcommerce.shared.web.AuditRequestContextFilter;
import br.com.systemcommerce.shared.web.CorrelationIdFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({
    JwtProperties.class,
    AuthProperties.class,
    CorsProperties.class,
    RateLimitProperties.class
})
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthRateLimitFilter authRateLimitFilter;
    private final br.com.systemcommerce.storecontext.StoreContextFilter storeContextFilter;
    private final br.com.systemcommerce.publicapi.security.PublicApiAuthenticationFilter publicApiAuthenticationFilter;
    private final SecurityErrorWriter securityErrorWriter;
    private final Environment environment;

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public AuditRequestContextFilter auditRequestContextFilter() {
        return new AuditRequestContextFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CorrelationIdFilter correlationIdFilter,
            AuditRequestContextFilter auditRequestContextFilter)
            throws Exception {
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        boolean test = environment.acceptsProfiles(Profiles.of("test"));

        http
                // API stateless com Bearer JWT (sem cookie de sessão) — CSRF desabilitado de forma consciente.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentTypeOptions(Customizer.withDefaults())
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer ->
                                referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                                .preload(true)))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                                    "/actuator/health",
                                    "/actuator/health/**",
                                    "/actuator/info")
                            .permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/health").permitAll();
                    auth.requestMatchers(
                                    "/api/v1/auth/login",
                                    "/api/v1/auth/refresh",
                                    "/api/v1/auth/logout",
                                    "/api/v1/auth/password/forgot",
                                    "/api/v1/auth/password/reset")
                            .permitAll();
                    auth.requestMatchers("/api/public/v1/oauth/token").permitAll();
                    auth.requestMatchers("/api/public/v1/**").authenticated();
                    if (!prod) {
                        auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                                .permitAll();
                    }
                    if (test) {
                        auth.requestMatchers("/api/v1/_test/**").permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                securityErrorWriter.write(
                                        request, response, securityErrorWriter.resolveUnauthorizedCode(request)))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                securityErrorWriter.write(request, response, ErrorCode.ACCESS_DENIED)))
                .addFilterBefore(correlationIdFilter, SecurityContextHolderFilter.class)
                .addFilterAfter(auditRequestContextFilter, CorrelationIdFilter.class)
                .addFilterBefore(authRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(publicApiAuthenticationFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(storeContextFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
