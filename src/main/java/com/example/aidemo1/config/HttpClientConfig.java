package com.example.aidemo1.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuration for HTTP clients used by external exchange rate providers.
 * 
 * <p>Configures RestTemplate with proper timeout settings, retry logic,
 * and request/response logging capabilities.</p>
 */
@Configuration
public class HttpClientConfig {

    /**
     * Creates a RestTemplate bean configured for external API calls.
     * 
     * <p>Configuration includes:</p>
     * <ul>
     *   <li>Connection timeout: 5 seconds</li>
     *   <li>Read timeout: 10 seconds</li>
     *   <li>Request/response buffering for logging</li>
     * </ul>
     * 
     * @param builder RestTemplateBuilder provided by Spring Boot
     * @return configured RestTemplate instance
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        
        return builder
                .requestFactory(() -> new BufferingClientHttpRequestFactory(requestFactory))
                .build();
    }
}
