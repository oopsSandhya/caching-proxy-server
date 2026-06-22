# 🚀 Caching Proxy Server

![Java](https://img.shields.io/badge/Java-21-orange?style=for-the-badge&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-green?style=for-the-badge&logo=springboot)
![Redis](https://img.shields.io/badge/Redis-7.0-red?style=for-the-badge&logo=redis)
![Maven](https://img.shields.io/badge/Maven-3.9-blue?style=for-the-badge&logo=apachemaven)
![Docker](https://img.shields.io/badge/Docker-29.5-blue?style=for-the-badge&logo=docker)

---

## 📌 What is this project?

A **Caching Proxy Server** is a server that sits **between a client and an origin server**.

Instead of hitting the origin server every single time, the proxy:
1. **Receives** the client's HTTP request
2. **Checks** if the response is already cached
3. If **YES (HIT)** → returns the cached response instantly ⚡
4. If **NO (MISS)** → forwards to origin, caches the response, then returns it

This help reduce:
- ⏱️ Response time (from ~1000ms to ~9ms in tests)
- 🔁 Redundant network calls to origin
- 💸 Origin server load and cost

---

## ❓ Why did I build this?

Caching is one of the most important concepts in backend engineering.
Every large-scale system — Google, Netflix, Amazon — uses caching heavily.

This project demonstrates:
- How a proxy server works internally
- How caching reduces latency
- Thread-safe programming with `ConcurrentHashMap`
- Distributed caching with Redis
- Real HTTP header manipulation (`X-Cache: HIT/MISS`)
- TTL-based cache expiration
- Layered Spring Boot architecture (Controller, Service, Config, Model)

---

## 🧠 How does it work? (Full Flow)
Client (Postman / Browser / Any HTTP client)

│

│  GET /proxy/posts/1

▼

┌─────────────────────────┐

│   Caching Proxy Server  │  ← Our Spring Boot app (port 8081)

│                         │

│  1. Extract path        │

│  2. Build cache key     │

│  3. Check Redis cache   │

│                         │

│   ┌─────────────────┐   │

│   │   Redis Cache   │   │

│   └────────┬────────┘   │

│            │            │

│       HIT? │ MISS?      │

└────────────┼────────────┘

│

┌──────┴──────┐

│             │

HIT           MISS

│             │

│    ┌────────▼────────┐

│    │  Origin Server  │

│    │ jsonplaceholder  │

│    │   .typicode.com │

│    └────────┬────────┘

│             │

│    Store in Redis

│    (with TTL = 60s)

│             │

└──────┬──────┘

│

Return Response

+ X-Cache: HIT/MISS

+ X-Proxy-By: CachingProxy/1.0

│

▼

Client

---

## ✨ Features

| Feature | Description |
|---|---|
| 🔁 **HTTP Forwarding** | Forwards all HTTP methods to origin server |
| ⚡ **In-Memory Cache** | ConcurrentHashMap for thread-safe caching |
| 🔴 **Redis Cache** | Distributed cache — survives server restarts |
| 🏷️ **X-Cache Headers** | Every response has `HIT` or `MISS` header |
| ⏱️ **TTL Expiration** | Entries auto-expire after 60 seconds |
| 🧹 **Scheduled Eviction** | Background thread runs every 30 seconds |
| 📊 **Cache Statistics** | Hit count, miss count, hit ratio endpoint |
| 🗑️ **Cache Clear** | Manual cache invalidation endpoint |
| 🏥 **Health Check** | Spring Actuator health endpoint |
| 🛡️ **Error Handling** | 502 Bad Gateway on origin failure |
| ⏰ **Timeout Config** | 3s connect, 5s read — fail-fast approach |

---

## 🛠️ Tech Stack

| Technology | Version | Why used |
|---|---|---|
| **Java** | 21 | Latest LTS — Virtual threads, Records support |
| **Spring Boot** | 3.2.5 | Backend application framework with dependency injection and auto-configuration |
| **Spring Web** | 6.1.6 | REST controllers, RestTemplate for HTTP calls |
| **Redis** | 7.0 |External cache store used for fast response retrieval |
| **Spring Data Redis** | 3.2.5 | Spring's Redis integration layer |
| **Docker** | 29.5 | Runs Redis container locally |
| **Lombok** | Latest | Eliminates boilerplate (@Getter, @Builder, @Slf4j) |
| **Spring Actuator** | 3.2.5 | Health check and monitoring end points |
| **Maven** | 3.9 | Build and dependency management |

---

## 📁 Project Structure
caching-proxy-server/

├── src/

│   └── main/

│       ├── java/

│       │   └── com/sandhya/cachingproxy/

│       │       ├── CachingProxyApplication.java   # Entry point + @EnableScheduling

│       │       │

│       │       ├── config/

│       │       │   ├── AppConfig.java             # RestTemplate bean with timeouts

│       │       │   └── RedisConfig.java           # RedisTemplate configuration

│       │       │

│       │       ├── controller/

│       │       │   ├── ProxyController.java       # Handles ALL proxy requests /**

│       │       │   └── CacheController.java       # Cache stats, clear, health

│       │       │

│       │       ├── model/

│       │       │   ├── CachedResponse.java        # Stores body + status + headers + timestamp

│       │       │   └── CacheStats.java            # AtomicLong hit/miss counters

│       │       │

│       │       └── service/

│       │           ├── CacheService.java          # Redis get/put/clear/evict logic

│       │           └── ProxyService.java          # HTTP forwarding via RestTemplate

│       │

│       └── resources/

│           └── application.properties             # All configuration

│

├── pom.xml                                        # Maven dependencies

└── README.md                                      # This file

---

## 🏗️ Architecture — Layer by Layer

### Controller Layer
Receives all HTTP requests. Makes the HIT/MISS decision. Adds X-Cache header.

**ProxyController** — mapped to `/proxy/**`
- Extracts path, query string, headers from incoming request
- Checks cache first for GET requests
- On MISS: delegates to ProxyService, then stores result in CacheService
- Builds response with X-Cache and X-Proxy-By headers

**CacheController** — mapped to `/proxy/cache`
- `GET /proxy/cache/stats` → returns hit/miss/size/ratio
- `DELETE /proxy/cache` → clears all entries
- `GET /proxy/cache/health` → cache health status

### Service Layer
Core business logic lives here.

**CacheService**
- Uses `RedisTemplate<String, String>` to interact with Redis
- Cache key format: `proxy:cache:GET:https://origin.com/path`
- Serializes `CachedResponse` to JSON for Redis storage
- Deserializes JSON back to `CachedResponse` on retrieval
- Scheduled cleanup every 30 seconds (Redis handles TTL natively)

**ProxyService**
- Builds target URL: `originUrl + path + ?queryString`
- Filters safe headers to forward (Content-Type, Accept, Authorization)
- Uses `RestTemplate.exchange()` for HTTP calls
- Handles 4xx, 5xx, and network errors gracefully

### Model Layer
Plain data classes.

**CachedResponse** — what we store in cache
- `body` → response body as String
- `status` → HTTP status code
- `headers` → response headers
- `cachedAt` → timestamp for TTL calculation

**CacheStats** — performance tracking
- `AtomicLong hitCount` → thread-safe hit counter
- `AtomicLong missCount` → thread-safe miss counter
- `getHitRatio()` → hits / (hits + misses)

---

## 🧠 Key Design Decisions & Interview Concepts

### Why ConcurrentHashMap over HashMap?
HashMap          → NOT thread-safe → race conditions under concurrent requests

synchronizedMap  → locks ENTIRE map → poor throughput

ConcurrentHashMap → thread-safe access with better performance than synchronizedMap under concurrent requests

### Why only cache GET requests?
GET    → Idempotent, safe → CACHEABLE ✅

POST   → Creates resources → NOT cacheable ❌

PUT    → Mutates data → NOT cacheable ❌

DELETE → Mutates data → NOT cacheable ❌
Caching a POST would mean: second identical POST returns cached response
without hitting origin → resource never created → DATA INTEGRITY VIOLATION.

### Why Redis over pure in-memory?
ConcurrentHashMap → Lost on restart, single instance only

Redis             → Survives restarts, shared across multiple instances

Built-in TTL, persistence, pub/sub support

### Why not cache error responses?
Caching a 503 Service Unavailable means:

Next user ALSO gets 503 even if origin has recovered!

Always fetch fresh on errors → better user experience.

### Cache Key Design
Format: "METHOD:FULL_URL"

Example: "GET:https://jsonplaceholder.typicode.com/posts/1"
Why include method?

GET  /posts/1 → fetch post    (cacheable)

POST /posts/1 → update post   (NOT cacheable)

Same URL, completely different operations!

### Timeout Configuration (Fail-Fast)
connectTimeout = 3 seconds  → max time to establish TCP connection

readTimeout    = 5 seconds  → max time to receive full response
Why? If origin takes 30s to respond:

Without timeout → all proxy threads stuck → proxy crashes

With timeout    → fail fast → return error → system stays healthy

### AtomicLong for Statistics
Plain long + hitCount++ → RACE CONDITION under concurrent requests

Two threads increment simultaneously

One increment gets lost!
AtomicLong.incrementAndGet() → Single atomic CPU instruction

No thread blocking

No data loss

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version | Download |
|---|---|---|
| Java JDK | 21 | https://adoptium.net |
| Docker | Any | https://docker.com |
| Maven | 3.9+ | Included (mvnw) |

### Step 1 — Clone the repository
```bash
git clone https://github.com/oopsSandhya/caching-proxy-server.git
cd caching-proxy-server
```

### Step 2 — Start Redis via Docker
```bash
docker run -d -p 6379:6379 --name redis-cache redis:latest
```

### Step 3 — Run the application
```bash
# Windows
.\mvnw spring-boot:run

# Mac/Linux
./mvnw spring-boot:run
```

### Step 4 — Verify it's running
http://localhost:8081/actuator/health
Should return: `{"status":"UP"}`

---

## 📡 API Reference

### 1. Proxy Any Request
GET http://localhost:8081/proxy/{path}

| Parameter | Description |
|---|---|
| `path` | Any path on the origin server |

**Example:**
```bash
curl http://localhost:8081/proxy/posts/1
```

**Response Headers:**
X-Cache: MISS          ← first request (fetched from origin)

X-Cache: HIT           ← subsequent requests (served from cache)

X-Proxy-By: CachingProxy/1.0

Content-Type: application/json

**Response Body:**
```json
{
  "userId": 1,
  "id": 1,
  "title": "sunt aut facere repellat provident",
  "body": "quia et suscipit..."
}
```

---

### 2. Cache Statistics
GET http://localhost:8081/proxy/cache/stats

**Response:**
```json
{
  "hitCount": 10,
  "missCount": 2,
  "cacheSize": 2,
  "hitRatio": "83.33%",
  "summary": "CacheStats{hits=10, misses=2, size=2, hitRatio=83.33%}"
}
```

| Field | Description |
|---|---|
| `hitCount` | Total requests served from cache |
| `missCount` | Total requests forwarded to origin |
| `cacheSize` | Current entries in cache |
| `hitRatio` | Percentage of requests served from cache |

---

### 3. Clear Cache
DELETE http://localhost:8081/proxy/cache

**Response:**
```json
{
  "status": "success",
  "message": "Cache cleared successfully",
  "entries": "0"
}
```

**When to use:**
- After deploying new version of origin service
- When cached data is known to be stale
- During testing

---

### 4. Cache Health
GET http://localhost:8081/proxy/cache/health

**Response:**
```json
{
  "status": "UP",
  "cacheSize": 5,
  "cacheType": "ConcurrentHashMap (In-Memory)",
  "message": "5 entries cached"
}
```

---

### 5. Application Health
GET http://localhost:8081/actuator/health

---

## ⚙️ Configuration Reference

All configuration lives in `src/main/resources/application.properties`

```properties
# Server
server.port=8081

# Origin server — change this to proxy any REST API
proxy.origin.url=https://jsonplaceholder.typicode.com

# Cache TTL — how long entries stay in cache (seconds)
proxy.cache.ttl.seconds=60

# Redis connection
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=2000ms

# Actuator — expose all monitoring endpoints
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always

# Logging — set to INFO in production
logging.level.com.sandhya.cachingproxy=DEBUG
```

---

## 📊 Performance Results

Tested with `GET /proxy/posts/1` against `jsonplaceholder.typicode.com`:

| Request | Response Time | Source |
|---|---|---|
| 1st request (MISS) | ~1000ms | Origin server |
| 2nd request (HIT) | ~9ms | Redis cache |
| **Improvement** | **~111x faster** | — |

---

## 🗺️ Roadmap

- [x] Core proxy with ConcurrentHashMap cache
- [x] X-Cache HIT/MISS headers
- [x] TTL expiration
- [x] Scheduled cache eviction
- [x] Redis distributed cache
- [x] Cache statistics endpoint
- [ ] Unit & Integration tests
- [ ] Docker Compose (app + Redis together)
- [ ] Rate limiting
- [ ] Cache warming on startup
- [ ] Grafana + Prometheus monitoring

---

## 👩‍💻 Author

**Sandhya** 

- GitHub: [@oopsSandhya](https://github.com/oopsSandhya)

---

## 📄 License

 [MIT License](LICENSE).