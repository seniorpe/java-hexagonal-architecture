package com.bcp.security.infrastructure.adapter.persistence.mapper;

import com.bcp.security.domain.model.Role;
import com.bcp.security.infrastructure.adapter.persistence.entity.RoleEntity;
import org.springframework.stereotype.Component;

@Component
public class RoleMapper {

    public Role toDomain(RoleEntity entity) {
        return Role.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public RoleEntity toEntity(Role domain) {
        return RoleEntity.builder()
                .id(domain.getId())
                .name(domain.getName())
                .build();
    }
}
