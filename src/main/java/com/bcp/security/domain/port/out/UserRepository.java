package com.bcp.security.domain.port.out;

import com.bcp.security.domain.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepository {
    Mono<User> findById(Long id);
    Mono<User> findByUsername(String username);
    Flux<User> findAll();
    Mono<User> save(User user);
    Mono<Void> deleteById(Long id);
}
