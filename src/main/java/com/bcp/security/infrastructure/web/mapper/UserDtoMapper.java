package com.bcp.security.infrastructure.web.mapper;

import com.bcp.security.domain.model.Role;
import com.bcp.security.domain.model.User;
import com.bcp.security.infrastructure.web.dto.request.RegisterRequest;
import com.bcp.security.infrastructure.web.dto.response.UserResponse;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserDtoMapper {

    public User toDomain(RegisterRequest request) {
        User user = User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .email(request.getEmail())
                .build();

        if (request.getRoles() != null && !request.getRoles().isEmpty()) {
            Set<Role> roles = request.getRoles().stream()
                    .map(roleName -> Role.builder().name(roleName).build())
                    .collect(Collectors.toSet());
            user.setRoles(roles);
        }

        return user;
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(user.getRoles() != null ?
                        user.getRoles().stream()
                                .map(role -> role.getName())
                                .collect(Collectors.toSet()) :
                        null)
                .createdAt(user.getCreatedAt())
                .build();
    }
}
