package com.bcp.security.infrastructure.adapter.persistence;

import com.bcp.security.domain.model.Role;
import com.bcp.security.domain.port.out.RoleRepository;
import com.bcp.security.infrastructure.adapter.persistence.mapper.RoleMapper;
import com.bcp.security.infrastructure.adapter.persistence.repository.R2dbcRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {

    private final R2dbcRoleRepository roleRepository;
    private final RoleMapper roleMapper;

    @Override
    public Mono<Role> findByName(String name) {
        return roleRepository.findByName(name)
                .map(roleMapper::toDomain);
    }
}
