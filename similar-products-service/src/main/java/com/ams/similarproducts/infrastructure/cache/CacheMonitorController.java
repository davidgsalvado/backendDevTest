package com.ams.similarproducts.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller para monitorear el estado del caché reactivo en tiempo real.
 * Accesible en: GET /cache/stats
 */
@RestController
@RequestMapping("/cache")
@RequiredArgsConstructor
@Slf4j
public class CacheMonitorController {

    private final ReactiveProductCache reactiveProductCache;

    @GetMapping("/stats")
    public Map<String, Object> getCacheStats() {
        log.info("[CacheMonitor] Retrieving cache stats");
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("cacheType", "Reactive Product Cache");
        stats.put("stats", reactiveProductCache.getStats());
        
        return stats;
    }

    @GetMapping("/clear")
    public Map<String, String> clearCache() {
        log.info("[CacheMonitor] Clearing cache");
        reactiveProductCache.clear();
        
        return Map.of("status", "success", "message", "Cache cleared successfully");
    }
}

