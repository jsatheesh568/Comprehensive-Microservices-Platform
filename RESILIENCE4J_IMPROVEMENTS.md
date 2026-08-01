# Resilience4J Implementation Guide

## Overview
This document describes the comprehensive Resilience4J implementation across the microservices platform, including Circuit Breaker, Retry, Bulkhead, Time Limiter, and Rate Limiter patterns.

---

## 1. What is Resilience4J?

Resilience4J is a lightweight, easy-to-use fault tolerance library inspired by Netflix Hystrix, designed for functional programming. It provides decorators/annotations to enhance reliability of your services.

### Core Benefits:
- ✅ Prevents cascading failures
- ✅ Fast failure and recovery
- ✅ Resource isolation
- ✅ Better user experience
- ✅ Monitoring and observability

---

## 2. Resilience Patterns Implemented

### 2.1 Circuit Breaker Pattern
**Purpose**: Prevent cascading failures by stopping calls to failing services

**States**:
- **CLOSED**: Normal operation, all calls pass through
- **OPEN**: Service is failing, calls fail immediately without trying
- **HALF_OPEN**: Testing if service recovered, limited calls allowed

**Configuration Example**:
```yaml
resilience4j:
  circuitbreaker:
    configs:
      default:
        failure-rate-threshold: 50           # Open if 50% calls fail
        wait-duration-in-open-state: 10s     # Wait 10s before trying again
        sliding-window-size: 10              # Monitor last 10 calls
        minimum-number-of-calls: 5           # Need 5 calls before deciding
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
```

**When to Use**:
- Service-to-service calls (Feign clients)
- External API calls
- Database operations

**Implementation**:
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallback")
public PaymentResponse processPayment(PaymentRequest request) {
    // Protected code
}

public PaymentResponse fallback(PaymentRequest request, Exception ex) {
    return PaymentResponse.builder()
            .success(false)
            .message("Service unavailable")
            .build();
}
```

---

### 2.2 Retry Pattern
**Purpose**: Automatically retry failed operations with backoff strategy

**Strategies**:
- No delay
- Fixed delay
- Exponential backoff (wait time increases: 1s, 2s, 4s, 8s)
- Random delay

**Configuration Example**:
```yaml
resilience4j:
  retry:
    configs:
      default:
        max-attempts: 3                      # Try 3 times total
        wait-duration: 1s                    # Wait 1s between retries
        enable-exponential-backoff: true     # Use exponential backoff
        exponential-backoff-multiplier: 2    # Multiply by 2 each retry
        max-wait-duration: 10s               # Don't wait more than 10s
        retry-exceptions:
          - java.lang.RuntimeException       # Retry on these exceptions
```

**When to Use**:
- Transient network failures
- Temporary database unavailability
- Rate-limited API endpoints (with backoff)

**Implementation**:
```java
@Retry(name = "paymentService")
public PaymentResponse processPayment(PaymentRequest request) {
    // Will be retried 3 times with exponential backoff
}
```

---

### 2.3 Bulkhead Pattern
**Purpose**: Isolate resources to prevent one failing operation from consuming all resources

**Types**:
- **ThreadPool Isolation**: Separate thread pool for each operation
- **Semaphore Isolation**: Limit concurrent calls using semaphore

**Configuration Example**:
```yaml
resilience4j:
  bulkhead:
    configs:
      default:
        max-concurrent-calls: 20              # Max 20 concurrent calls
        max-wait-duration: 0                  # Don't wait for thread
        thread-pool:
          core-thread-pool-size: 5            # Start with 5 threads
          max-thread-pool-size: 15            # Scale to 15 threads
          queue-capacity: 10                  # Queue 10 requests
```

**When to Use**:
- Long-running operations
- Service-to-service calls
- Database operations
- Any operation that might block threads

**Implementation**:
```java
@Bulkhead(name = "inventoryProcessing", type = Bulkhead.Type.THREADPOOL)
public InventoryResponse reserveInventory(InventoryRequest request) {
    // Runs in isolated thread pool
    // Prevents blocking other operations
}
```

---

### 2.4 Time Limiter Pattern
**Purpose**: Set maximum execution time, fail fast if operation takes too long

**Configuration Example**:
```yaml
resilience4j:
  timelimiter:
    configs:
      default:
        timeout-duration: 5s                  # Timeout after 5 seconds
        cancel-running-future: true           # Cancel async operation
```

**When to Use**:
- Prevent indefinite waiting
- Database query timeouts
- Hanging network connections
- Async operations

**Implementation**:
```java
@TimeLimiter(name = "paymentProcessing")
@CircuitBreaker(name = "paymentProcessing", fallbackMethod = "fallback")
public CompletableFuture<PaymentResponse> processPaymentAsync(PaymentRequest request) {
    return CompletableFuture.supplyAsync(() -> {
        // Fails if takes more than 5 seconds
    });
}
```

---

### 2.5 Rate Limiter Pattern
**Purpose**: Limit number of calls per time period to prevent overwhelming services

**Configuration Example**:
```yaml
resilience4j:
  ratelimiter:
    configs:
      default:
        limit-refresh-period: 10s             # Reset every 10 seconds
        limit-for-period: 5                   # Allow 5 calls per period
        timeout-duration: 5s                  # Timeout if can't get permit
```

**When to Use**:
- Payment gateway integration
- Third-party API calls with rate limits
- Database connection pooling

**Implementation**:
```java
@RateLimiter(name = "paymentProcessing", fallbackMethod = "fallback")
public PaymentResponse processPayment(PaymentRequest request) {
    // Fails if more than 5 calls per 10 seconds
}
```

---

## 3. Service-Specific Implementations

### 3.1 Inventory Service
**URL**: Port 8083
**Role**: Provides inventory reservation and release (Saga participant)

**Resilience Patterns Applied**:
- ✅ Circuit Breaker (fail-safe on database errors)
- ✅ Retry (handle transient DB issues)
- ✅ Bulkhead (isolate inventory operations)
- ✅ Time Limiter (prevent hanging DB queries)

**Key Methods**:
- `reserveInventory()`: Reserves stock for order
- `releaseInventory()`: Releases stock (compensation)
- `getInventory()`: Retrieves inventory info

**Configuration**:
```yaml
# High threshold (60%) because inventory is critical
failure-rate-threshold: 60
# More bulkhead capacity (20 concurrent)
max-concurrent-calls: 20
# Longer timeout (3s) for DB operations
timeout-duration: 3s
```

**Use Cases**:
- Handles temporary database connection issues
- Prevents one slow inventory query from blocking entire service
- Gracefully degrades when database is slow

---

### 3.2 Payment Service
**URL**: Port 8082
**Role**: Processes payments with full resilience

**Resilience Patterns Applied**:
- ✅ Circuit Breaker (fail-safe on gateway errors)
- ✅ Retry (retry transient payment failures)
- ✅ Bulkhead (isolate payment operations)
- ✅ Rate Limiter (respect payment gateway limits)
- ✅ Time Limiter (timeout slow payment gateway)

**Key Methods**:
- `processPayment()`: Synchronous payment processing
- `processPaymentAsync()`: Asynchronous payment processing

**Configuration**:
```yaml
# Payment is critical: 50% threshold
failure-rate-threshold: 50
# Rate limit: 5 payments per 10 seconds
limit-for-period: 5
# Strict timeout (5s) for payment gateway
timeout-duration: 5s
```

**Use Cases**:
- Handles payment gateway temporary failures
- Prevents timeout on slow gateway responses
- Controls payment volume to prevent overload

---

### 3.3 Order Service
**URL**: Port 8081
**Role**: Orchestrates order creation using Saga pattern

**Resilience Patterns Applied**:
- ✅ Bulkhead (isolate order orchestration)
- ✅ Time Limiter (prevent hanging saga)
- ✅ Circuit Breaker (stop if too many failures)
- ✅ Retry (retry transient failures)

**Plus on Feign Clients**:
- **PaymentServiceClient**:
  - Circuit Breaker + Retry + Bulkhead + TimeLimiter
  - Smaller bulkhead (15 concurrent) as payment is critical
  - Shorter timeout (5s)

- **InventoryServiceClient**:
  - Circuit Breaker + Retry + Bulkhead + TimeLimiter
  - Larger bulkhead (20 concurrent) as inventory is frequent
  - Shorter timeout (4s)

**Configuration**:
```yaml
# Order orchestration: 25 concurrent
max-concurrent-calls: 25
# Saga max time: 10 seconds
timeout-duration: 10s
# Individual service timeouts:
# - Payment: 5s (critical)
# - Inventory: 4s (frequent)
```

**Use Cases**:
- Prevents order service thread starvation
- Fails fast if downstream services are slow
- Ensures saga completes in reasonable time

---

## 4. Pattern Combination and Order

### Execution Order (Patterns Applied in Reverse):
```
HTTP Request
    ↓
RateLimiter (if configured)
    ↓
Bulkhead (ThreadPool isolation)
    ↓
Retry (with backoff)
    ↓
CircuitBreaker (fail-fast)
    ↓
TimeLimiter (timeout check)
    ↓
Your Method
```

### Why This Order?
1. **RateLimiter First**: Fast rejection for rate-limited requests
2. **Bulkhead Early**: Isolate threads before expensive operations
3. **Retry**: After ensuring resources available
4. **CircuitBreaker**: Prevent repeated failed attempts
5. **TimeLimiter Last**: Final timeout guard on actual execution

### Example: Combining Patterns
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallback")
@Retry(name = "paymentService")
@Bulkhead(name = "paymentService", type = Bulkhead.Type.THREADPOOL)
@RateLimiter(name = "paymentService")
@TimeLimiter(name = "paymentService")
public PaymentResponse processPayment(PaymentRequest request) {
    // Protected by all 5 patterns
}
```

---

## 5. Configuration Best Practices

### 5.1 Threshold Settings
```yaml
# Conservative for critical services
failure-rate-threshold: 60        # Open circuit at 60% failure
permitted-number-of-calls-in-half-open-state: 2

# Aggressive for non-critical services
failure-rate-threshold: 40        # Open circuit at 40% failure
permitted-number-of-calls-in-half-open-state: 5
```

### 5.2 Timeout Strategies
```yaml
# Database operations
timeout-duration: 3s

# Network API calls
timeout-duration: 5s

# External payment gateways
timeout-duration: 5-10s

# Saga orchestration
timeout-duration: 10s
```

### 5.3 Retry Backoff
```yaml
# For transient network errors
enable-exponential-backoff: true
exponential-backoff-multiplier: 2
max-wait-duration: 10s

# Wait durations: 1s, 2s, 4s, 8s (then cap at 10s)
```

### 5.4 Thread Pool Sizing
```yaml
# High-frequency operations (Inventory)
core-thread-pool-size: 5
max-thread-pool-size: 15
queue-capacity: 10

# Low-frequency critical operations (Payment)
core-thread-pool-size: 5
max-thread-pool-size: 10
queue-capacity: 8

# Orchestration (Order Service)
core-thread-pool-size: 10
max-thread-pool-size: 20
queue-capacity: 15
```

---

## 6. Monitoring and Observability

### 6.1 Actuator Endpoints
Enable metrics for all resilience patterns:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,circuitbreakers,retries,bulkheads,timelimiters,ratelimiters
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
```

### 6.2 Available Metrics

**Circuit Breaker Metrics**:
- `resilience4j.circuitbreaker.calls` - Total calls
- `resilience4j.circuitbreaker.calls.successful` - Successful calls
- `resilience4j.circuitbreaker.calls.failed` - Failed calls
- `resilience4j.circuitbreaker.state` - Circuit state (CLOSED=0, OPEN=1, HALF_OPEN=2)

**Retry Metrics**:
- `resilience4j.retry.calls` - Retry call count
- `resilience4j.retry.calls.successful` - Successful retries

**Bulkhead Metrics**:
- `resilience4j.bulkhead.calls.allowed` - Allowed concurrent calls
- `resilience4j.bulkhead.calls.rejected` - Rejected due to full bulkhead

**Rate Limiter Metrics**:
- `resilience4j.ratelimiter.calls` - Total rate limiter calls
- `resilience4j.ratelimiter.calls.allowed` - Allowed calls
- `resilience4j.ratelimiter.calls.denied` - Denied calls

### 6.3 Viewing Metrics

**Via HTTP Endpoints**:
```bash
# Circuit breaker status
curl http://localhost:8081/actuator/circuitbreakers

# Detailed circuit breaker info
curl http://localhost:8081/actuator/circuitbreakers/paymentService

# Bulkhead metrics
curl http://localhost:8081/actuator/bulkheads

# All metrics in Prometheus format
curl http://localhost:8081/actuator/metrics
```

---

## 7. Common Failure Scenarios & Solutions

### Scenario 1: Payment Service Down
```
Without Resilience:
Request → Order Service → Payment Service (timeout) → Thread hangs → Service hangs

With Resilience:
Request → Order Service → Payment Service
    ↓ Fails
    ↓ Retry with backoff (3 attempts)
    ↓ Still fails
    ↓ Circuit opens
    ↓ Fail-fast on next request
    ↓ Return fallback response
    → User gets quick response
    → Other requests not affected
    → Service recovers when payment service is back
```

### Scenario 2: Database Connection Pool Exhausted
```
Without Resilience:
Request → Service → DB (all connections busy) → Request hangs → Thread starves

With Resilience:
Request → Service → DB
    ↓ Bulkhead prevents new threads
    ↓ Request rejected immediately
    ↓ Fail-fast
    ↓ Other requests succeed with available resources
```

### Scenario 3: Slow External Service
```
Without Resilience:
Request → Order Service → Payment Service (slow, 30s) → Thread hangs

With Resilience:
Request → Order Service → Payment Service
    ↓ TimeLimiter waits 5 seconds
    ↓ Timeout
    ↓ Return fallback response
    ↓ User gets response in 5s instead of 30s
```

---

## 8. Testing Resilience Patterns

### 8.1 Test Circuit Breaker
```bash
# Make 6 requests to trigger failure threshold
for i in {1..6}; do
  curl -X POST http://localhost:8082/api/payments/process \
    -H "Content-Type: application/json" \
    -d '{"orderId": 1, "amount": 100}'
done

# After 5+ failures (50% threshold), circuit opens
# Next request fails immediately
```

### 8.2 Test Bulkhead
```bash
# Make many parallel requests to exceed bulkhead capacity
for i in {1..30}; do
  curl -X POST http://localhost:8083/api/inventory/reserve \
    -H "Content-Type: application/json" \
    -d '{"productId": 1, "quantity": 1}' &
done

# Some requests are rejected by bulkhead
```

### 8.3 Test Time Limiter
```bash
# Service endpoint that takes 10 seconds
# But timeout is 5 seconds
curl -X POST http://localhost:8082/api/payments/process-slow \
  -H "Content-Type: application/json" \
  -d '{"orderId": 1, "amount": 100}'

# Returns timeout error after 5 seconds
```

---

## 9. Improvements & Enhancements

### 9.1 Inventory Service Improvements
**Before**: 
- No resilience patterns
- DB errors crash service
- Single thread pool shared across all operations

**After**:
- ✅ Circuit breaker prevents cascading DB failures
- ✅ Retry handles transient connection issues
- ✅ Bulkhead isolates inventory operations
- ✅ Time limiter prevents hanging queries
- ✅ Graceful fallback responses

**Metrics**: Can now monitor inventory operation health via actuator

---

### 9.2 Payment Service Enhancements
**Before**:
- Basic resilience (circuit breaker + retry only)

**After**:
- ✅ Full resilience stack (all 5 patterns)
- ✅ Rate limiting for payment gateway protection
- ✅ Multiple timeout configurations (default + high-throughput)
- ✅ Async support with time limiter
- ✅ Enhanced monitoring

**Metrics**: 30+ metrics available for payment processing health

---

### 9.3 Order Service Enhancements
**Before**:
- Feign clients had basic protection only
- No protection for saga orchestration itself

**After**:
- ✅ Orchestration protected by bulkhead + time limiter
- ✅ Individual client timeouts (5s payment, 4s inventory)
- ✅ Bulkhead for payment (15 concurrent) and inventory (20 concurrent)
- ✅ Comprehensive fallback handling
- ✅ Better resource isolation

**Benefits**: 
- Saga completes or fails within 10 seconds
- No thread starvation
- Graceful degradation

---

## 10. Production Deployment Checklist

- [ ] All services have resilience4j dependencies
- [ ] Configuration files updated with all patterns
- [ ] Fallback methods implemented and tested
- [ ] Actuator endpoints exposed for monitoring
- [ ] Prometheus metrics enabled
- [ ] Monitoring dashboards set up
- [ ] Alert thresholds configured
- [ ] Load testing completed
- [ ] Failure scenarios tested
- [ ] Runbooks documented for on-call team
- [ ] Documentation reviewed by team

---

## 11. References

- [Resilience4j Official Docs](https://resilience4j.readme.io/)
- [Spring Cloud Resilience4j Integration](https://spring.io/projects/spring-cloud-circuitbreaker)
- [Circuit Breaker Pattern - Martin Fowler](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Release It! - Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/)

---

## 12. FAQ

**Q: Why use bulkhead for inventory if it's a single instance?**
A: Bulkhead protects *other* services by isolating inventory threads. Even if inventory queries hang, order service threads remain available for other requests.

**Q: Why is payment service timeout shorter (5s) than inventory (4s)?**
A: Payment is more critical and external. Inventory is internal/database. Shorter timeout for payment ensures quick failure without waiting for retry.

**Q: Can I tune these settings?**
A: Yes! Monitor metrics and adjust thresholds based on your SLAs and performance targets. Start conservative, then optimize.

**Q: What if circuit breaker is open but I need to call the service?**
A: Use the fallback method to return cached data or queue the request for retry later.

---

**Last Updated**: 2026-08-01
**Maintainer**: Platform Team
