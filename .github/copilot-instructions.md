# Copilot Instructions for aidemo1

## Project Overview
Spring Boot 3.5.6 + Java 25 exchange rate aggregation service. Fetches currency rates from multiple providers (Fixer.io, ExchangeRatesAPI.io, internal mocks), stores all rates in PostgreSQL, and returns the best rate (lowest value).

## Core Architecture

### Provider Pattern (Critical)
All exchange rate providers implement `ExchangeRateProvider` interface with 4 methods:
- `fetchRate(from, to)` - Returns `ExchangeRate` entity with rate, provider name, timestamp
- `supports(from, to)` - Fast check without API calls
- `getProviderName()` - Unique identifier stored in DB
- `getPriority()` - Real providers = 100, mocks = 50

**Key Design:** Spring auto-wires all `ExchangeRateProvider` beans into `RateAggregatorService` constructor, enabling dynamic provider registration without code changes.

### Best Rate Selection Logic
`RateAggregatorService.selectBestRate()`:
1. **Primary:** Lowest exchange rate value (best deal)
2. **Tie-breaker:** Highest provider priority (prefer real over mock)

All fetched rates are persisted for historical tracking before selecting the best.

### Data Flow
```
Controller → ExchangeRateService (DB lookup) → RateAggregatorService 
→ [Provider1, Provider2, ...] → Save ALL rates → Return BEST rate
```

Database lookup checks for rates within `RATE_FRESHNESS_HOURS = 1` hour.

## Package Structure
- `integration/provider/` - Provider implementations (Fixer, ExchangeRatesAPI, Mock1/2)
- `integration/client/` - HTTP clients wrapping RestTemplate calls
- `integration/aggregator/` - `RateAggregatorService` orchestrates providers
- `controller/api/v1/` - REST endpoints (`/api/v1/currencies`)
- `controller/mock/` - Mock provider endpoints (`/mock/provider1`, `/mock/provider2`)
- `scheduler/` - Hourly rate refresh via `@Scheduled`

## Development Patterns

### Adding New Provider
1. Create class implementing `ExchangeRateProvider`
2. Add `@Component` annotation (auto-discovered by Spring)
3. Annotate `fetchRate()` with `@Retryable` (3 attempts, exponential backoff)
4. Set priority via `getPriority()` return value
5. Add configuration to `application.properties`:
   ```properties
   exchange.provider.{name}.api-key=...
   exchange.provider.{name}.base-url=...
   exchange.provider.{name}.priority=100
   ```

### Retry Configuration
All providers use `@Retryable` on `fetchRate()`:
```java
@Retryable(
    retryFor = ExternalProviderException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
```
Enabled by `@EnableRetry` in `RetryConfig`.

### Test Conventions
- Unit tests: `@ExtendWith(MockitoExtension.class)` with `@Mock`, `@InjectMocks`
- Controller tests: `@WebMvcTest(ControllerClass.class)` for MockMvc
- Integration tests: `@SpringBootTest` (SecurityConfig uses this)
- Use `@MockitoSettings(strictness = Strictness.LENIENT)` for aggregator tests
- Prefer `assertThat()` from AssertJ over JUnit assertions

### Security Model
Configured in `SecurityConfig`:
- **Public:** GET endpoints (`/api/v1/currencies/**`), mock endpoints, Swagger, health
- **Admin-only:** POST `/api/v1/currencies` (add currency), POST `/api/v1/currencies/refresh`
- Authentication: Form login + HTTP Basic (database-backed via `CustomUserDetailsService`)
- Roles: USER, PREMIUM_USER, ADMIN

## Build & Run

### Local Development
```bash
# Start PostgreSQL + Adminer
docker-compose up -d postgres adminer

# Run application (uses embedded Spring Boot)
./mvnw spring-boot:run

# Run tests
./mvnw test

# Database available at localhost:5432 (postgres/postgres)
# Adminer UI at http://localhost:8081
```

### Database Migrations
Liquibase manages schema via `src/main/resources/db/changelog/`.
- Master: `db.changelog-master.yaml`
- Changes: `changelog/changes/*.yaml`
- Runs automatically on startup (`spring.liquibase.enabled=true`)

### Secrets Management
API keys stored in `secrets/` directory (gitignored):
- `fixer_api_key.txt`
- `exchangeratesapi_key.txt`
- `logins.txt`

Fallback to environment variables: `${FIXER_API_KEY:default_value}`

## Critical Constants
- `ExchangeRateServiceImpl.RATE_FRESHNESS_HOURS = 1` - DB cache duration
- `ExchangeRateServiceImpl.DECIMAL_SCALE = 6` - Rate precision
- Scheduler runs every hour: `@Scheduled(fixedRate = 3600000, initialDelay = 10000)`

## Common Gotchas
1. **Priority matters:** When adding providers, set priority correctly or they'll be deprioritized
2. **All rates saved:** `RateAggregatorService` saves ALL fetched rates before selecting best
3. **No @Transactional on providers:** Retry logic requires proxy, so no direct transaction
4. **Mock endpoints** are public and respond without authentication for testing
5. **Currency codes** must be 3 characters (enforced via `@Size` validation)

## API Endpoints
- `GET /api/v1/currencies` - List all currencies (public)
- `GET /api/v1/currencies/exchange-rates?from=USD&to=EUR&amount=100` - Get rate (public)
- `POST /api/v1/currencies` - Add currency (admin)
- `POST /api/v1/currencies/refresh` - Force rate refresh (admin)
- Swagger: http://localhost:8080/swagger-ui.html

## Dependencies of Note
- Lombok (`@Data`, `@Builder`, `@Slf4j`) - Keep getters/setters/builders implicit
- Spring Retry - Automatic retry with exponential backoff
- Liquibase - Database versioning (never modify existing changesets)
- PostgreSQL 17 - Primary database
- springdoc-openapi - Auto-generates Swagger UI from annotations
