# Docker Setup Quick Start

## What's Included

This Docker Compose setup includes:
- **PostgreSQL** (latest) - Database server
- **Spring Boot Application** - Your aidemo1 app
- **Adminer** - Web-based database management tool

## Quick Start

```bash
# Start everything (builds app image on first run)
docker compose up --build

# Or run in background
docker compose up -d --build
```

## Access Points

Once running, you can access:

1. **Spring Boot Application**: http://localhost:8080
2. **Adminer (Database UI)**: http://localhost:8081
   - System: `PostgreSQL`
   - Server: `postgres`
   - Username: `postgres`
   - Password: `postgres`
   - Database: `aidemo`

## Useful Commands

```bash
# Stop all containers
docker compose down

# Stop and remove all data (fresh start)
docker compose down -v

# View logs
docker compose logs -f

# View logs for specific service
docker compose logs -f app
docker compose logs -f postgres

# Rebuild only the app (after code changes)
docker compose up --build app
```

## Database Connection

The Spring Boot app connects to PostgreSQL using:
- **URL**: `jdbc:postgresql://postgres:5432/aidemo`
- **Username**: `postgres`
- **Password**: `postgres`

These are configured via environment variables in `docker compose.yml` and can be overridden.

## Development Workflow

1. Make code changes
2. Run `docker compose up --build` to rebuild and restart
3. Access app at http://localhost:8080
4. Use Adminer at http://localhost:8081 to inspect database

## Troubleshooting

### Port conflicts
If ports 5432, 8080, or 8081 are already in use, modify the port mappings in `docker compose.yml`:
```yaml
ports:
  - "5433:5432"  # Change left side (host port)
```

### Database connection issues
Check that PostgreSQL is healthy:
```bash
docker compose ps
docker compose logs postgres
```

### App won't start
Check application logs:
```bash
docker compose logs app
```
