package br.com.systemcommerce.auth.controller;

import br.com.systemcommerce.auth.dto.AuthTokenResponse;
import br.com.systemcommerce.auth.dto.ChangePasswordRequest;
import br.com.systemcommerce.auth.dto.ForgotPasswordRequest;
import br.com.systemcommerce.auth.dto.ForgotPasswordResponse;
import br.com.systemcommerce.auth.dto.LoginRequest;
import br.com.systemcommerce.auth.dto.LogoutRequest;
import br.com.systemcommerce.auth.dto.MessageResponse;
import br.com.systemcommerce.auth.dto.RefreshTokenRequest;
import br.com.systemcommerce.auth.dto.ResetPasswordRequest;
import br.com.systemcommerce.auth.service.AuthService;
import br.com.systemcommerce.shared.exception.ApiErrorResponse;
import br.com.systemcommerce.shared.response.ApiResponse;
import br.com.systemcommerce.shared.web.CorrelationIdConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Autenticação e sessão")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login com login/e-mail e senha")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Autenticado",
                headers = @Header(name = CorrelationIdConstants.HEADER)),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.of(
                authService.login(request, httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova access token com refresh token (rotação)")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.of(authService.refresh(
                request.refreshToken(), httpRequest.getRemoteAddr(), httpRequest.getHeader("User-Agent"))));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoga o refresh token informado")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(@Valid @RequestBody LogoutRequest request) {
        return ResponseEntity.ok(ApiResponse.of(authService.logout(request.refreshToken())));
    }

    @PostMapping("/logout-all")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Revoga todos os refresh tokens do usuário autenticado")
    public ResponseEntity<ApiResponse<MessageResponse>> logoutAll(Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.of(authService.logoutAll(userId)));
    }

    @PostMapping("/password/change")
    @SecurityRequirement(name = "bearer-jwt")
    @Operation(summary = "Altera a senha do usuário autenticado")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            Authentication authentication, @Valid @RequestBody ChangePasswordRequest request) {
        UUID userId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(ApiResponse.of(authService.changePassword(userId, request)));
    }

    @PostMapping("/password/forgot")
    @Operation(summary = "Solicita recuperação de senha")
    public ResponseEntity<ApiResponse<ForgotPasswordResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.of(authService.forgotPassword(request.email())));
    }

    @PostMapping("/password/reset")
    @Operation(summary = "Redefine a senha com token de recuperação")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.of(authService.resetPassword(request)));
    }
}
