# Comprehensive Microservices Platform

A complete Spring Boot microservices platform demonstrating enterprise-level patterns and best practices. This project is designed for senior developers to understand all key microservices concepts with detailed code explanations and comments.

## 🏗️ Architecture Overview

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       ▼
┌─────────────────────────────────┐
│      API Gateway (8080)          │
│  - Spring Cloud Gateway          │
│  - Security (JWT)                │
│  - Routing                       │
└──────┬───────────────────────────┘
       │
       ▼
┌─────────────────────────────────┐
│   Eureka Server (8761)          │
│   Service Discovery              │
└─────────────────────────────────┘
       │
       ├─────────────────┬─────────────────┬─────────────────┐
       ▼                 ▼                 ▼                 ▼
┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐
│   Order     │  │  Payment    │  │  Inventory  │  │Notification │
│  Service    │  │  Service    │  │  Service    │  │  Service    │
│   (8081)    │  │   (8082)    │  │   (8083)    │  │   (8084)    │
│             │  │             │  │             │  │             │
│ - Saga      │  │ - Circuit   │  │ - Sync REST │  │ - Async     │
│   Pattern   │  │   Breaker   │  │ - Reserve/  │  │   RabbitMQ  │
│ - Feign     │  │ - Retry     │  │   Release   │  │ - Consumer  │
│ - RabbitMQ  │  │ - Bulkhead  │  │             │  │             │
└─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘
       │                 │                 │                 │
       └─────────────────┴─────────────────┴─────────────────┘
                                    │
                                    ▼
                          ┌─────────────────┐
                          │    RabbitMQ     │
                          │  Message Broker │
                          └─────────────────┘
```

## 📚 Concepts Covered

### 1. **Service Discovery (Eureka)**
- **What**: Central registry where services register themselves and discover other services
- **Why**: Eliminates hardcoded URLs, enables dynamic service discovery, load balancing
- **Implementation**: `eureka-server` module
- **Key Annotations**: `@EnableEurekaServer`, `@EnableDiscoveryClient`

### 2. **API Gateway (Spring Cloud Gateway)**
- **What**: Single entry point for all client requests
- **Why**: Centralized routing, security, rate limiting, request/response transformation
- **Implementation**: `api-gateway` module
- **Features**: 
  - Dynamic routing based on service discovery
  - JWT authentication
  - CORS handling
  - Load balancing

### 3. **Spring Boot Security**
- **What**: Authentication and authorization framework
- **Why**: Protect APIs, manage user access
- **Implementation**: `api-gateway/src/main/java/com/gateway/config/SecurityConfig.java`
- **Concepts**: 
  - JWT token validation
  - Role-based access control
  - CORS configuration
  - CSRF protection

### 4. **Saga Pattern**
- **What**: Pattern for managing distributed transactions across microservices
- **Why**: No distributed locks, better scalability, handles long-running transactions
- **Implementation**: `order-service` orchestrates saga
- **Saga Steps**:
  1. Reserve Inventory (compensation: Release Inventory)
  2. Process Payment (compensation: Refund Payment)
  3. Create Order
  4. Send Notification (async, no compensation)
- **Type**: Orchestration-based (central coordinator)

### 5. **Resilience4j Patterns**

#### Circuit Breaker
- **What**: Prevents cascading failures by opening circuit when service fails
- **States**: CLOSED → OPEN → HALF_OPEN
- **Implementation**: `payment-service` with `@CircuitBreaker` annotation
- **Configuration**: `resilience4j.circuitbreaker` in `application.yml`

#### Retry Pattern
- **What**: Automatically retries failed operations
- **Why**: Handle transient failures
- **Implementation**: `@Retry` annotation with exponential backoff
- **Configuration**: `resilience4j.retry` in `application.yml`

#### Bulkhead Pattern
- **What**: Isolates resources (thread pools) to prevent resource exhaustion
- **Types**: ThreadPool (this implementation) and Semaphore
- **Implementation**: `@Bulkhead` annotation
- **Configuration**: `resilience4j.bulkhead` in `application.yml`

#### Rate Limiter
- **What**: Limits number of calls per time period
- **Why**: Prevent overwhelming downstream services
- **Implementation**: `@RateLimiter` annotation
- **Configuration**: `resilience4j.ratelimiter` in `application.yml`

#### Time Limiter
- **What**: Sets maximum execution time for operations
- **Why**: Prevent hanging operations
- **Implementation**: `@TimeLimiter` with `CompletableFuture`
- **Configuration**: `resilience4j.timelimiter` in `application.yml`

### 6. **Synchronous Communication (REST/Feign)**
- **What**: Request-response pattern using HTTP
- **Why**: Need immediate response, simple integration
- **Implementation**: 
  - `order-service` uses Feign clients to call `payment-service` and `inventory-service`
  - `@FeignClient` annotation
- **Features**: 
  - Service discovery integration
  - Load balancing
  - Automatic retry with Resilience4j

### 7. **Asynchronous Communication (RabbitMQ)**
- **What**: Event-driven messaging pattern
- **Why**: Decoupling, non-blocking, better scalability
- **Implementation**: 
  - `order-service` publishes messages
  - `notification-service` consumes messages
- **Concepts**: 
  - Exchange (routes messages)
  - Queue (stores messages)
  - Binding (connects exchange to queue)
  - Producer/Consumer pattern

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.6+
- Docker & Docker Compose (optional, for containerized deployment)
- RabbitMQ (or use Docker Compose)

### Running Locally

#### Step 1: Start RabbitMQ
```bash
# Using Docker
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3-management

# Or use docker-compose (includes all services)
docker-compose up rabbitmq
```

#### Step 2: Build All Services
```bash
# From project root
mvn clean install
```

#### Step 3: Start Services in Order

**Terminal 1 - Eureka Server:**
```bash
cd eureka-server
mvn spring-boot:run
# Access at http://localhost:8761
```

**Terminal 2 - API Gateway:**
```bash
cd api-gateway
mvn spring-boot:run
# Runs on http://localhost:8080
```

**Terminal 3 - Order Service:**
```bash
cd order-service
mvn spring-boot:run
# Runs on http://localhost:8081
```

**Terminal 4 - Payment Service:**
```bash
cd payment-service
mvn spring-boot:run
# Runs on http://localhost:8082
```

**Terminal 5 - Inventory Service:**
```bash
cd inventory-service
mvn spring-boot:run
# Runs on http://localhost:8083
```

**Terminal 6 - Notification Service:**
```bash
cd notification-service
mvn spring-boot:run
# Runs on http://localhost:8084
```

### Running with Docker Compose
```bash
# Build all services
mvn clean install

# Start all services
docker-compose up

# Or in detached mode
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

## 📖 API Endpoints

### API Gateway (Port 8080)
All requests go through the API Gateway.

### Order Service
- `POST /api/orders` - Create order (triggers Saga pattern)
- `GET /api/orders/{id}` - Get order by ID
- `GET /api/orders?userId={userId}` - Get orders by user
- `POST /api/auth/login` - Mock authentication (returns JWT token)

### Payment Service
- `POST /api/payments/process` - Process payment (sync, with all resilience patterns)
- `POST /api/payments/process-async` - Process payment (async, with time limiter)
- `GET /api/payments/{transactionId}` - Get payment by transaction ID

### Inventory Service
- `POST /api/inventory/reserve` - Reserve inventory (Saga step)
- `POST /api/inventory/release` - Release inventory (Saga compensation)
- `GET /api/inventory/{productId}` - Get inventory for product

### Notification Service
- `GET /api/notifications/user/{userId}` - Get notifications for user

## 🧪 Testing the System

### 1. Check Eureka Dashboard
Visit http://localhost:8761 to see all registered services.

### 2. Create an Order (Saga Pattern)
```bash
# First, get a token (mock authentication)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"pass"}'

# Create an order (replace TOKEN with actual token)
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TOKEN" \
  -d '{
    "userId": 1,
    "productId": 1,
    "quantity": 2
  }'
```

This will:
1. Reserve inventory (sync call to inventory-service)
2. Process payment (sync call to payment-service with circuit breaker, retry, bulkhead)
3. Create order
4. Send notification (async via RabbitMQ)

### 3. Test Circuit Breaker
Stop the payment-service and try creating orders. The circuit breaker will open after failures.

### 4. Test Async Communication
Check RabbitMQ Management UI at http://localhost:15672 (guest/guest) to see messages.

### 5. View Resilience4j Metrics
```bash
# Circuit breaker state
curl http://localhost:8082/actuator/circuitbreakers

# Retry metrics
curl http://localhost:8082/actuator/retries

# Bulkhead metrics
curl http://localhost:8082/actuator/bulkheads
```

## 📝 Key Spring Boot Annotations Explained

### Application Level
- `@SpringBootApplication` - Combines `@Configuration`, `@EnableAutoConfiguration`, `@ComponentScan`
- `@EnableDiscoveryClient` - Enables Eureka client
- `@EnableFeignClients` - Enables Feign clients for service-to-service calls
- `@EnableAsync` - Enables asynchronous method execution

### Service Layer
- `@Service` - Marks class as a Spring service component
- `@Transactional` - Ensures database operations are atomic
- `@RequiredArgsConstructor` - Lombok: generates constructor for final fields

### Controller Layer
- `@RestController` - Combines `@Controller` and `@ResponseBody`
- `@RequestMapping` - Base path for all endpoints
- `@GetMapping`, `@PostMapping` - HTTP method mappings
- `@PathVariable` - Extracts variable from URL path
- `@RequestParam` - Extracts query parameter
- `@RequestBody` - Deserializes JSON to Java object
- `@Valid` - Triggers validation

### Data Layer
- `@Entity` - Marks class as JPA entity
- `@Table` - Specifies database table name
- `@Id` - Primary key
- `@GeneratedValue` - Auto-generates primary key
- `@Column` - Maps field to database column
- `@Repository` - Marks interface as Spring Data repository

### Resilience4j
- `@CircuitBreaker` - Wraps method with circuit breaker
- `@Retry` - Automatically retries failed calls
- `@Bulkhead` - Isolates method execution in thread pool
- `@RateLimiter` - Limits call rate
- `@TimeLimiter` - Sets timeout for async operations

### Messaging
- `@RabbitListener` - Marks method as RabbitMQ message consumer
- `@Component` - Marks class as Spring component

## 🔍 Code Structure

```
comprehensive-microservices-platform/
├── eureka-server/              # Service Discovery
│   └── src/main/java/com/eureka/
│       └── EurekaServerApplication.java
│
├── api-gateway/                # API Gateway with Security
│   └── src/main/java/com/gateway/
│       ├── ApiGatewayApplication.java
│       ├── config/
│       │   └── SecurityConfig.java
│       └── filter/
│           └── AuthenticationFilter.java
│
├── order-service/              # Saga Orchestrator
│   └── src/main/java/com/orders/
│       ├── OrderServiceApplication.java
│       ├── controller/
│       ├── service/            # Saga pattern implementation
│       ├── client/             # Feign clients
│       ├── model/
│       ├── repository/
│       └── config/             # RabbitMQ config
│
├── payment-service/            # Resilience4j Patterns
│   └── src/main/java/com/payment/
│       ├── PaymentServiceApplication.java
│       ├── controller/
│       ├── service/            # All resilience patterns
│       ├── model/
│       └── repository/
│
├── inventory-service/         # Saga Participant
│   └── src/main/java/com/inventory/
│       ├── InventoryServiceApplication.java
│       ├── controller/
│       ├── service/            # Reserve/Release operations
│       └── model/
│
├── notification-service/      # Async Consumer
│   └── src/main/java/com/arval/notification/
│       ├── NotificationServiceApplication.java
│       ├── listener/           # RabbitMQ consumers
│       ├── service/
│       └── config/             # RabbitMQ config
│
└── docker-compose.yml          # Container orchestration
```

## 🎓 Learning Path

1. **Start with Eureka Server** - Understand service discovery
2. **API Gateway** - Learn routing and security
3. **Order Service** - Study Saga pattern orchestration
4. **Payment Service** - Deep dive into Resilience4j patterns
5. **Inventory Service** - Understand Saga participants
6. **Notification Service** - Learn async communication

## 🔧 Configuration Files

Each service has detailed `application.yml` with:
- Service discovery configuration
- Resilience4j patterns configuration
- Database configuration
- RabbitMQ configuration
- Actuator endpoints

## 📊 Monitoring

- **Eureka Dashboard**: http://localhost:8761
- **RabbitMQ Management**: http://localhost:15672 (guest/guest)
- **Actuator Endpoints**: 
  - Health: `http://localhost:PORT/actuator/health`
  - Metrics: `http://localhost:PORT/actuator/metrics`
  - Circuit Breakers: `http://localhost:PORT/actuator/circuitbreakers`

## 🐛 Troubleshooting

### Services not registering with Eureka
- Check Eureka server is running
- Verify `eureka.client.service-url.defaultZone` in application.yml
- Check network connectivity

### RabbitMQ connection issues
- Ensure RabbitMQ is running: `docker ps | grep rabbitmq`
- Check connection settings in application.yml
- Verify ports 5672 and 15672 are accessible

### Circuit Breaker not working
- Check Resilience4j configuration in application.yml
- Verify `@CircuitBreaker` annotation is present
- Check AOP is enabled (Spring Boot Starter AOP)

## 📚 Additional Resources

- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [RabbitMQ Tutorial](https://www.rabbitmq.com/getstarted.html)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

## 👨‍💻 For Senior Developers

This project demonstrates:
- **Enterprise Patterns**: Saga, Circuit Breaker, Bulkhead, Retry
- **Architecture**: Microservices, Service Discovery, API Gateway
- **Communication**: Sync (REST/Feign) and Async (RabbitMQ)
- **Resilience**: Failure handling, retry, timeout, rate limiting
- **Security**: JWT authentication, API Gateway security
- **Best Practices**: Separation of concerns, dependency injection, configuration management

All code includes comprehensive comments explaining:
- What each annotation does
- Why patterns are used
- How components interact
- Best practices and trade-offs

## 📄 License

This project is for educational purposes.

---

**Happy Learning! 🚀**
