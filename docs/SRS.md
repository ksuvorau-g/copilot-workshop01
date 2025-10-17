# Software Requirements Specification (SRS)
## Currency Exchange Rates Provider Service

**Version:** 1.0  
**Date:** October 17, 2025  
**Project:** aidemo1  
**Technology Stack:** Spring Boot 3.5.6, Java 25, PostgreSQL 17

---

## 1. Introduction

### 1.1 Purpose
This document specifies the functional and non-functional requirements for the Currency Exchange Rates Provider Service, a Spring Boot application designed to aggregate, store, and deliver real-time currency exchange rates from multiple providers.

### 1.2 Scope
The system will:
- Fetch exchange rates from multiple external providers (Fixer.io, ExchangeRatesAPI.io)
- Implement mock exchange rate providers for testing
- Store all exchange rates with historical tracking
- Provide REST API endpoints for currency management and rate queries
- Support role-based access control (USER, PREMIUM_USER, ADMIN)
- Update exchange rates on a scheduled basis (hourly)
- Calculate exchange rate trends over specified time periods

### 1.3 Definitions, Acronyms, and Abbreviations
- **API**: Application Programming Interface
- **REST**: Representational State Transfer
- **SRS**: Software Requirements Specification
- **RBAC**: Role-Based Access Control
- **ORM**: Object-Relational Mapping
- **JPA**: Java Persistence API

---

## 2. Overall Description

### 2.1 Product Perspective
The Currency Exchange Rates Provider Service is a standalone microservice that aggregates exchange rate data from multiple sources and provides a unified API for clients. The system uses a best-rate selection algorithm to return the most favorable exchange rate from all available providers.

### 2.2 Product Functions
1. **Currency Management**: Add and list supported currencies
2. **Exchange Rate Retrieval**: Fetch current exchange rates with amount conversion
3. **Rate Aggregation**: Collect rates from multiple providers and select the best rate
4. **Historical Tracking**: Store all rates for trend analysis
5. **Trend Analysis**: Calculate percentage changes over time periods
6. **Scheduled Updates**: Automatic hourly rate refresh
7. **User Authentication & Authorization**: Role-based access control

### 2.3 User Classes and Characteristics

| Role | Description | Permissions |
|------|-------------|-------------|
| **USER** | Standard authenticated user | View currencies, query exchange rates |
| **PREMIUM_USER** | Premium subscriber | All USER permissions + view trends |
| **ADMIN** | System administrator | All permissions including currency management and manual refresh |

### 2.4 Operating Environment
- **Runtime**: Docker containers orchestrated via Docker Compose
- **Database**: PostgreSQL 17
- **Application Server**: Embedded Spring Boot (Tomcat)
- **Network**: Internal Docker network for mock providers
- **External Dependencies**: Internet access for Fixer.io and ExchangeRatesAPI.io

### 2.5 Design and Implementation Constraints
- Java 25 required
- Spring Boot 3.5.6 framework
- Maven build system
- PostgreSQL database only (no NoSQL alternatives)
- Liquibase for schema management
- Docker containerization mandatory
- Rate precision: 6 decimal places
- Cache freshness: 1 hour

---

## 3. System Features

### 3.1 Currency Management

#### 3.1.1 List Currencies
**Priority:** High  
**Description:** Retrieve all currencies registered in the system.

**Functional Requirements:**
- **FR-1.1**: System SHALL provide endpoint `GET /api/v1/currencies`
- **FR-1.2**: Endpoint SHALL be accessible to all authenticated and unauthenticated users
- **FR-1.3**: Response SHALL include currency code and name
- **FR-1.4**: Response SHALL be in JSON format

**Database Reference:** See `001-create-currency-table.yaml`
```yaml
Table: currency
Columns:
  - id: BIGINT (PK, auto-increment)
  - code: VARCHAR(3) (unique, not null)
  - name: VARCHAR(100) (not null)
  - created_at: TIMESTAMP (not null)
  - updated_at: TIMESTAMP (not null)
```

#### 3.1.2 Add Currency
**Priority:** High  
**Description:** Register a new currency for exchange rate tracking.

**Functional Requirements:**
- **FR-2.1**: System SHALL provide endpoint `POST /api/v1/currencies?currency={code}`
- **FR-2.2**: Endpoint SHALL be restricted to ADMIN role only
- **FR-2.3**: Currency code SHALL be exactly 3 characters (validated via `@Size`)
- **FR-2.4**: System SHALL return HTTP 409 if currency already exists
- **FR-2.5**: System SHALL return HTTP 201 on successful creation

### 3.2 Exchange Rate Retrieval

#### 3.2.1 Get Exchange Rate
**Priority:** Critical  
**Description:** Calculate and return the best exchange rate for a currency pair with optional amount conversion.

**Functional Requirements:**
- **FR-3.1**: System SHALL provide endpoint `GET /api/v1/currencies/exchange-rates?from={from}&to={to}&amount={amount}`
- **FR-3.2**: Endpoint SHALL be accessible to all users (public)
- **FR-3.3**: Parameters `from` and `to` are REQUIRED
- **FR-3.4**: Parameter `amount` is OPTIONAL (default: 1.0)
- **FR-3.5**: System SHALL check database for rates within 1 hour freshness window
- **FR-3.6**: If no fresh rates exist, system SHALL fetch from all providers
- **FR-3.7**: System SHALL select best rate using lowest value as primary criterion
- **FR-3.8**: System SHALL use provider priority as tie-breaker (real providers: 100, mocks: 50)
- **FR-3.9**: System SHALL store ALL fetched rates before returning result
- **FR-3.10**: Returned amount SHALL be calculated as `amount * rate` with 6 decimal precision

**Database Reference:** See `002-create-exchange-rate-table.yaml`
```yaml
Table: exchange_rate
Columns:
  - id: BIGINT (PK, auto-increment)
  - base_currency: VARCHAR(3) (not null)
  - target_currency: VARCHAR(3) (not null)
  - rate: DECIMAL(19, 6) (not null)
  - provider: VARCHAR(50) (not null)
  - timestamp: TIMESTAMP (not null)
  - created_at: TIMESTAMP (not null)
  - updated_at: TIMESTAMP (not null)
```

#### 3.2.2 Refresh Exchange Rates
**Priority:** High  
**Description:** Force immediate retrieval of latest rates from all providers.

**Functional Requirements:**
- **FR-4.1**: System SHALL provide endpoint `POST /api/v1/currencies/refresh`
- **FR-4.2**: Endpoint SHALL be restricted to ADMIN role only
- **FR-4.3**: System SHALL fetch rates for all registered currencies
- **FR-4.4**: System SHALL query all configured providers in parallel
- **FR-4.5**: System SHALL store all successful responses

### 3.3 Trend Analysis

#### 3.3.1 Get Exchange Rate Trends
**Priority:** Medium  
**Description:** Calculate percentage change in exchange rate over a specified time period.

**Functional Requirements:**
- **FR-5.1**: System SHALL provide endpoint `GET /api/v1/currencies/trends?from={from}&to={to}&period={period}`
- **FR-5.2**: Endpoint SHALL be restricted to PREMIUM_USER and ADMIN roles
- **FR-5.3**: Period format SHALL support: `{n}H` (hours), `{n}D` (days), `{n}M` (months), `{n}Y` (years)
- **FR-5.4**: Minimum period SHALL be 12H (12 hours)
- **FR-5.5**: System SHALL compare current rate with rate from period ago
- **FR-5.6**: System SHALL calculate percentage change as: `((current - past) / past) * 100`
- **FR-5.7**: Response SHALL include: base currency, target currency, period, percentage change, trend direction

### 3.4 Provider Integration

#### 3.4.1 External Provider Integration
**Priority:** Critical  
**Description:** Integrate with at least 2 external exchange rate APIs.

**Functional Requirements:**
- **FR-6.1**: System SHALL implement integration with Fixer.io
- **FR-6.2**: System SHALL implement integration with ExchangeRatesAPI.io
- **FR-6.3**: Each provider SHALL implement `ExchangeRateProvider` interface
- **FR-6.4**: Each provider SHALL support retry mechanism with 3 attempts
- **FR-6.5**: Retry backoff SHALL use exponential strategy (1s, 2s, 4s)
- **FR-6.6**: Provider SHALL be auto-discovered via Spring component scanning
- **FR-6.7**: API keys SHALL be loaded from `secrets/` directory or environment variables

**Provider Interface:**
```java
public interface ExchangeRateProvider {
    ExchangeRate fetchRate(String from, String to);
    boolean supports(String from, String to);
    String getProviderName();
    int getPriority();
}
```

#### 3.4.2 Mock Provider Services
**Priority:** High  
**Description:** Create standalone mock services simulating exchange rate providers.

**Functional Requirements:**
- **FR-7.1**: System SHALL include at least 2 mock provider services
- **FR-7.2**: Mock providers SHALL run in separate Docker containers
- **FR-7.3**: Mock providers SHALL expose endpoints: `/mock/provider1` and `/mock/provider2`
- **FR-7.4**: Mock providers SHALL return randomized or hardcoded exchange rates
- **FR-7.5**: Mock providers SHALL respond in same format as real providers
- **FR-7.6**: Mock provider priority SHALL be set to 50 (lower than real providers)

### 3.5 Scheduled Operations

#### 3.5.1 Hourly Rate Updates
**Priority:** High  
**Description:** Automatically refresh exchange rates on a scheduled basis.

**Functional Requirements:**
- **FR-8.1**: System SHALL schedule rate refresh every 3600000ms (1 hour)
- **FR-8.2**: Initial delay SHALL be 10000ms (10 seconds) after startup
- **FR-8.3**: Scheduler SHALL fetch rates for all registered currencies
- **FR-8.4**: Scheduler SHALL use `@Scheduled` Spring annotation with fixed rate
- **FR-8.5**: Failed updates SHALL be logged but not prevent subsequent runs

### 3.6 Security & Authentication

#### 3.6.1 User Management
**Priority:** Critical  
**Description:** Implement user authentication and role-based authorization.

**Functional Requirements:**
- **FR-9.1**: System SHALL support form-based login
- **FR-9.2**: System SHALL support HTTP Basic authentication
- **FR-9.3**: Passwords SHALL be stored using BCrypt encryption
- **FR-9.4**: Users SHALL be stored in database (see `003-create-user-table.yaml`)
- **FR-9.5**: System SHALL support many-to-many user-role relationship

**Database Reference:** See `003-create-user-table.yaml`, `004-create-role-table.yaml`, `005-create-user-roles-table.yaml`
```yaml
Table: users
Columns:
  - id: BIGINT (PK, auto-increment)
  - username: VARCHAR(50) (unique, not null)
  - password: VARCHAR(255) (not null, BCrypt hashed)
  - email: VARCHAR(100) (unique, not null)
  - enabled: BOOLEAN (not null, default true)
  - created_at: TIMESTAMP (not null)
  - updated_at: TIMESTAMP (not null)

Table: roles
Columns:
  - id: BIGINT (PK, auto-increment)
  - name: VARCHAR(50) (unique, not null)

Table: user_roles
Columns:
  - user_id: BIGINT (PK, FK to users.id, CASCADE delete)
  - role_id: BIGINT (PK, FK to roles.id, CASCADE delete)
```

#### 3.6.2 Authorization Rules
**Functional Requirements:**
- **FR-10.1**: Public endpoints: GET `/api/v1/currencies`, GET `/api/v1/currencies/exchange-rates`
- **FR-10.2**: PREMIUM_USER + ADMIN: GET `/api/v1/currencies/trends`
- **FR-10.3**: ADMIN only: POST `/api/v1/currencies`, POST `/api/v1/currencies/refresh`
- **FR-10.4**: Public endpoints: All `/mock/**` endpoints, Swagger UI, actuator health
- **FR-10.5**: Unauthenticated requests to protected endpoints SHALL return HTTP 401
- **FR-10.6**: Insufficient permissions SHALL return HTTP 403

---

## 4. External Interface Requirements

### 4.1 API Specifications

| Endpoint | Method | Access | Parameters | Response |
|----------|--------|--------|------------|----------|
| `/api/v1/currencies` | GET | Public | None | `200 OK` - List of currencies |
| `/api/v1/currencies` | POST | ADMIN | `currency` (query) | `201 Created` - Currency created |
| `/api/v1/currencies/exchange-rates` | GET | Public | `from`, `to`, `amount?` | `200 OK` - Exchange rate result |
| `/api/v1/currencies/refresh` | POST | ADMIN | None | `200 OK` - Refresh confirmation |
| `/api/v1/currencies/trends` | GET | PREMIUM+ADMIN | `from`, `to`, `period` | `200 OK` - Trend data |

### 4.2 Error Response Format
All error responses SHALL follow this JSON structure:
```json
{
  "timestamp": "2025-10-17T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Currency code must be exactly 3 characters",
  "path": "/api/v1/currencies"
}
```

Implemented via `@RestControllerAdvice` with appropriate HTTP status codes:
- 400: Validation errors
- 401: Authentication required
- 403: Insufficient permissions
- 404: Resource not found
- 409: Conflict (duplicate currency)
- 500: Internal server error
- 503: External provider unavailable

### 4.3 API Documentation
- **FR-11.1**: System SHALL provide Swagger/OpenAPI documentation
- **FR-11.2**: Swagger UI SHALL be accessible at `/swagger-ui.html`
- **FR-11.3**: OpenAPI spec SHALL be available at `/v3/api-docs`

---

## 5. Non-Functional Requirements

### 5.1 Performance Requirements
- **NFR-1.1**: Rate retrieval SHALL complete within 5 seconds (including all provider calls)
- **NFR-1.2**: Database queries SHALL use appropriate indexes (see `006-add-indexes.yaml`)
- **NFR-1.3**: Concurrent requests SHALL be supported (minimum 10 simultaneous users)
- **NFR-1.4**: Cache freshness window of 1 hour SHALL minimize unnecessary API calls

### 5.2 Reliability Requirements
- **NFR-2.1**: System SHALL implement retry logic for external provider failures
- **NFR-2.2**: Retry attempts SHALL be limited to 3 per provider
- **NFR-2.3**: Provider failure SHALL NOT prevent other providers from being queried
- **NFR-2.4**: System SHALL continue operating if one provider is unavailable
- **NFR-2.5**: Database transactions SHALL be atomic (all-or-nothing)

### 5.3 Data Integrity Requirements
- **NFR-3.1**: Exchange rates SHALL be stored with 6 decimal places precision
- **NFR-3.2**: All rates SHALL include provider name and timestamp
- **NFR-3.3**: Historical data SHALL NOT be deleted (append-only model)
- **NFR-3.4**: Database schema changes SHALL be managed via Liquibase migrations
- **NFR-3.5**: Existing Liquibase changesets SHALL NEVER be modified

### 5.4 Security Requirements
- **NFR-4.1**: Passwords SHALL be encrypted using BCrypt with minimum strength 10
- **NFR-4.2**: API keys SHALL NOT be committed to version control
- **NFR-4.3**: API keys SHALL be stored in `secrets/` directory (gitignored)
- **NFR-4.4**: HTTPS SHALL be supported for production deployments
- **NFR-4.5**: SQL injection SHALL be prevented via JPA parameterized queries
- **NFR-4.6**: CSRF protection SHALL be enabled for non-API endpoints

### 5.5 Maintainability Requirements
- **NFR-5.1**: Code coverage SHALL be measured using JaCoCo
- **NFR-5.2**: Static analysis SHALL be performed using CheckStyle and PMD
- **NFR-5.3**: Code SHALL follow Lombok patterns (`@Data`, `@Builder`, `@Slf4j`)
- **NFR-5.4**: Services SHALL use `Optional` and Stream API where applicable
- **NFR-5.5**: New providers SHALL be addable without modifying existing code

### 5.6 Testability Requirements
- **NFR-6.1**: Unit test coverage SHALL exceed 80%
- **NFR-6.2**: All controllers SHALL have `@WebMvcTest` tests
- **NFR-6.3**: External API calls SHALL be tested with WireMock
- **NFR-6.4**: Integration tests SHALL use TestContainers for PostgreSQL
- **NFR-6.5**: Testing framework SHALL be JUnit 5
- **NFR-6.6**: Test framework SHALL include Spring Test Framework

---

## 6. Database Schema

### 6.1 Schema Management
All database schema changes are managed through Liquibase YAML changesets located in:
```
src/main/resources/db/changelog/
├── db.changelog-master.yaml
└── changes/
    ├── 001-create-currency-table.yaml
    ├── 002-create-exchange-rate-table.yaml
    ├── 003-create-user-table.yaml
    ├── 004-create-role-table.yaml
    ├── 005-create-user-roles-table.yaml
    ├── 006-add-indexes.yaml
    ├── 007-insert-default-roles.yaml
    ├── 008-insert-test-users.yaml
    └── 009-fix-test-user-passwords.yaml
```

### 6.2 Entity Relationships

```
┌─────────────┐       ┌──────────────┐       ┌─────────┐
│   users     │ ──┬── │  user_roles  │ ──┬── │  roles  │
└─────────────┘   │   └──────────────┘   │   └─────────┘
                  │                      │
                  │   Composite PK:      │
                  │   (user_id, role_id) │
                  │                      │
                  └──────────────────────┘
                     Many-to-Many

┌─────────────┐
│  currency   │
└─────────────┘
      │
      │ Referenced by (not FK)
      │
      ▼
┌─────────────────┐
│ exchange_rate   │
│                 │
│ base_currency   │ ──┐
│ target_currency │   │ VARCHAR(3) references
└─────────────────┘   └─ currency.code (logical)
```

### 6.3 Indexes
Performance optimization indexes (defined in `006-add-indexes.yaml`):
- Index on `exchange_rate(base_currency, target_currency, timestamp)` for rate queries
- Index on `exchange_rate(provider, timestamp)` for provider-specific queries
- Unique index on `currency(code)` for fast lookups
- Unique index on `users(username)` and `users(email)` for authentication

---

## 7. System Architecture

### 7.1 Architectural Patterns
- **Layered Architecture**: Controller → Service → Repository
- **Provider Pattern**: Pluggable exchange rate providers
- **Dependency Injection**: Spring IoC container
- **Repository Pattern**: Spring Data JPA repositories
- **DTO Pattern**: Separation of API models from entities

### 7.2 Key Components

```
┌─────────────────────────────────────────────────────┐
│                  REST Controllers                    │
│  CurrencyController | ExchangeRateController        │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────┐
│                  Service Layer                       │
│  CurrencyService | ExchangeRateService              │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────┴──────────────────────────────┐
│               RateAggregatorService                  │
│  (Coordinates all ExchangeRateProvider instances)   │
└──────────────────────┬──────────────────────────────┘
                       │
        ┌──────────────┼──────────────┬───────────────┐
        │              │               │               │
┌───────▼──────┐ ┌────▼─────┐ ┌──────▼───┐ ┌────────▼───┐
│FixerProvider │ │ExchRates │ │MockProv1 │ │MockProv2   │
│              │ │ApiProvider│ │          │ │            │
└──────────────┘ └───────────┘ └──────────┘ └────────────┘
        │              │               │               │
        └──────────────┴───────────────┴───────────────┘
                       │
┌──────────────────────┴──────────────────────────────┐
│              Repository Layer (JPA)                  │
│  CurrencyRepo | ExchangeRateRepo | UserRepo         │
└──────────────────────┬──────────────────────────────┘
                       │
                ┌──────▼─────┐
                │ PostgreSQL │
                └────────────┘
```

### 7.3 Deployment Architecture

```
┌─────────────────────────────────────────────────────┐
│              Docker Compose Network                  │
│                                                      │
│  ┌──────────────┐  ┌────────────┐  ┌─────────────┐ │
│  │ aidemo1-app  │  │ PostgreSQL │  │  Adminer    │ │
│  │  (Spring)    │◄─┤  Database  │◄─┤  (DB GUI)   │ │
│  │ Port: 8080   │  │ Port: 5432 │  │ Port: 8081  │ │
│  └──────┬───────┘  └────────────┘  └─────────────┘ │
│         │                                           │
│    ┌────┴────┐                                      │
│    │         │                                      │
│  ┌─▼────┐  ┌▼─────┐                                │
│  │Mock1 │  │Mock2 │                                │
│  │:8081 │  │:8082 │                                │
│  └──────┘  └──────┘                                │
│                                                      │
│  External APIs (outside Docker):                    │
│  - https://api.fixer.io                             │
│  - https://api.exchangeratesapi.io                  │
└─────────────────────────────────────────────────────┘
```

---

## 8. Technology Stack

### 8.1 Core Technologies
| Component | Technology | Version | Purpose |
|-----------|------------|---------|---------|
| Language | Java | 25 | Application runtime |
| Framework | Spring Boot | 3.5.6 | Application framework |
| Build Tool | Maven | 3.x | Dependency management |
| Database | PostgreSQL | 17 | Data persistence |
| Migration | Liquibase | Latest | Schema versioning |
| ORM | Spring Data JPA | (via Spring Boot) | Database access |
| Security | Spring Security | (via Spring Boot) | Authentication/Authorization |
| Validation | Hibernate Validator | (via Spring Boot) | Input validation |
| Retry | Spring Retry | (via Spring Boot) | Fault tolerance |

### 8.2 Development & Testing
| Component | Technology | Purpose |
|-----------|------------|---------|
| Testing Framework | JUnit 5 | Unit testing |
| Mocking | Mockito | Test doubles |
| Integration Tests | Spring Test | Context-aware tests |
| API Mocking | WireMock | External API simulation |
| Container Testing | TestContainers | PostgreSQL test instances |
| Code Coverage | JaCoCo | Coverage analysis |
| Static Analysis | CheckStyle, PMD | Code quality |
| Mutation Testing | PiTest (Optional) | Test quality |

### 8.3 Libraries
| Component | Technology | Purpose |
|-----------|------------|---------|
| Lombok | Project Lombok | Boilerplate reduction |
| HTTP Client | RestTemplate | External API calls |
| API Docs | springdoc-openapi | Swagger generation |
| Containerization | Docker, Docker Compose | Deployment |
| Database UI | Adminer | Database management |

---

## 9. Development Standards

### 9.1 Code Quality Requirements
- **DQR-1**: All public methods SHALL have JavaDoc comments
- **DQR-2**: Variable names SHALL be descriptive and follow camelCase
- **DQR-3**: Classes SHALL follow Single Responsibility Principle
- **DQR-4**: Magic numbers SHALL be replaced with named constants
- **DQR-5**: Exception messages SHALL be descriptive and actionable

### 9.2 Lombok Usage
Required Lombok annotations:
- `@Data` for entities and DTOs (generates getters, setters, toString, equals, hashCode)
- `@Builder` for complex object construction
- `@Slf4j` for logging
- `@RequiredArgsConstructor` for dependency injection

### 9.3 Stream API & Optional
- **DQR-6**: Collections processing SHALL use Stream API where applicable
- **DQR-7**: Null-returning methods SHALL use `Optional<T>` return type
- **DQR-8**: Avoid `.get()` on Optional; prefer `.orElse()`, `.orElseThrow()`, or `.ifPresent()`

### 9.4 Exception Handling
- **DQR-9**: Business exceptions SHALL extend `RuntimeException`
- **DQR-10**: Exception hierarchy: `ExternalProviderException`, `CurrencyNotFoundException`, etc.
- **DQR-11**: All exceptions SHALL be handled in `@RestControllerAdvice` class
- **DQR-12**: Exception responses SHALL include timestamp, status, error, message, path

---

## 10. Testing Strategy

### 10.1 Test Coverage Requirements
| Layer | Tool | Minimum Coverage | Key Annotations |
|-------|------|------------------|-----------------|
| Unit Tests | JUnit 5 + Mockito | 80% | `@ExtendWith(MockitoExtension.class)` |
| Controller Tests | MockMvc | 100% | `@WebMvcTest(ControllerClass.class)` |
| Service Tests | Mockito | 90% | `@Mock`, `@InjectMocks` |
| Integration Tests | Spring Test | N/A | `@SpringBootTest` |
| Provider Tests | WireMock | 100% | `@WireMockTest` |

### 10.2 Test Scenarios

#### Currency Management
- Add valid currency (success case)
- Add duplicate currency (409 conflict)
- Add invalid currency code (400 validation error)
- List currencies (empty and non-empty)
- Unauthorized access to admin endpoints (403)

#### Exchange Rate Retrieval
- Get rate with fresh cache (database lookup)
- Get rate with stale cache (provider fetch)
- Get rate with amount conversion
- Multiple providers return different rates (best selection)
- Provider failure with retry (success on retry)
- All providers fail (503 error)
- Invalid currency codes (400 error)

#### Trend Analysis
- Valid trend calculation (12H, 1D, 1M, 1Y periods)
- Insufficient historical data (404 error)
- Invalid period format (400 error)
- Access by non-premium user (403 error)

#### Security
- Login with valid credentials
- Login with invalid credentials (401)
- Access protected endpoint without authentication (401)
- Access admin endpoint as regular user (403)
- Password encryption verification

### 10.3 Test Naming Convention
```
methodName_StateUnderTest_ExpectedBehavior()

Examples:
- fetchRate_WhenProviderReturnsData_ReturnsExchangeRate()
- addCurrency_WhenCurrencyExists_ThrowsConflictException()
- getTrends_WhenUserIsNotPremium_ReturnsForbidden()
```

---

## 11. Deployment

### 11.1 Docker Compose Services
```yaml
services:
  postgres:
    image: postgres:17
    ports: 5432:5432
    environment:
      POSTGRES_DB: exchangerates
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
  
  adminer:
    image: adminer
    ports: 8081:8080
  
  aidemo1:
    build: .
    ports: 8080:8080
    depends_on:
      - postgres
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/exchangerates
  
  mock-provider-1:
    # Mock service implementation
  
  mock-provider-2:
    # Mock service implementation
```

### 11.2 Environment Configuration
Configuration properties in `application.properties`:
```properties
# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/exchangerates
spring.datasource.username=postgres
spring.datasource.password=postgres

# Liquibase
spring.liquibase.enabled=true
spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml

# JPA
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false

# Provider Configuration
exchange.provider.fixer.api-key=${FIXER_API_KEY}
exchange.provider.fixer.base-url=https://api.fixer.io
exchange.provider.exchangeratesapi.api-key=${EXCHANGERATESAPI_KEY}
exchange.provider.exchangeratesapi.base-url=https://api.exchangeratesapi.io

# Scheduler
exchange.scheduler.fixed-rate=3600000
exchange.scheduler.initial-delay=10000
```

### 11.3 Build & Run Commands
```bash
# Build application
./mvnw clean package

# Run tests
./mvnw test

# Start infrastructure
docker-compose up -d postgres adminer

# Run application locally
./mvnw spring-boot:run

# Run full stack in Docker
docker-compose up --build
```

---

## 12. API Examples

### 12.1 Example: Add Currency
```http
POST /api/v1/currencies?currency=USD HTTP/1.1
Host: localhost:8080
Authorization: Basic YWRtaW46cGFzc3dvcmQ=

Response: 201 Created
{
  "id": 1,
  "code": "USD",
  "name": "US Dollar",
  "createdAt": "2025-10-17T12:00:00Z"
}
```

### 12.2 Example: Get Exchange Rate
```http
GET /api/v1/currencies/exchange-rates?from=USD&to=EUR&amount=100 HTTP/1.1
Host: localhost:8080

Response: 200 OK
{
  "baseCurrency": "USD",
  "targetCurrency": "EUR",
  "rate": 0.850000,
  "amount": 100.0,
  "convertedAmount": 85.00,
  "provider": "Fixer",
  "timestamp": "2025-10-17T12:00:00Z"
}
```

### 12.3 Example: Get Trends
```http
GET /api/v1/currencies/trends?from=USD&to=EUR&period=24H HTTP/1.1
Host: localhost:8080
Authorization: Basic cHJlbWl1bTpwYXNzd29yZA==

Response: 200 OK
{
  "baseCurrency": "USD",
  "targetCurrency": "EUR",
  "period": "24H",
  "percentageChange": -1.25,
  "trend": "DECREASING",
  "currentRate": 0.850000,
  "previousRate": 0.860714
}
```

---

## 13. Glossary

| Term | Definition |
|------|------------|
| **Base Currency** | The currency being converted FROM (e.g., USD in USD→EUR) |
| **Target Currency** | The currency being converted TO (e.g., EUR in USD→EUR) |
| **Exchange Rate** | Conversion factor between two currencies |
| **Provider** | External service or mock service supplying exchange rates |
| **Best Rate** | Rate with lowest value (best deal for customer) |
| **Rate Freshness** | Time window (1 hour) during which cached rates are considered valid |
| **Provider Priority** | Numeric value determining tie-breaker in rate selection (real=100, mock=50) |
| **Changeset** | Liquibase migration unit defining database schema changes |
| **Trend** | Percentage change in exchange rate over time period |

---

## 14. Appendix A: Database Initialization Data

### 14.1 Default Roles (007-insert-default-roles.yaml)
- USER
- PREMIUM_USER
- ADMIN

### 14.2 Test Users (008-insert-test-users.yaml, 009-fix-test-user-passwords.yaml)
- **user**: Password-encrypted, role: USER
- **premium**: Password-encrypted, role: PREMIUM_USER
- **admin**: Password-encrypted, role: ADMIN

All passwords are BCrypt-hashed and loaded from `secrets/logins.txt`.

---

## 15. Appendix B: Configuration Files Reference

### 15.1 Liquibase Changelog Structure
```
db.changelog-master.yaml (includes all changes sequentially)
├─ 001-create-currency-table.yaml
├─ 002-create-exchange-rate-table.yaml
├─ 003-create-user-table.yaml
├─ 004-create-role-table.yaml
├─ 005-create-user-roles-table.yaml
├─ 006-add-indexes.yaml
├─ 007-insert-default-roles.yaml
├─ 008-insert-test-users.yaml
└─ 009-fix-test-user-passwords.yaml
```

### 15.2 Secrets Files (gitignored)
```
secrets/
├── fixer_api_key.txt          # Fixer.io API key
├── exchangeratesapi_key.txt   # ExchangeRatesAPI.io API key
└── logins.txt                 # Test user credentials
```

---

## 16. Revision History

| Version | Date | Author | Description |
|---------|------|--------|-------------|
| 1.0 | 2025-10-17 | System | Initial SRS based on requirements.txt and Liquibase schema |

---

**End of Document**
