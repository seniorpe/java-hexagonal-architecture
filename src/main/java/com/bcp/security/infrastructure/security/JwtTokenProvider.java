package com.bcp.security.infrastructure.security;

import com.bcp.security.domain.model.Role;
import com.bcp.security.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.stream.Collectors;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long tokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long tokenValidityInMilliseconds) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.tokenValidityInMilliseconds = tokenValidityInMilliseconds;
        log.info("JWT Token Provider initialized with token validity of {} ms", tokenValidityInMilliseconds);
    }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        log.debug("Generating JWT token for authenticated user: {}", username);

        String authorities = extractAuthorities(authentication.getAuthorities());
        return createToken(username, authorities);
    }

    public String generateToken(User user) {
        String username = user.getUsername();
        log.debug("Generating JWT token for user: {}", username);

        String authorities = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(","));

        return createToken(username, authorities);
    }

    private String createToken(String subject, String authorities) {
        log.debug("Creating token with authorities: {}", authorities);

        long now = System.currentTimeMillis();
        Date validity = new Date(now + tokenValidityInMilliseconds);

        String token = Jwts.builder()
                .setSubject(subject)
                .claim("auth", authorities)
                .signWith(key, SignatureAlgorithm.HS512)
                .setExpiration(validity)
                .compact();

        log.debug("JWT token created successfully");
        return token;
    }

    private String extractAuthorities(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
    }

    public Authentication getAuthentication(String token) {
        log.debug("Extracting authentication from token");

        Claims claims = parseToken(token);
        String username = claims.getSubject();

        Collection<? extends GrantedAuthority> authorities = extractAuthoritiesFromClaims(claims);

        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User(username, "", authorities);

        log.debug("Authentication created for user: {}", username);
        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    private Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Collection<? extends GrantedAuthority> extractAuthoritiesFromClaims(Claims claims) {
        String authoritiesString = claims.get("auth").toString();
        log.debug("Authorities from token: {}", authoritiesString);

        return Arrays.stream(authoritiesString.split(","))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            log.debug("Token validation successful");
            return true;
        } catch (Exception e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
