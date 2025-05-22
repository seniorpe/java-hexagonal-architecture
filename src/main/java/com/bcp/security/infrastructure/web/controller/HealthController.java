package com.bcp.security.infrastructure.web.controller;

import com.bcp.security.infrastructure.web.dto.response.ApiResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Mono<ApiResponse<Map<String, String>>> healthCheck() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "Security Microservice");

         logger.info("Este es un mensaje de log de nivel INFO");
        logger.warn("Este es un mensaje de log de nivel WARN");
        logger.error("Este es un mensaje de log de nivel ERROR");
        logger.debug("Este es un mensaje de log de nivel DEBUG");
        logger.trace("Este es un mensaje de log de nivel TRACE");

        return Mono.just(ApiResponse.success(status));
    }
}
