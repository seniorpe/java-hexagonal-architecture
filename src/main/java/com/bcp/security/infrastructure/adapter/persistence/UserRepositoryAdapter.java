package com.bcp.security.infrastructure.adapter.persistence;

import com.bcp.security.domain.model.Role;
import com.bcp.security.domain.model.User;
import com.bcp.security.domain.port.out.UserRepository;
import com.bcp.security.infrastructure.adapter.persistence.entity.UserEntity;
import com.bcp.security.infrastructure.adapter.persistence.entity.UserRoleEntity;
import com.bcp.security.infrastructure.adapter.persistence.mapper.UserMapper;
import com.bcp.security.infrastructure.adapter.persistence.repository.R2dbcRoleRepository;
import com.bcp.security.infrastructure.adapter.persistence.repository.R2dbcUserRepository;
import com.bcp.security.infrastructure.adapter.persistence.repository.R2dbcUserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final R2dbcUserRepository userRepository;
    private final R2dbcRoleRepository roleRepository;
    private final R2dbcUserRoleRepository userRoleRepository;
    private final UserMapper userMapper;

    @Override
    public Mono<User> findById(Long id) {
        return userRepository.findById(id)
                .flatMap(this::enrichWithRoles);
    }

    @Override
    public Mono<User> findByUsername(String username) {
        return userRepository.findByUsername(username)
                .flatMap(this::enrichWithRoles);
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll()
                .flatMap(this::enrichWithRoles);
    }

    @Override
    @Transactional
    public Mono<User> save(User user) {
        UserEntity userEntity = userMapper.toEntity(user);

        if (userEntity.getId() == null) {
            userEntity.setCreatedAt(LocalDateTime.now());
            userEntity.setUpdatedAt(LocalDateTime.now());
        } else {
            userEntity.setUpdatedAt(LocalDateTime.now());
        }

        return userRepository.save(userEntity)
                .flatMap(savedUser -> {
                    if (user.getRoles() != null && !user.getRoles().isEmpty()) {
                        return Flux.fromIterable(user.getRoles())
                                .flatMap(role -> userRoleRepository.save(
                                        UserRoleEntity.builder()
                                                .userId(savedUser.getId())
                                                .roleId(role.getId())
                                                .build()
                                ))
                                .then(Mono.just(savedUser));
                    }
                    return Mono.just(savedUser);
                })
                .flatMap(this::enrichWithRoles);
    }

    @Override
    @Transactional
    public Mono<Void> deleteById(Long id) {
        return userRoleRepository.findRoleIdsByUserId(id)
                .flatMap(roleId -> userRoleRepository.delete(
                        UserRoleEntity.builder()
                                .userId(id)
                                .roleId(roleId)
                                .build()
                ))
                .then(userRepository.deleteById(id));
    }

    private Mono<User> enrichWithRoles(UserEntity userEntity) {
        return userRoleRepository.findRoleIdsByUserId(userEntity.getId())
                .flatMap(roleId -> roleRepository.findById(roleId))
                .map(roleEntity -> Role.builder()
                        .id(roleEntity.getId())
                        .name(roleEntity.getName())
                        .build())
                .collectList()
                .map(HashSet::new)
                .map(roles -> {
                    User user = userMapper.toDomain(userEntity);
                    user.setRoles(roles);
                    return user;
                });
    }
}
