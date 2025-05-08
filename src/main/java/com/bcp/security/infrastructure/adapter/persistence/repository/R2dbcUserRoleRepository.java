package com.bcp.security.infrastructure.adapter.persistence.repository;

import com.bcp.security.infrastructure.adapter.persistence.entity.UserRoleEntity;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface R2dbcUserRoleRepository extends R2dbcRepository<UserRoleEntity, Long> {

    @Query("SELECT role_id FROM user_roles WHERE user_id = :userId")
    Flux<Long> findRoleIdsByUserId(Long userId);
}
