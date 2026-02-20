package io.gocklkatz.helloopenapi.controller;

import com.example.api.AuthApi;
import com.example.model.LoginRequest;
import com.example.model.LoginResponse;
import io.gocklkatz.helloopenapi.auth.JwtService;
import io.gocklkatz.helloopenapi.config.UserConfig;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

    private final JwtService jwtService;
    private final UserConfig userConfig;
    private final PasswordEncoder passwordEncoder;

    public AuthController(JwtService jwtService, UserConfig userConfig, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userConfig = userConfig;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
        boolean valid = userConfig.getUsers().stream()
                .anyMatch(u -> u.username().equals(loginRequest.getUsername())
                        && passwordEncoder.matches(loginRequest.getPassword(), u.password()));
        if (!valid) {
            throw new BadCredentialsException("Invalid credentials");
        }
        String token = jwtService.generateToken(loginRequest.getUsername());
        return ResponseEntity.ok(new LoginResponse(token));
    }
}
