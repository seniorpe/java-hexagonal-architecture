package com.bcp.security.infrastructure.adapter.persistence.repository;

import com.bcp.security.infrastructure.adapter.persistence.entity.RoleEntity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface R2dbcRoleRepository extends R2dbcRepository<RoleEntity, Long> {
    Mono<RoleEntity> findByName(String name);
}
