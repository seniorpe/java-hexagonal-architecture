package com.bcp.security.infrastructure.security;

import com.bcp.security.domain.port.in.UserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UserUseCase userUseCase;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return userUseCase.findByUsername(username)
                .map(user -> {
                    var authorities = user.getRoles().stream()
                            .map(role -> new SimpleGrantedAuthority(role.getName()))
                            .collect(Collectors.toList());

                    return new User(
                            user.getUsername(),
                            user.getPassword(),
                            true, true, true, true,
                            authorities
                    );
                });
    }
}
