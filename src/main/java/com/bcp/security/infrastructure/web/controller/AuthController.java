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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthUseCase authUseCase;
    private final UserDtoMapper userDtoMapper;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<JwtResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        return authUseCase.login(loginRequest.getUsername(), loginRequest.getPassword())
                .map(token -> JwtResponse.builder().token(token).build())
                .map(ApiResponse::success);
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        return authUseCase.register(userDtoMapper.toDomain(registerRequest))
                .map(userDtoMapper::toResponse)
                .map(userResponse -> ApiResponse.success("Usuario registrado exitosamente", userResponse));
    }
}
