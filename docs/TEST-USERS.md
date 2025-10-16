# Test Users and Credentials

This document lists the test users created for development and testing purposes.

## Test Users

The following users are automatically created when the application starts (via Liquibase migrations):

### 1. Regular User
- **Username**: `user`
- **Password**: `user`
- **Email**: `user@example.com`
- **Role**: `ROLE_USER`
- **Permissions**:
  - GET /api/v1/currencies
  - GET /api/v1/currencies/exchange-rates

### 2. Administrator
- **Username**: `admin`
- **Password**: `admin`
- **Email**: `admin@example.com`
- **Role**: `ROLE_ADMIN`
- **Permissions**:
  - All USER permissions
  - POST /api/v1/currencies (add new currency)
  - POST /api/v1/currencies/refresh (refresh exchange rates)

### 3. Premium User
- **Username**: `prem`
- **Password**: `prem`
- **Email**: `prem@example.com`
- **Role**: `ROLE_PREMIUM_USER`
- **Permissions**:
  - All USER permissions
  - GET /api/v1/currencies/trends (when implemented)

## Using Test Credentials

### Browser Login (Form-Based)

1. Navigate to any protected endpoint (e.g., `http://localhost:8080/api/v1/currencies`)
2. You'll be redirected to the login page
3. Enter one of the test credentials above
4. After successful login, you'll be redirected to the requested page

### API Access (HTTP Basic Authentication)

Use curl or any HTTP client with Basic Authentication:

```bash
# Public endpoint (no authentication needed)
curl http://localhost:8080/api/v1/currencies

# Protected endpoint with USER credentials
curl -u user:user http://localhost:8080/api/v1/currencies/exchange-rates?from=USD&to=EUR&amount=100

# Admin-only endpoint with ADMIN credentials
curl -u admin:admin \
  -X POST \
  -H "Content-Type: application/json" \
  -d '{"code":"USD","name":"US Dollar"}' \
  http://localhost:8080/api/v1/currencies

# Refresh rates (ADMIN only)
curl -u admin:admin -X POST http://localhost:8080/api/v1/currencies/refresh
```

### Testing with Postman

1. **Authorization Tab**: Select "Basic Auth"
2. **Username**: Enter one of the test usernames
3. **Password**: Enter the corresponding password
4. Send your request

## Security Notes

⚠️ **Important**: These are test credentials for development purposes only.

- **DO NOT** use these credentials in production
- Passwords are BCrypt-hashed in the database
- All test users are enabled by default
- These users are created by Liquibase migration `008-insert-test-users.yaml`

## Database Location

Test users are stored in:
- **Table**: `users`
- **Role assignments**: `user_roles` (junction table)
- **Roles**: `roles` table

## Modifying Test Users

To modify or add more test users, edit or create a new Liquibase changeset in:
```
src/main/resources/db/changelog/changes/
```

Remember to:
1. Use BCrypt to hash passwords
2. Assign appropriate roles
3. Update this documentation
