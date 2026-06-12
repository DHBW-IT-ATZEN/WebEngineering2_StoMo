package com.dhbw.webeng2.stomo.controller;

import com.dhbw.webeng2.stomo.exception.ResourceNotFoundException;
import com.dhbw.webeng2.stomo.model.dto.AuthResponse;
import com.dhbw.webeng2.stomo.model.dto.LoginRequest;
import com.dhbw.webeng2.stomo.model.dto.RegisterRequest;
import com.dhbw.webeng2.stomo.model.dto.UserResponse;
import com.dhbw.webeng2.stomo.model.entity.User;
import com.dhbw.webeng2.stomo.repository.UserRepo;
import com.dhbw.webeng2.stomo.service.JwtService;
import com.dhbw.webeng2.stomo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final UserRepo userRepo;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokenFor(user));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.authenticate(request);
        return ResponseEntity.ok(tokenFor(user));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal Jwt jwt) {
        User user = userRepo.findByEmail(jwt.getSubject())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return ResponseEntity.ok(UserResponse.from(user));
    }

    private AuthResponse tokenFor(User user) {
        return AuthResponse.builder()
                .token(jwtService.issue(user))
                .user(UserResponse.from(user))
                .build();
    }
}
