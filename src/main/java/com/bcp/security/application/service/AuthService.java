package com.bcp.security.application.service;

import com.bcp.security.domain.model.Role;
import com.bcp.security.domain.model.User;
import com.bcp.security.domain.port.in.AuthUseCase;
import com.bcp.security.domain.port.out.RoleRepository;
import com.bcp.security.domain.port.out.UserRepository;
import com.bcp.security.infrastructure.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService implements AuthUseCase {

    private final ReactiveAuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<String> login(String username, String password) {
        return authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(username, password))
                .map(tokenProvider::generateToken);
    }

    @Override
    public Mono<User> register(User user) {
        // Encriptar la contraseña
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Si el usuario ya tiene roles especificados
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            // Buscar los roles por nombre y asignarlos al usuario
            return Flux.fromIterable(user.getRoles())
                    .flatMap(role -> roleRepository.findByName(role.getName()))
                    .collectList()
                    .map(HashSet::new)
                    .map(roles -> {
                        user.setRoles(roles);
                        return user;
                    })
                    .flatMap(userRepository::save);
        } else {
            // Si no se especificaron roles, asignar ROLE_USER por defecto
            return roleRepository.findByName("ROLE_USER")
                    .map(role -> {
                        user.setRoles(new HashSet<>(Collections.singletonList(role)));
                        return user;
                    })
                    .flatMap(userRepository::save);
        }
    }
}
