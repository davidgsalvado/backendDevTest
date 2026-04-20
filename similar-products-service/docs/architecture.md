# Architecture Overview

## Introduction

The Similar Products Service provides customers with product recommendations based on the item they are currently viewing. It aggregates data from two upstream endpoints:

1. **`GET /product/{id}/similarids`** → returns a list of similar product IDs
2. **`GET /product/{id}`** → returns the detail of a single product

The service is built with **Spring Boot 3.5 + WebFlux** and follows **hexagonal architecture** (Ports & Adapters).

## The Problem

Some upstream product detail calls are **extremely slow** (up to 50 seconds of latency). Under high concurrency, this causes:

- Thread/connection pool exhaustion
- Cascading timeouts
- Degraded user experience across all requests (even for fast products)

## Implemented Solution

After evaluating six different approaches (timeout-based, SSE streaming, circuit breaker standalone, etc.), the implemented solution combines **four complementary patterns** that together address every dimension of the problem:

### 1. Request Coalescing (Singleflight Pattern)

**What it does**: When N concurrent users request the same slow product simultaneously, only **one HTTP call** is made to the upstream. All N callers subscribe to the same `Mono` and receive the shared result.

**Why**: Without this, 1000 concurrent users requesting product `10000` would generate 1000 upstream calls, each taking 50s. With coalescing, there is exactly 1 call.

**Implementation**: `ReactiveProductCache` uses a `ConcurrentHashMap<String, Mono<Product>>` for inflight requests. `Mono.cache()` from Project Reactor converts a cold Mono into a hot one, allowing multiple subscribers to share a single execution.

**Source**: [Project Reactor - Mono.cache()](https://projectreactor.io/docs/core/release/api/reactor/core/publisher/Mono.html#cache--), inspired by [Go's singleflight](https://pkg.go.dev/golang.org/x/sync/singleflight).

### 2. Stale-While-Revalidate (SWR)

**What it does**: When a cached entry expires, the **stale data is returned immediately** to the caller. A background refresh is triggered concurrently. The next caller gets fresh data.

**Why**: This ensures users **never wait** for slow upstream calls after the first cache population. The worst-case staleness is one TTL cycle (30 seconds).

**Implementation**: In `ReactiveProductCache.cache()`, when an entry is expired but present, it returns `Mono.just(staleProduct)` and calls `triggerBackgroundRefresh()` which subscribes fire-and-forget.

**Source**: Pattern from [HTTP Cache-Control: stale-while-revalidate (RFC 5861)](https://datatracker.ietf.org/doc/html/rfc5861), applied at application level.

### 3. Cache Warming (Pre-fetching)

**What it does**: A `@Scheduled` task runs every 25 seconds (before the 30s TTL expires), proactively fetching all product details into the cache. It discovers product IDs dynamically by querying the `similarids` endpoint for a set of seed products.

**Why**: Solves the **cold-start problem**. Without warming, the first user after a deployment or cache eviction would face the full 50s latency. With warming, the cache is populated before any user request arrives.

**Implementation**: `CacheWarmingScheduler` uses `@Scheduled(fixedDelay)` with configurable intervals via `application.yml`. It discovers products to warm by querying seed product similarids, then fetches each product detail directly (bypassing the circuit breaker to ensure warming works even when the circuit is open for user-facing traffic).

**Source**: [Spring Framework - @Scheduled](https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-scheduled).

**Design consideration - unknown product catalog**: In a real system the set of products may not be known in advance. An alternative implementation was evaluated where the scheduler
has **no seed products** and only re-warms products that have already entered the cache through organic user traffic:

```java
@Scheduled(fixedDelayString = "${cache.warming.interval-ms:25000}")
public void warmCache() {
    Set<String> knownIds = new HashSet<>(reactiveProductCache.getCachedProductIds());
    Flux.fromIterable(knownIds)
            .flatMap(this::fetchAndCache, 5)
            .subscribe();
}
```

This approach was not chosen because in this project the product catalog is known and small, and the cold-start penalty for slow products (up to 50s) is too severe to let the first
user absorb it. However, for a large or dynamic catalog where enumerating products upfront is impractical, the traffic-based warming approach is the recommended alternative - the 
coalescing and SWR patterns still protect all users after the first request per product.

### 4. Circuit Breaker (Resilience4j)

**What it does**: Monitors upstream call performance. If >80% of calls are slow (>5s) or >50% fail, the circuit **opens** and subsequent calls fail fast instead of waiting. After 30s, it enters half-open state and probes with 3 test calls.

**Why**: Protects the service from cascading failures. When the upstream is degraded, it prevents all threads from blocking on slow calls. Combined with SWR, the circuit opening means users get stale cached data instantly rather than waiting for inevitable timeouts.

**Implementation**: `CircuitBreakerConfiguration` defines the Resilience4j `CircuitBreaker` bean. `FetchProductsApiAdapter` wraps the upstream call with `CircuitBreakerOperator.of()`. When `CallNotPermittedException` fires (circuit open), the adapter returns `Mono.empty()` so the cache layer serves stale data.

**Source**: [Resilience4j Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker), [Resilience4j Reactor integration](https://resilience4j.readme.io/docs/examples-1).

## How the Patterns Work Together

```
User Request → Controller → UseCase → FetchProductsApiAdapter
                                          │
                                          ▼
                                   ReactiveProductCache
                                     │          │
                              Cache HIT?    Cache MISS?
                              (fresh)       (no entry)
                                 │               │
                                 ▼               ▼
                            Return          Request Coalescing
                           immediately     (share inflight call)
                                                 │
                              Cache HIT?         ▼
                              (stale)       Circuit Breaker
                                 │               │
                                 ▼               │
                            Return stale    ┌────┴────┐
                          + background     Open?    Closed?
                            refresh         │         │
                                            ▼         ▼
                                       Mono.empty  Upstream
                                       (use stale)  HTTP call
                                                      │
                                                      ▼
                                                 Cache SET
                                                 + return

          Background (every 25s):
          CacheWarmingScheduler → discovers products → fetches → cache.put()
```

### Request Lifecycle by Scenario

| Scenario | Latency | Data freshness |
|---|---|---|
| Cache HIT (fresh) | ~0ms | ≤30s old |
| Cache HIT (stale) + SWR | ~0ms | ≤60s old (refreshing) |
| Cache MISS + coalescing (Nth caller) | same as 1st caller | real-time |
| Cache MISS (1st caller) | upstream latency | real-time |
| Circuit OPEN + stale cache | ~0ms | last known good |
| Circuit OPEN + no cache | empty (skipped) | N/A |

## Hexagonal Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Domain Layer                        │
│  ┌─────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │ Product │  │ GetSimilarProd.  │  │ FetchProducts │  │
│  │ (entity)│  │ UseCase (port)   │  │ Port (port)   │  │
│  └─────────┘  └──────────────────┘  └───────────────┘  │
├─────────────────────────────────────────────────────────┤
│                   Application Layer                     │
│  ┌──────────────────────────────────────────────────┐   │
│  │ GetSimilarProductsUseCaseImpl (orchestration)    │   │
│  └──────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│                  Infrastructure Layer                   │
│  ┌──────────────────┐  ┌─────────────────────────────┐  │
│  │ Inbound Adapters │  │     Outbound Adapters       │  │
│  │                  │  │                             │  │
│  │ SimilarProducts  │  │ FetchProductsApiAdapter     │  │
│  │ Controller       │  │   + CircuitBreaker          │  │
│  │                  │  │   + ReactiveProductCache    │  │
│  │ CacheMonitor     │  │                             │  │
│  │ Controller       │  │ GetSimilarProductsApi       │  │
│  └──────────────────┘  │   Adapter                   │  │
│                        │                             │  │
│  ┌──────────────────┐  │ CacheWarmingScheduler       │  │
│  │  Configuration   │  └─────────────────────────────┘  │
│  │                  │                                   │
│  │ WebClientConfig  │                                   │
│  │ CircuitBreaker   │                                   │
│  │   Configuration  │                                   │
│  │ CacheConfig      │                                   │
│  └──────────────────┘                                   │
└─────────────────────────────────────────────────────────┘
```

## Why Not Other Solutions?

| Alternative | Why rejected |
|---|---|
| **Timeout + partial results** | User requirement is all products, not partial |
| **SSE / Streaming** | Requires frontend contract change; over-engineering for this use case |
| **Retry with backoff** (previous impl) | Retrying a 50s call 3 times can block for 2+ minutes; makes the problem worse |
| **Circuit Breaker alone** | Doesn't solve the fundamental latency; only manages failure. Combined with cache patterns it becomes effective |
| **Redis / external cache** | Adds infrastructure dependency not needed for a single-instance service |

## Configuration

All tuning parameters are externalized in `application.yml`:

| Property | Default | Purpose |
|---|---|---|
| `external.config.max-timeout` | 120s | Max time to wait for upstream (used by cache warming) |
| `cache.warming.interval-ms` | 25000 | How often to refresh the cache (should be < TTL) |
| `cache.warming.initial-delay-ms` | 0 | Delay before first warm-up (0 = immediate) |

## CAP Theorem Analysis

The [CAP theorem](https://en.wikipedia.org/wiki/CAP_theorem) states that a distributed system can only guarantee two of three properties simultaneously: **Consistency**, **Availability**, and **Partition Tolerance**.

> **Note**: CAP in its strict definition applies to distributed systems with replicated state. This service has no database or replicas, but the CAP tradeoff manifests in the relationship between the service and its upstream dependency — when the upstream becomes unreachable (a network partition), the service must choose between returning an error (consistency) or serving stale cached data (availability).

### This service is AP (Availability + Partition Tolerance)

**Consistency is explicitly relaxed** in favor of always returning a response:

| Mechanism | How it relaxes consistency |
|---|---|
| **Stale-While-Revalidate** | Returns expired cache entries immediately. Data can be up to ~60s stale (one full TTL cycle + SWR window) |
| **Cache Warming** | Pre-fetches every 25s with a 30s TTL — there is always a window where cached data doesn't reflect the latest upstream state |
| **Circuit Breaker → `Mono.empty()`** | When upstream is down, stale cached data is served instead of failing. If no cache exists, the product is silently skipped |
| **`onErrorContinue`** | Returns partial results when individual product fetches fail — the client receives an incomplete but usable response |

### Why this is the right tradeoff

1. **The domain tolerates eventual consistency**: product catalog data (name, price, availability) is not transactional. A price being 30 seconds stale has negligible business impact — this is not a financial ledger or inventory reservation system.

2. **Unavailability is more costly than inconsistency**: a `503 Service Unavailable` loses the user entirely. A slightly outdated product recommendation still provides value and keeps the user engaged.

3. **Industry-standard for read-heavy aggregators**: CDNs, API gateways, and BFF (Backend-for-Frontend) services universally adopt AP with SWR caching. This service follows the same well-proven pattern.

### Known limitations of this choice

- **No cache eviction policy**: entries in the `ConcurrentHashMap` are never removed, only overwritten on refresh. For a growing product catalog this could lead to unbounded memory growth. A bounded cache (e.g., Caffeine with `maximumSize`) would mitigate this.
- **Staleness is unbounded during extended outages**: if the upstream is down for hours and the circuit breaker stays open, the cache serves increasingly outdated data with no mechanism to signal staleness to the client (e.g., via `Cache-Control` or `Age` headers).
- **Silent data omission**: when a product has no cache entry and the circuit is open, it is silently dropped from results. The client has no way to know the response is incomplete.