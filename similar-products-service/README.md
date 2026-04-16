# Similar Products Service

REST API service that provides product detail for similar products given a product ID. Built with **Spring Boot 3.5**, **WebFlux**, and **Java 21**.

## Problem Statement

We want to show customers products similar to the one they are viewing. Two upstream APIs exist:

- `GET /product/{id}/similarids` → list of similar product IDs
- `GET /product/{id}` → product detail (id, name, price, availability)

This service aggregates both into a single endpoint: `GET /product/{id}/similar`.

### The Challenge

Some upstream product detail responses are **extremely slow** (1s, 5s, and up to 50s). Under high concurrency, this degrades performance for all users — even those requesting fast products.

## Solution

The service implements **four complementary patterns** that together provide fast responses, full data, and resilience:

| Pattern | Purpose |
|---|---|
| **Request Coalescing** | 1000 concurrent requests for the same slow product → 1 upstream call |
| **Stale-While-Revalidate** | Expired cache returns stale data instantly; refreshes in background |
| **Cache Warming** | Scheduled task pre-fetches products every 25s so cache is always hot |
| **Circuit Breaker** | Protects against cascading failures; fails fast when upstream is degraded |

### Why this combination?

Six approaches were evaluated. See [docs/architecture.md](docs/architecture.md) for the full analysis and justification. In summary:

- **Timeout + partial results** was rejected because the requirement is to return all products, not a subset.
- **SSE streaming** was rejected because it requires a different frontend contract.
- **Retry with backoff** was removed because retrying a 50s call makes latency worse.
- **Cache-only** solutions don't guarantee data freshness.
- **Circuit Breaker alone** manages failure but doesn't solve latency.

The chosen combination covers all failure modes: cold start (warming), thundering herd (coalescing), slow upstream (SWR + circuit breaker), and upstream downtime (circuit breaker + stale cache).

## Architecture

The service follows **hexagonal architecture** (Ports & Adapters):

```
Domain:          Product, GetSimilarProductsUseCase, FetchProductsPort
Application:     GetSimilarProductsUseCaseImpl
Infrastructure:  Controllers, WebClient adapters, Cache, Circuit Breaker, Scheduler
```

See [docs/architecture.md](docs/architecture.md) for detailed diagrams and component descriptions.

## API

### Get Similar Products

```
GET /product/{productId}/similar
```

**Response** `200 OK`:
```json
[
  {
    "id": "2",
    "name": "Dress",
    "price": 19.99,
    "availability": true
  },
  {
    "id": "3",
    "name": "Blazer",
    "price": 29.99,
    "availability": false
  }
]
```

### Cache Stats (monitoring)

```
GET /cache/stats
```

```json
{
  "cacheType": "Reactive Product Cache",
  "stats": {
    "size": 7,
    "inflightRequests": 0,
    "cachedProductIds": ["1", "2", "3", "4", "100", "1000", "10000"]
  }
}
```

## Getting Started

### Prerequisites

- Java 21
- Docker & Docker Compose

### Run

1. Start the mock upstream API and monitoring infrastructure:
   ```bash
   docker-compose up -d simulado influxdb grafana
   ```

2. Verify the mock is running:
   ```bash
   curl http://localhost:3001/product/1/similarids
   # Expected: [2,3,4]
   ```

3. Run the service:
   ```bash
   cd similar-products-service
   ./mvnw spring-boot:run
   ```

4. Test the endpoint:
   ```bash
   curl http://localhost:5000/product/1/similar
   ```

### Performance Tests

```bash
docker-compose run --rm k6 run scripts/test.js
```

View results at: http://localhost:3000/d/Le2Ku9NMk/k6-performance-test

> **Note**: k6 will report ~600 interrupted iterations. This is expected - the `normal`, `notFound` and `error` scenarios are configured with `gracefulStop: 0s, which means
k6 forcefully terminates all in-flight iterations (200 VUs x 3 scenarios = 600) the instant each scenario's duration ends. It does not indicate a service error.

## Configuration

All parameters are tunable in `application.yml`:

| Property | Default | Description |
|---|---|---|
| `external.products.base-url` | `http://localhost:3001/product` | Upstream API base URL |
| `external.config.max-timeout` | `120` | Max timeout in seconds for upstream calls |
| `cache.warming.interval-ms` | `25000` | Cache warm-up frequency (ms) |
| `cache.warming.initial-delay-ms` | `0` | Delay before first warm-up (0 = immediate) |

Circuit Breaker parameters are configured in `CircuitBreakerConfiguration.java`:

| Parameter | Value | Description |
|---|---|---|
| `slowCallDurationThreshold` | 5s | Calls slower than this are "slow" |
| `slowCallRateThreshold` | 80% | % of slow calls to open circuit |
| `failureRateThreshold` | 50% | % of failures to open circuit |
| `waitDurationInOpenState` | 30s | Time before probing upstream again |
| `slidingWindowSize` | 10 | Number of calls evaluated |

## Tech Stack

- **Java 21** + **Spring Boot 3.5** + **Spring WebFlux**
- **Project Reactor** (reactive streams)
- **Resilience4j** (circuit breaker)
- **MapStruct** (DTO mapping)
- **Lombok** (boilerplate reduction)
- **Caffeine** (available for future cache needs)
- **Docker Compose** (infrastructure)
- **k6** + **Grafana** + **InfluxDB** (performance testing & dashboards)