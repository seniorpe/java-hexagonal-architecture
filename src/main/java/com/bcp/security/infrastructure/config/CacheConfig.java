package com.bcp.security.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.cache.support.NoOpCacheManager;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(60))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(
                                new GenericJackson2JsonRedisSerializer()));
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(cacheConfiguration())
                .withCacheConfiguration("users",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMinutes(10)))
                .withCacheConfiguration("roles",
                        RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofHours(24)))
                .build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "none", matchIfMissing = true)
    public CacheManager noOpCacheManager() {
        log.info("Configuring NoOpCacheManager as Redis is not available");
        return new NoOpCacheManager();
    }

    @Bean
    @ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> valueSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, Object> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, Object> context = builder
                .value(valueSerializer)
                .build();

        return new ReactiveRedisTemplate<>(factory, context);
    }
}
