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
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepository {

    private final R2dbcUserRepository userRepository;
    private final R2dbcRoleRepository roleRepository;
    private final R2dbcUserRoleRepository userRoleRepository;
    private final UserMapper userMapper;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private static final String USER_CACHE_PREFIX = "user:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(30);

    @Override
    public Mono<User> findById(Long id) {
        String cacheKey = USER_CACHE_PREFIX + "id:" + id;

        return redisTemplate.opsForValue().get(cacheKey)
                .cast(User.class)
                .switchIfEmpty(
                        userRepository.findById(id)
                                .flatMap(this::enrichWithRoles)
                                .doOnNext(user -> {
                                    log.debug("Caching user with ID: {}", id);
                                    redisTemplate.opsForValue().set(cacheKey, user, CACHE_TTL).subscribe();
                                })
                );
    }

    @Override
    public Mono<User> findByUsername(String username) {
        log.debug("Finding user by username: {}", username);
        String cacheKey = USER_CACHE_PREFIX + "username:" + username;

        return redisTemplate.opsForValue().get(cacheKey)
                .cast(User.class)
                .doOnNext(user -> log.debug("User found in cache: {}", username))
                .switchIfEmpty(
                        Mono.defer(() -> {
                            log.debug("User not found in cache, querying database: {}", username);
                            return userRepository.findByUsername(username)
                                    .flatMap(this::enrichWithRoles)
                                    .doOnNext(user -> {
                                        log.debug("Caching user: {}", username);
                                        redisTemplate.opsForValue().set(cacheKey, user, CACHE_TTL).subscribe();
                                    });
                        })
                )
                .doOnError(e -> log.error("Error finding user by username: {}", username, e));
    }

    @Override
    public Flux<User> findAll() {
        return userRepository.findAll()
                .flatMap(this::enrichWithRoles);
    }

    @Override
    @Transactional
    public Mono<User> save(User user) {
        log.debug("Saving user: {}", user.getUsername());
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
                .flatMap(this::enrichWithRoles)
                .doOnNext(savedUser -> {
                    // Invalidate cache
                    String usernameKey = USER_CACHE_PREFIX + "username:" + savedUser.getUsername();
                    String idKey = USER_CACHE_PREFIX + "id:" + savedUser.getId();

                    redisTemplate.delete(usernameKey).subscribe();
                    redisTemplate.delete(idKey).subscribe();

                    log.debug("User saved and cache invalidated: {}", savedUser.getUsername());
                });
    }

    @Override
    @Transactional
    public Mono<Void> deleteById(Long id) {
        log.debug("Deleting user with ID: {}", id);
        return userRepository.findById(id)
                .flatMap(user -> {
                    // Invalidate cache
                    String usernameKey = USER_CACHE_PREFIX + "username:" + user.getUsername();
                    String idKey = USER_CACHE_PREFIX + "id:" + id;

                    return redisTemplate.delete(usernameKey)
                            .then(redisTemplate.delete(idKey))
                            .then();
                })
                .then(userRoleRepository.findRoleIdsByUserId(id)
                        .flatMap(roleId -> userRoleRepository.delete(
                                UserRoleEntity.builder()
                                        .userId(id)
                                        .roleId(roleId)
                                        .build()
                        ))
                        .then(userRepository.deleteById(id)));
    }

    private Mono<User> enrichWithRoles(UserEntity userEntity) {
        log.debug("Enriching user with roles: {}", userEntity.getUsername());
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
                    log.debug("User enriched with {} roles: {}", roles.size(), userEntity.getUsername());
                    return user;
                });
    }
}
