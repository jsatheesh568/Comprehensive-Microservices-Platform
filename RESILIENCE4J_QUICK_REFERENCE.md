# Resilience4J Quick Reference Guide

## When to Use Each Pattern

### Circuit Breaker ✅
**Use When**:
- Calling external services that might fail
- Database operations might fail
- Want to prevent cascading failures
- Need fast-fail behavior

**Example**:
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "fallback")
public PaymentResponse process(PaymentRequest req) { }
```

**Metrics to Watch**:
- `circuitbreaker.state` (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `circuitbreaker.calls.failed`
- `circuitbreaker.calls.successful`

---

### Retry ✅
**Use When**:
- Dealing with transient failures (network glitches)
- External API has temporary issues
- Want automatic recovery with backoff

**Example**:
```java
@Retry(name = "inventoryService")
public void reserveInventory(InventoryRequest req) { }
```

**Configuration Tips**:
- Use exponential backoff for external services
- Set reasonable max-attempts (3-5)
- Consider max-wait-duration to prevent long delays

**Metrics to Watch**:
- `retry.calls` - Total retry attempts
- `retry.calls.successful` - Successful retries

---

### Bulkhead ✅
**Use When**:
- Long-running operations that might block
- Want to prevent thread starvation
- Need to isolate different operation types
- Using thread pool isolation (not just semaphore)

**Example**:
```java
@Bulkhead(name = "paymentService", type = Bulkhead.Type.THREADPOOL)
public PaymentResponse process(PaymentRequest req) { }
```

**Configuration Tips**:
- Set `max-concurrent-calls` based on expected throughput
- Start with small `core-thread-pool-size`, scale as needed
- Queue capacity = burst traffic buffer

**Metrics to Watch**:
- `bulkhead.calls.allowed` - Calls allowed through
- `bulkhead.calls.rejected` - Calls rejected (bulkhead full)

---

### Time Limiter ⏱️
**Use When**:
- Operations might hang (slow DB, network delays)
- Want to fail-fast on slow responses
- Using async/CompletableFuture
- Strict SLA on response time

**Example**:
```java
@TimeLimiter(name = "paymentService")
@CircuitBreaker(name = "paymentService")
public CompletableFuture<PaymentResponse> processAsync(PaymentRequest req) { }
```

**Configuration Tips**:
- Set timeout based on acceptable latency
- Always pair with CircuitBreaker for sync methods
- For async, must return CompletableFuture

**Metrics to Watch**:
- Timeout events in logs
- Request latency percentiles

---

### Rate Limiter 🚦
**Use When**:
- External API has rate limits
- Want to control request throughput
- Protecting downstream service capacity
- Implementing fair-share resource allocation

**Example**:
```java
@RateLimiter(name = "externalAPI", fallbackMethod = "fallback")
public void callExternalAPI(Request req) { }
```

**Configuration Tips**:
- `limit-for-period` = max calls per refresh period
- `limit-refresh-period` = when limits reset
- Adjust based on downstream service capacity

**Metrics to Watch**:
- `ratelimiter.calls.allowed`
- `ratelimiter.calls.denied`

---

## Pattern Combinations

### Minimum Protection (External Services)
```java
@CircuitBreaker(name = "service")
@Retry(name = "service")
public Response call() { }
```
✅ Fast failure, auto-retry  
❌ No timeout, no resource isolation

---

### Recommended Protection (Important Operations)
```java
@CircuitBreaker(name = "service", fallbackMethod = "fallback")
@Retry(name = "service")
@Bulkhead(name = "service")
@TimeLimiter(name = "service")
public Response call() { }
```
✅ Fast failure, retry, isolated, timeout  
✅ Resource protected, cascading failures prevented

---

### Maximum Protection (Critical Operations)
```java
@CircuitBreaker(name = "service", fallbackMethod = "fallback")
@Retry(name = "service")
@Bulkhead(name = "service")
@RateLimiter(name = "service")
@TimeLimiter(name = "service")
public Response call() { }
```
✅ All 5 patterns  
✅ Maximum resilience and protection

---

## Configuration Quick Reference

### Conservative (Critical Services)
```yaml
circuitbreaker:
  failure-rate-threshold: 60    # High threshold
  wait-duration-in-open-state: 15s
  
bulkhead:
  max-concurrent-calls: 10      # Few concurrent
  core-thread-pool-size: 3

timelimiter:
  timeout-duration: 3s          # Short timeout
```

---

### Balanced (Normal Services)
```yaml
circuitbreaker:
  failure-rate-threshold: 50
  wait-duration-in-open-state: 10s
  
bulkhead:
  max-concurrent-calls: 20
  core-thread-pool-size: 5

timelimiter:
  timeout-duration: 5s
```

---

### Aggressive (High-Throughput Services)
```yaml
circuitbreaker:
  failure-rate-threshold: 40    # Low threshold
  wait-duration-in-open-state: 5s
  
bulkhead:
  max-concurrent-calls: 50      # Many concurrent
  core-thread-pool-size: 10

timelimiter:
  timeout-duration: 10s         # Long timeout
```

---

## Service-Specific Settings in This Platform

### Inventory Service (Port 8083)
```
Circuit Breaker: 60% threshold (conservative)
Retry: 3 attempts, exponential backoff
Bulkhead: Max 20 concurrent, 5-15 threads
Time Limiter: 3 seconds
```
**Why**: Database is critical, need high threshold but prevent hanging

### Payment Service (Port 8082)
```
Circuit Breaker: 50% threshold
Retry: 3 attempts, exponential backoff
Bulkhead: Max 10 concurrent, 5-10 threads
Rate Limiter: 5 per 10 seconds
Time Limiter: 5 seconds
```
**Why**: External gateway, strict rate limits, fast timeout needed

### Order Service (Port 8081)
```
Orchestration Bulkhead: Max 25 concurrent
Orchestration Time Limiter: 10 seconds
Payment Client Time Limiter: 5 seconds (critical)
Inventory Client Time Limiter: 4 seconds (frequent)
```
**Why**: Saga must complete quickly, service-specific timeouts

---

## Monitoring Checklists

### Daily Monitoring
- [ ] Check circuit breaker states - any OPEN?
- [ ] Monitor retry rates - increasing?
- [ ] Check bulkhead rejections - any?
- [ ] Look for timeout events - increasing?

### Weekly Review
- [ ] Analyze failure rate trends
- [ ] Review slowest operations
- [ ] Check resource utilization
- [ ] Validate threshold effectiveness

### Before Deployment
- [ ] Run load tests
- [ ] Verify all fallback methods work
- [ ] Check metrics exports
- [ ] Alert thresholds configured

---

## Troubleshooting

### Circuit Breaker Stuck OPEN
```
Symptom: All requests failing immediately
Cause: Too many repeated failures
Fix:
  1. Check downstream service health
  2. Check application logs for errors
  3. Increase wait-duration-in-open-state
  4. Lower failure-rate-threshold if too aggressive
```

### Bulkhead Always Rejecting
```
Symptom: Many 429 Too Many Requests
Cause: Not enough threads in pool
Fix:
  1. Increase max-thread-pool-size
  2. Increase queue-capacity
  3. Reduce operation time (optimize code)
  4. Increase max-concurrent-calls
```

### High Timeout Rate
```
Symptom: Many operations timing out
Cause: Operations taking too long or network slow
Fix:
  1. Increase timeout-duration
  2. Optimize operation performance
  3. Check network latency
  4. Scale up downstream service
  5. Reduce load (rate limit earlier)
```

### Rate Limiter Always Rejecting
```
Symptom: 429 Too Many Requests errors
Cause: Limit too strict for actual load
Fix:
  1. Increase limit-for-period
  2. Increase limit-refresh-period
  3. Reduce client request frequency
  4. Implement client-side batching
```

---

## Performance Impact

### Circuit Breaker
- **Overhead**: Negligible (<1ms)
- **Memory**: ~100 bytes per instance
- **State Check**: O(1) atomic operation

### Retry
- **Overhead**: Only on failures
- **Wait**: Configurable exponential backoff
- **Total Time**: Original + (wait × retry-attempts)

### Bulkhead (ThreadPool)
- **Overhead**: Thread pool management
- **Memory**: ~1MB per thread
- **Context Switch**: Minimal with proper sizing

### Time Limiter
- **Overhead**: Negligible on sync
- **Impact**: CompletableFuture overhead on async
- **Timing**: Precise timeout enforcement

### Rate Limiter
- **Overhead**: Atomic operation (~1μs)
- **Memory**: O(1) per instance
- **Permits**: Generated at configured rate

---

## Example: Adding Resilience to New Method

### Step 1: Identify the service call
```java
public OrderResponse createOrder(CreateOrderRequest req) {
    return paymentService.processPayment(paymentReq);  // External call
}
```

### Step 2: Choose patterns
- Circuit Breaker ✅ (prevent cascading)
- Retry ✅ (handle transient failures)
- Bulkhead ✅ (isolate threads)
- Time Limiter ✅ (prevent hanging)
- Rate Limiter ❌ (no rate limits needed)

### Step 3: Add annotations
```java
@CircuitBreaker(name = "paymentService", fallbackMethod = "createOrderFallback")
@Retry(name = "paymentService")
@Bulkhead(name = "paymentService", type = Bulkhead.Type.THREADPOOL)
@TimeLimiter(name = "paymentService")
public OrderResponse createOrder(CreateOrderRequest req) {
    return paymentService.processPayment(paymentReq);
}
```

### Step 4: Add configuration
```yaml
resilience4j:
  circuitbreaker:
    instances:
      paymentService:
        baseConfig: default
  retry:
    instances:
      paymentService:
        baseConfig: default
  bulkhead:
    instances:
      paymentService:
        max-concurrent-calls: 15
        thread-pool:
          core-thread-pool-size: 5
          max-thread-pool-size: 10
  timelimiter:
    instances:
      paymentService:
        timeout-duration: 5s
```

### Step 5: Add fallback
```java
public OrderResponse createOrderFallback(CreateOrderRequest req, Exception ex) {
    log.error("Payment service unavailable", ex);
    return OrderResponse.builder()
            .success(false)
            .message("Order service temporarily unavailable")
            .build();
}
```

### Step 6: Test & Monitor
- Load test with failures
- Monitor metrics via actuator
- Adjust thresholds based on traffic patterns

---

## Useful Endpoints

```bash
# Check all circuit breaker states
curl http://localhost:8081/actuator/circuitbreakers

# Check specific circuit breaker
curl http://localhost:8081/actuator/circuitbreakers/paymentService

# Get retry statistics
curl http://localhost:8081/actuator/retries

# Get bulkhead status
curl http://localhost:8081/actuator/bulkheads

# Get rate limiter status
curl http://localhost:8081/actuator/ratelimiters

# Get time limiter status
curl http://localhost:8081/actuator/timelimiters

# All metrics in Prometheus format
curl http://localhost:8081/actuator/metrics

# Pretty print specific metric
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.calls
```

---

## References

- Full Guide: See `RESILIENCE4J_IMPROVEMENTS.md`
- Implementation Summary: See `RESILIENCE_IMPLEMENTATION_SUMMARY.md`
- Official Docs: https://resilience4j.readme.io/

---

**Last Updated**: 2026-08-01
**Quick Reference Version**: 1.0
