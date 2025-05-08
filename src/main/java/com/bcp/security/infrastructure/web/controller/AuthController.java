package com.bcp.security.infrastructure.web.controller;

import com.bcp.security.domain.port.in.AuthUseCase;
import com.bcp.security.infrastructure.web.dto.request.LoginRequest;
import com.bcp.security.infrastructure.web.dto.request.RegisterRequest;
import com.bcp.security.infrastructure.web.dto.response.ApiResponse;
import com.bcp.security.infrastructure.web.dto.response.JwtResponse;
import com.bcp.security.infrastructure.web.dto.response.UserResponse;
import com.bcp.security.infrastructure.web.mapper.UserDtoMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for username: {}", loginRequest.getUsername());

        return authUseCase.login(loginRequest.getUsername(), loginRequest.getPassword())
                .map(token -> {
                    log.info("Login successful for user: {}", loginRequest.getUsername());
                    return JwtResponse.builder().token(token).build();
                })
                .map(ApiResponse::success)
                .doOnError(e -> log.error("Login failed: {}", e.getMessage(), e))
                .onErrorResume(ResponseStatusException.class, e -> {
                    log.warn("Login error: {}", e.getMessage());
                    return Mono.error(e);
                });
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("Registration request received for username: {}", registerRequest.getUsername());

        return authUseCase.register(userDtoMapper.toDomain(registerRequest))
                .map(userDtoMapper::toResponse)
                .map(userResponse -> {
                    log.info("User registered successfully: {}", registerRequest.getUsername());
                    return ApiResponse.success("Usuario registrado exitosamente", userResponse);
                })
                .doOnError(e -> log.error("Registration failed: {}", e.getMessage(), e));
    }
}
