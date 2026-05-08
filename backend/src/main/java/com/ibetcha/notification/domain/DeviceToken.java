package com.ibetcha.notification.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "device_tokens")
public class DeviceToken {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeviceToken() {}

    public static DeviceToken create(UUID userId, String token, Platform platform, String deviceId) {
        DeviceToken dt = new DeviceToken();
        dt.id = UUID.randomUUID();
        dt.userId = userId;
        dt.token = token;
        dt.platform = platform;
        dt.deviceId = deviceId;
        dt.createdAt = Instant.now();
        dt.updatedAt = Instant.now();
        return dt;
    }

    public void updateToken(String newToken) {
        this.token = newToken;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Platform getPlatform() { return platform; }
    public String getDeviceId() { return deviceId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public enum Platform { IOS, ANDROID }
}
