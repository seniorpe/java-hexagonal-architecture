package com.bcp.security.infrastructure.adapter.persistence;

import com.bcp.security.domain.model.Role;
import com.bcp.security.domain.port.out.RoleRepository;
import com.bcp.security.infrastructure.adapter.persistence.mapper.RoleMapper;
import com.bcp.security.infrastructure.adapter.persistence.repository.R2dbcRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final R2dbcRoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final ReactiveRedisTemplate<String, Object> redisTemplate;

    private static final String ROLE_CACHE_PREFIX = "role:";
    private static final Duration CACHE_TTL = Duration.ofHours(24);

    @Override
    public Mono<Role> findByName(String name) {
        log.debug("Finding role by name: {}", name);
        String cacheKey = ROLE_CACHE_PREFIX + name;

        return redisTemplate.opsForValue().get(cacheKey)
                .cast(Role.class)
                .doOnNext(role -> log.debug("Role found in cache: {}", name))
                .switchIfEmpty(
                        Mono.defer(() -> {
                            log.debug("Role not found in cache, querying database: {}", name);
                            return roleRepository.findByName(name)
                                    .map(roleMapper::toDomain)
                                    .doOnNext(role -> {
                                        log.debug("Caching role: {}", name);
                                        redisTemplate.opsForValue().set(cacheKey, role, CACHE_TTL).subscribe();
                                    });
                        })
                )
                .doOnError(e -> log.error("Error finding role by name: {}", name, e));
    }
}
