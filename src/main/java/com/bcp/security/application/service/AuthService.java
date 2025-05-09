package com.bcp.security.application.service;

import com.bcp.security.domain.model.Role;
import com.bcp.security.domain.model.User;
import com.bcp.security.domain.port.in.AuthUseCase;
import com.bcp.security.domain.port.out.RoleRepository;
import com.bcp.security.domain.port.out.UserRepository;
import com.bcp.security.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<String> login(String username, String password) {
        log.info("Attempting login for user: {}", username);

        return userRepository.findByUsername(username)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Login failed: User not found: {}", username);
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid username or password"));
                }))
                .flatMap(user -> {
                    log.debug("User found, verifying password for: {}", username);

                    // Verificar directamente la contraseña sin usar el AuthenticationManager
                    if (!passwordEncoder.matches(password, user.getPassword())) {
                        log.error("Login failed: Invalid password for user: {}", username);
                        return Mono.error(new BadCredentialsException("Invalid username or password"));
                    }

                    log.info("Password verified successfully for user: {}", username);

                    // Generar token directamente
                    return Mono.just(tokenProvider.generateToken(user))
                            .doOnNext(token -> log.info("JWT token generated successfully for user: {}", username));
                })
                .onErrorResume(BadCredentialsException.class, e -> {
                    log.error("Authentication failed: {}", e.getMessage());
                    return Mono.error(new ResponseStatusException(
                            HttpStatus.UNAUTHORIZED, "Invalid username or password"));
                });
    }

    @Override
    public Mono<User> register(User user) {
        final String username = user.getUsername();
        log.info("Registering new user: {}", username);

        // Verificar si el usuario ya existe
        return userRepository.findByUsername(username)
                .hasElement()
                .flatMap(exists -> {
                    if (exists) {
                        log.warn("Username already exists: {}", username);
                        return Mono.error(new ResponseStatusException(
                                HttpStatus.BAD_REQUEST, "Username already exists"));
                    }

                    // Encriptar contraseña
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    log.info("Password encrypted for user: {}", username);

                    // Asignar roles
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        log.info("Assigning custom roles for user: {}", username);
                        return assignCustomRoles(user);
                    } else {
                        log.info("Assigning default ROLE_USER for user: {}", username);
                        return assignDefaultRole(user);
                    }
                });
    }

    private Mono<User> assignCustomRoles(User user) {
        return Flux.fromIterable(user.getRoles())
                .flatMap(role -> {
                    log.info("Looking up role: {}", role.getName());
                    return roleRepository.findByName(role.getName());
                })
                .collectList()
                .map(roles -> {
                    user.setRoles(new HashSet<>(roles));
                    return user;
                })
                .flatMap(this::saveAndLogUser);
    }

    private Mono<User> assignDefaultRole(User user) {
        return roleRepository.findByName("ROLE_USER")
                .map(role -> {
                    user.setRoles(new HashSet<>(Collections.singletonList(role)));
                    return user;
                })
                .flatMap(this::saveAndLogUser);
    }

    private Mono<User> saveAndLogUser(User user) {
        return userRepository.save(user)
                .doOnSuccess(savedUser -> log.info("User registered successfully: {}", savedUser.getUsername()))
                .doOnError(e -> log.error("Error saving user: {}", e.getMessage(), e));
    }
}
