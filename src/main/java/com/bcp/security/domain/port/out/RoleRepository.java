package com.bcp.security.domain.port.out;

import com.bcp.security.domain.model.Role;
import reactor.core.publisher.Mono;

public interface RoleRepository {
    Mono<Role> findByName(String name);
}
