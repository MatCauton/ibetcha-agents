package com.ibetcha.identity.api;

import com.ibetcha.identity.application.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        AuthService.AuthResult result = authService.register(
                request.email(), request.username(), request.password(), request.displayName()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(buildAuthResponse(result));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.AuthResult result = authService.login(request.email(), request.password());
        return ResponseEntity.ok(buildAuthResponse(result));
    }

    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        AuthService.AuthResult result = authService.refresh(refreshToken);
        return ResponseEntity.ok(Map.of(
                "accessToken", result.accessToken(),
                "refreshToken", result.refreshToken(),
                "expiresIn", result.expiresIn()
        ));
    }

    @PostMapping("/oauth/google")
    public ResponseEntity<Map<String, Object>> googleLogin(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuthService.AuthResult result = authService.loginWithGoogle(idToken);
        return ResponseEntity.ok(buildAuthResponse(result));
    }

    @PostMapping("/oauth/apple")
    public ResponseEntity<Map<String, Object>> appleLogin(@RequestBody Map<String, String> request) {
        String identityToken = request.get("identityToken");
        if (identityToken == null || identityToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        AuthService.AuthResult result = authService.loginWithApple(identityToken);
        return ResponseEntity.ok(buildAuthResponse(result));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        authService.logout(refreshToken);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> buildAuthResponse(AuthService.AuthResult result) {
        return Map.of(
                "accessToken", result.accessToken(),
                "refreshToken", result.refreshToken(),
                "expiresIn", result.expiresIn(),
                "user", Map.of(
                        "id", result.user().id(),
                        "email", result.user().email(),
                        "username", result.user().username() != null ? result.user().username() : "",
                        "displayName", result.user().displayName() != null ? result.user().displayName() : ""
                )
        );
    }

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 3, max = 30) String username,
            @NotBlank @Size(min = 8) String password,
            String displayName
    ) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password
    ) {}
}
