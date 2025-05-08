package com.bcp.security.infrastructure.security;

import com.bcp.security.domain.port.in.UserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UserUseCase userUseCase;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        log.info("Loading user details for username: {}", username);

        return userUseCase.findByUsername(username)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("User not found: {}", username);
                    return Mono.error(new UsernameNotFoundException("User not found: " + username));
                }))
                .doOnNext(user -> log.info("User found: {}", user.getUsername()))
                .map(domainUser -> {
                    if (domainUser.getRoles() == null || domainUser.getRoles().isEmpty()) {
                        log.warn("User has no roles: {}", username);
                    }

                    var authorities = domainUser.getRoles().stream()
                            .map(role -> {
                                log.debug("Adding authority: {} for user: {}", role.getName(), username);
                                return new SimpleGrantedAuthority(role.getName());
                            })
                            .collect(Collectors.toList());

                    log.debug("User password hash: {}", domainUser.getPassword());

                    return new User(
                            domainUser.getUsername(),
                            domainUser.getPassword(),
                            true, true, true, true,
                            authorities
                    );
                })
                .cast(UserDetails.class) // Asegura el tipo final como UserDetails
                .doOnError(error -> log.error("Error loading user details: {}", error.getMessage(), error))
                .doOnSuccess(userDetails -> log.info("User details loaded successfully for: {}", username));
    }
}
