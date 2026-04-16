package com.ams.similarproducts.infrastructure.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de caché.
 * 
 * NOTA: Ya no usamos Spring Cache @Cacheable porque no es compatible con métodos reactivos (Mono/Flux).
 * En su lugar, usamos ReactiveProductCache que implementa caché nativa de Reactor.
 */
@Configuration
@Slf4j
public class CacheConfig {

    public CacheConfig() {
        log.info("[CacheConfig] Using Reactive Product Cache (Reactor-native)");
    }
}

