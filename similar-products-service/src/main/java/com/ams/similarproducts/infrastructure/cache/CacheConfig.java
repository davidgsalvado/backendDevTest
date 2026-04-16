package com.ams.similarproducts.infrastructure.cache;

import org.springframework.context.annotation.Configuration;

/**
 * Cache configuration.
 *
 * Spring @Cacheable is not used because it is not compatible with reactive types (Mono/Flux).
 * Instead, {@link ReactiveProductCache} provides a Reactor-native cache with:
 * - Request Coalescing (Singleflight) via Mono.cache()
 * - Stale-While-Revalidate pattern
 * - Background warming via {@link CacheWarmingScheduler}
 */
@Configuration
public class CacheConfig {
}
