package io.gocklkatz.helloopenapi.auth;

import io.gocklkatz.helloopenapi.config.UserConfig;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtService jwtService;
    private final UserConfig userConfig;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService, UserConfig userConfig, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userConfig = userConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        boolean valid = userConfig.getUsers().stream()
                .anyMatch(u -> u.username().equals(request.username())
                        && passwordEncoder.matches(request.password(), u.password()));
        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid credentials"));
        }
        String token = jwtService.generateToken(request.username());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
