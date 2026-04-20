package com.ams.similarproducts.infrastructure.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheMonitorControllerTest {

    @Mock
    private ReactiveProductCache reactiveProductCache;

    @InjectMocks
    private CacheMonitorController cacheMonitorController;

    @Test
    void getCacheStats_shouldReturnStats() {
        final Map<String, Object> stats = Map.of(
                "size", 2,
                "inflightRequests", 0,
                "cachedProductIds", Set.of("1", "2")
        );

        when(reactiveProductCache.getStats()).thenReturn(stats);

        final Map<String, Object> result = cacheMonitorController.getCacheStats();

        assertEquals("Reactive Product Cache", result.get("cacheType"));
        assertEquals(stats, result.get("stats"));
        verify(reactiveProductCache).getStats();
    }

    @Test
    void clearCache_shouldClearAndReturnSuccess() {
        final Map<String, String> result = cacheMonitorController.clearCache();

        assertEquals("success", result.get("status"));
        assertEquals("Cache cleared successfully", result.get("message"));
        verify(reactiveProductCache).clear();
    }
}
