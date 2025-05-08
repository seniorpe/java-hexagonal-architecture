package com.bcp.security.application.service;

import com.bcp.security.domain.model.User;
import com.bcp.security.domain.port.in.UserUseCase;
import com.bcp.security.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public Mono<User> save(User user) {
        return Mono.just(user)
                .map(u -> {
                    if (u.getPassword() != null) {
                        u.setPassword(passwordEncoder.encode(u.getPassword()));
                    }
                    return u;
                })
                .flatMap(userRepository::save);
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        return userRepository.deleteById(id);
    }
}
