package com.bcp.security.infrastructure.web.controller;

import com.bcp.security.domain.port.in.UserUseCase;
import com.bcp.security.infrastructure.web.dto.response.ApiResponse;
import com.bcp.security.infrastructure.web.dto.response.UserResponse;
import com.bcp.security.infrastructure.web.mapper.UserDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserUseCase userUseCase;
    private final UserDtoMapper userDtoMapper;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<UserResponse> getAllUsers() {
        return userUseCase.findAll()
                .map(userDtoMapper::toResponse);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        return userUseCase.findById(id)
                .map(userDtoMapper::toResponse)
                .map(ApiResponse::success);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Void> deleteUser(@PathVariable Long id) {
        return userUseCase.deleteById(id);
    }
}
