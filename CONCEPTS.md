# Microservices Concepts - Quick Reference

This document provides a quick reference to all concepts implemented in this project.

## 🏛️ Architecture Patterns

### 1. Microservices Architecture
- **Definition**: Building applications as a suite of small, independent services
- **Benefits**: Scalability, technology diversity, independent deployment
- **Implementation**: 6 independent services (Eureka, Gateway, Order, Payment, Inventory, Notification)

### 2. Service Discovery (Eureka)
- **Pattern**: Central registry for service location
- **Implementation**: `eureka-server` module
- **Key Annotations**: 
  - `@EnableEurekaServer` - Server side
  - `@EnableDiscoveryClient` - Client side
- **Benefits**: Dynamic service discovery, load balancing, health monitoring

### 3. API Gateway Pattern
- **Definition**: Single entry point for all client requests
- **Implementation**: `api-gateway` using Spring Cloud Gateway
- **Features**: 
  - Routing
  - Security (JWT)
  - Load balancing
  - Request/Response transformation
- **Benefits**: Centralized cross-cutting concerns

## 🔄 Communication Patterns

### 4. Synchronous Communication (REST/Feign)
- **Pattern**: Request-Response
- **Implementation**: Feign clients in `order-service`
- **Use Cases**: 
  - Payment processing (needs immediate response)
  - Inventory reservation (needs confirmation)
- **Trade-offs**: 
  - ✅ Simple, immediate feedback
  - ❌ Tight coupling, both services must be available

### 5. Asynchronous Communication (RabbitMQ)
- **Pattern**: Event-driven messaging
- **Implementation**: RabbitMQ between `order-service` and `notification-service`
- **Use Cases**: 
  - Notifications (fire-and-forget)
  - Event logging
  - Analytics
- **Trade-offs**: 
  - ✅ Decoupled, scalable, resilient
  - ❌ Eventual consistency, complexity

## 🛡️ Resilience Patterns (Resilience4j)

### 6. Circuit Breaker Pattern
- **Purpose**: Prevent cascading failures
- **States**: 
  - CLOSED: Normal operation
  - OPEN: Service failing, fail fast
  - HALF_OPEN: Testing recovery
- **Implementation**: `@CircuitBreaker` in `payment-service`
- **Configuration**: `resilience4j.circuitbreaker`

### 7. Retry Pattern
- **Purpose**: Handle transient failures
- **Implementation**: `@Retry` with exponential backoff
- **Configuration**: `resilience4j.retry`
- **Benefits**: Automatic recovery from temporary failures

### 8. Bulkhead Pattern
- **Purpose**: Isolate resources to prevent total failure
- **Types**: 
  - ThreadPool: Separate thread pool
  - Semaphore: Semaphore-based isolation
- **Implementation**: `@Bulkhead` in `payment-service`
- **Configuration**: `resilience4j.bulkhead`

### 9. Rate Limiter Pattern
- **Purpose**: Limit number of calls per time period
- **Implementation**: `@RateLimiter`
- **Configuration**: `resilience4j.ratelimiter`
- **Use Cases**: API throttling, preventing overload

### 10. Time Limiter Pattern
- **Purpose**: Set maximum execution time
- **Implementation**: `@TimeLimiter` with `CompletableFuture`
- **Configuration**: `resilience4j.timelimiter`
- **Use Cases**: Prevent hanging operations

## 📦 Transaction Management

### 11. Saga Pattern
- **Definition**: Pattern for managing distributed transactions
- **Type**: Orchestration-based (central coordinator)
- **Implementation**: `order-service` orchestrates saga
- **Steps**:
  1. Reserve Inventory → Compensation: Release Inventory
  2. Process Payment → Compensation: Refund Payment
  3. Create Order
  4. Send Notification (async, no compensation)
- **Benefits**: 
  - No distributed locks
  - Better scalability
  - Handles long-running transactions
- **Trade-offs**: Eventual consistency, complex error handling

## 🔐 Security

### 12. Spring Boot Security
- **Implementation**: JWT-based authentication in API Gateway
- **Components**:
  - `SecurityConfig` - Security configuration
  - `AuthenticationFilter` - JWT validation
- **Features**: 
  - Token validation
  - Role-based access control
  - CORS handling

## 📊 Spring Boot Annotations Reference

### Application Level
| Annotation | Purpose |
|------------|---------|
| `@SpringBootApplication` | Main application annotation |
| `@EnableDiscoveryClient` | Enable Eureka client |
| `@EnableFeignClients` | Enable Feign clients |
| `@EnableAsync` | Enable async execution |
| `@EnableEurekaServer` | Enable Eureka server |

### Component Level
| Annotation | Purpose |
|------------|---------|
| `@Service` | Business logic component |
| `@Repository` | Data access component |
| `@Controller` / `@RestController` | Web controller |
| `@Component` | Generic Spring component |
| `@Configuration` | Configuration class |

### Data Layer
| Annotation | Purpose |
|------------|---------|
| `@Entity` | JPA entity |
| `@Table` | Database table mapping |
| `@Id` | Primary key |
| `@GeneratedValue` | Auto-generate ID |
| `@Column` | Column mapping |
| `@Transactional` | Transaction boundary |

### Web Layer
| Annotation | Purpose |
|------------|---------|
| `@RequestMapping` | Base URL mapping |
| `@GetMapping` | HTTP GET mapping |
| `@PostMapping` | HTTP POST mapping |
| `@PathVariable` | URL path variable |
| `@RequestParam` | Query parameter |
| `@RequestBody` | Request body |
| `@Valid` | Trigger validation |

### Resilience4j
| Annotation | Purpose |
|------------|---------|
| `@CircuitBreaker` | Circuit breaker pattern |
| `@Retry` | Retry pattern |
| `@Bulkhead` | Bulkhead pattern |
| `@RateLimiter` | Rate limiting |
| `@TimeLimiter` | Timeout handling |

### Messaging
| Annotation | Purpose |
|------------|---------|
| `@RabbitListener` | RabbitMQ consumer |
| `@EnableRabbit` | Enable RabbitMQ |

## 🔍 Key Design Decisions

### Why Saga over 2PC?
- **2PC (Two-Phase Commit)**: Requires distributed locks, poor scalability
- **Saga**: No locks, better scalability, eventual consistency

### Why Orchestration over Choreography?
- **Orchestration**: Centralized control, easier to understand
- **Choreography**: Decoupled, but harder to track

### Why Feign over RestTemplate?
- **Feign**: Declarative, integrates with Eureka, cleaner code
- **RestTemplate**: More verbose, manual service discovery

### Why RabbitMQ over Kafka?
- **RabbitMQ**: Better for request-response, simpler setup
- **Kafka**: Better for high-throughput event streaming

## 📈 Scalability Considerations

1. **Horizontal Scaling**: All services can be scaled independently
2. **Load Balancing**: Eureka provides client-side load balancing
3. **Async Processing**: RabbitMQ enables async, non-blocking operations
4. **Resource Isolation**: Bulkhead pattern prevents resource exhaustion

## 🚨 Error Handling Strategies

1. **Circuit Breaker**: Fail fast when service is down
2. **Retry**: Handle transient failures
3. **Fallback**: Provide default responses
4. **Saga Compensation**: Rollback distributed transactions
5. **Dead Letter Queue**: Handle failed messages (RabbitMQ)

## 📚 Further Reading

- [Microservices Patterns](https://microservices.io/patterns/)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Resilience4j Guide](https://resilience4j.readme.io/)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)
- [Circuit Breaker Pattern](https://martinfowler.com/bliki/CircuitBreaker.html)

---

**This project demonstrates production-ready microservices patterns with comprehensive code explanations.**
