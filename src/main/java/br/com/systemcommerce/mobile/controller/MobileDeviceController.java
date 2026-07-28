package br.com.systemcommerce.mobile.controller;

import br.com.systemcommerce.mobile.dto.DevicePushTokenRegisterRequest;
import br.com.systemcommerce.mobile.dto.DevicePushTokenResponse;
import br.com.systemcommerce.mobile.service.DevicePushTokenService;
import br.com.systemcommerce.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearer-jwt")
@Tag(name = "Mobile", description = "Preparação mobile (Prompt 86)")
public class MobileDeviceController {

    private final DevicePushTokenService devicePushTokenService;

    @PostMapping("/device-tokens")
    @PreAuthorize("hasAuthority('MOBILE_DEVICE_MANAGE') or isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DevicePushTokenResponse> register(
            @Valid @RequestBody DevicePushTokenRegisterRequest request) {
        return ApiResponse.of(devicePushTokenService.register(request));
    }
}
