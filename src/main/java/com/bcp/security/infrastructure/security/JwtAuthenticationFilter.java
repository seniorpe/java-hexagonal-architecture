package com.bcp.security.infrastructure.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider tokenProvider;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    // Lista de rutas públicas que no requieren autenticación
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String method = exchange.getRequest().getMethod().name();

        log.debug("Processing request: {} {}", method, path);

        // Si la ruta es pública, no intentamos autenticar
        if (isPublicPath(path)) {
            log.debug("Public path detected, skipping authentication: {}", path);
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange.getRequest());

        if (StringUtils.hasText(token)) {
            log.debug("JWT token found in request");
            if (tokenProvider.validateToken(token)) {
                log.debug("JWT token is valid");
                Authentication authentication = tokenProvider.getAuthentication(token);
                log.info("Setting authentication in SecurityContext for user: {}",
                        authentication.getName());
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
            } else {
                log.warn("Invalid JWT token");
            }
        } else {
            log.debug("No JWT token found in request");
        }
        return chain.filter(exchange);
    }

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
