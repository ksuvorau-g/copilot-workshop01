package com.example.aidemo1.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis configuration for caching exchange rate data.
 * 
 * This configuration:
 * - Enables Spring Cache abstraction with Redis
 * - Configures RedisTemplate for direct cache operations
 * - Sets up JSON serialization for cached objects
 * - Defines TTL (Time To Live) for cached entries
 */
@Configuration
@EnableCaching
public class RedisConfig {

    /**
     * Configure RedisTemplate with custom serializers.
     * 
     * Uses String serialization for keys and JSON serialization for values.
     * This allows storing complex objects while maintaining readable keys.
     * 
     * @param connectionFactory Redis connection factory provided by Spring Boot
     * @return Configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for keys to ensure readable keys in Redis
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Use JSON serializer for values to store complex objects
        GenericJackson2JsonRedisSerializer jsonSerializer = createJsonSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Configure RedisCacheManager for Spring's cache abstraction.
     * 
     * Sets default cache configuration with:
     * - 1 hour TTL (aligned with scheduled rate refresh)
     * - JSON serialization for cached values
     * - String serialization for cache keys
     * 
     * @param connectionFactory Redis connection factory
     * @return Configured RedisCacheManager
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // Cache expires after 1 hour (scheduler refreshes hourly)
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(createJsonSerializer())
                );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }

    /**
     * Create JSON serializer with proper configuration for LocalDateTime and Redis caching.
     * 
     * Configures ObjectMapper with:
     * - JavaTimeModule for Java 8 date/time support (LocalDateTime, etc.)
     * - RESTRICTED polymorphic type handling for secure deserialization
     * 
     * Why we need polymorphic types:
     * RedisTemplate stores values as Object, so Jackson needs type information
     * to deserialize back to the correct class (ExchangeRate). Without this,
     * it would deserialize to LinkedHashMap instead of ExchangeRate.
     * 
     * Security:
     * We use a RESTRICTED validator that only allows our specific entity classes
     * (com.example.aidemo1.entity.*) to prevent arbitrary class deserialization attacks.
     * This is much more secure than allowing all classes with Object.class base.
     * 
     * @return Configured GenericJackson2JsonRedisSerializer
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        // Register JavaTimeModule to handle LocalDateTime, LocalDate, etc.
        objectMapper.registerModule(new JavaTimeModule());
        
        // Enable type information BUT restrict to only our entity package for security
        // This prevents deserialization attacks while allowing proper type reconstruction
        PolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("com.example.aidemo1.entity") // Only allow our entity classes
                .allowIfSubType("java.time") // Allow Java time classes (LocalDateTime, etc.)
                .allowIfSubType("java.math") // Allow BigDecimal
                .build();
        
        objectMapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL, // Only apply to non-final types
                JsonTypeInfo.As.PROPERTY // Store type as "@class" property
        );

        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
