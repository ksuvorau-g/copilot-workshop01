# aidemo1

A Spring Boot web application with PostgreSQL database support.

## Prerequisites

- JDK 21 or higher
- Maven 3.9+ (or use included Maven wrapper)
- Docker and Docker Compose (for containerized deployment)

## Running with Docker Compose

The easiest way to run the application with all dependencies:

```bash
# Build and start all containers (PostgreSQL, Spring Boot app, Adminer)
docker compose up --build

# Or run in detached mode
docker compose up -d --build

# Stop all containers
docker compose down

# Stop and remove volumes (clears database data)
docker compose down -v
```

Once running, access:
- **Application**: http://localhost:8080
- **Adminer** (DB management): http://localhost:8081
  - System: PostgreSQL
  - Server: postgres
  - Username: postgres
  - Password: postgres
  - Database: aidemo

## Running Locally (without Docker)

1. Ensure PostgreSQL is running locally on port 5432 with database `aidemo`
2. Build the application:
   ```bash
   ./mvnw clean package
   ```
3. Run the application:
   ```bash
   java -jar target/aidemo1-0.0.1-SNAPSHOT.jar
   ```

## Development

### Using VS Code
Use the "Launch Aidemo1Application (Spring Boot)" configuration from `.vscode/launch.json`.

### Running Tests
```bash
./mvnw test
```

### Database Configuration
The application uses environment variables for database configuration:
- `SPRING_DATASOURCE_URL` (default: `jdbc:postgresql://localhost:5432/aidemo`)
- `SPRING_DATASOURCE_USERNAME` (default: `postgres`)
- `SPRING_DATASOURCE_PASSWORD` (default: `postgres`)

## Technology Stack

- Spring Boot 3.5.6
- PostgreSQL (latest)
- Java 25
- Maven
- Lombok
- Docker & Docker Compose
