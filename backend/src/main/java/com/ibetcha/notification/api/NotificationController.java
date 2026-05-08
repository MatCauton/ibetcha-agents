package com.ibetcha.notification.api;

import com.ibetcha.notification.application.NotificationService;
import com.ibetcha.notification.domain.DeviceToken;
import com.ibetcha.shared.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * POST /api/v1/notifications/device-token
     * Register or update the FCM device token for the authenticated user.
     * Idempotent: calling again with a new token for the same deviceId updates it.
     */
    @PostMapping("/device-token")
    public ResponseEntity<Map<String, Object>> registerDeviceToken(
            @Valid @RequestBody DeviceTokenRequest request) {
        UUID userId = AuthenticatedUser.currentUserId();
        DeviceToken.Platform platform = DeviceToken.Platform.valueOf(request.platform().toUpperCase());

        // Use token as deviceId fallback when no explicit deviceId is provided
        String deviceId = request.deviceId() != null ? request.deviceId() : request.token();

        DeviceToken saved = notificationService.registerDeviceToken(userId, request.token(), platform, deviceId);
        return ResponseEntity.ok(Map.of(
                "deviceTokenId", saved.getId(),
                "platform", saved.getPlatform().name(),
                "registered", true
        ));
    }

    public record DeviceTokenRequest(
            @NotBlank String token,
            @NotNull @Pattern(regexp = "ANDROID|IOS", message = "platform must be ANDROID or IOS") String platform,
            String deviceId
    ) {}
}
