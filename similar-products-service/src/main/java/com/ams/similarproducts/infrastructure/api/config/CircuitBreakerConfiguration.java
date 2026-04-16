package com.ams.similarproducts.infrastructure.api.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j Circuit Breaker configuration for upstream product API calls.
 *
 * The circuit breaker protects the service from cascading failures when the
 * upstream product detail API is slow or unavailable. When the circuit opens,
 * calls fail fast and the cache (stale-while-revalidate) serves the last
 * known good data.
 *
 * Configuration rationale:
 * - slowCallDurationThreshold(5s): product details taking >5s are considered slow
 * - slowCallRateThreshold(80%): if 80% of calls are slow, open the circuit
 * - failureRateThreshold(50%): if 50% of calls fail outright, open the circuit
 * - waitDurationInOpenState(30s): wait 30s before probing the upstream again
 * - slidingWindowSize(10): evaluate the last 10 calls
 *
 * @see <a href="https://resilience4j.readme.io/docs/circuitbreaker">Resilience4j Circuit Breaker docs</a>
 */
@Configuration
public class CircuitBreakerConfiguration {

    @Bean
    public CircuitBreaker productCircuitBreaker() {
        final CircuitBreakerConfig config = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .slowCallRateThreshold(80)
            .slowCallDurationThreshold(Duration.ofSeconds(5))
            .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
            .slidingWindowSize(10)
            .minimumNumberOfCalls(5)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(3)
            .automaticTransitionFromOpenToHalfOpenEnabled(true)
            .build();

        return CircuitBreaker.of("productDetailService", config);
    }
}
