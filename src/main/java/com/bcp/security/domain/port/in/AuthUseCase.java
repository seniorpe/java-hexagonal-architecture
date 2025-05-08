package com.bcp.security.domain.port.in;

import com.bcp.security.domain.model.User;
import reactor.core.publisher.Mono;

public interface AuthUseCase {
    Mono<String> login(String username, String password);
    Mono<User> register(User user);
}
