# Integration testing strategy

## Goal

The integration suite verifies the behavior of the deployed application boundary rather than isolated classes. Each integration test starts the real Spring Boot application with a random HTTP port and connects it to a disposable PostgreSQL 17 database managed by Testcontainers.

## What is real in these tests?

- Spring MVC controllers and HTTP serialization
- Spring Security and JWT authentication/authorization
- Service and transaction boundaries
- JPA/Hibernate mappings
- PostgreSQL behavior and constraints
- Flyway migrations
- pessimistic/optimistic locking used by the lottery flow

No local PostgreSQL instance is required.

## Maven lifecycle

| Command | Unit tests | Integration tests |
| --- | --- | --- |
| `mvn test` | yes | no |
| `mvn verify` | yes | yes |

Integration classes use the `*IT` suffix and are executed by Maven Failsafe.

## Test scenarios

### `DatabaseMigrationIT`
Starts from an empty PostgreSQL database and verifies all Flyway migrations and expected tables.

### `SecurityIT`
Verifies anonymous/admin/user authorization boundaries and that draft forms are not disclosed to normal users.

### `SubmissionRulesIT`
Verifies schema-driven validation through the HTTP API and verifies duplicate submission protection at the persistence boundary.

### `FullWorkflowIT`
Covers the main business journey end-to-end:

```text
admin login
  -> create form
  -> publish
  -> register two users
  -> submit twice
  -> close form
  -> create lottery snapshot
  -> run lottery
  -> verify one winner
  -> verify DRAWN form
  -> verify public result
  -> verify snapshot/winner/audit rows
```

### `ConcurrentLotteryIT`
Sends two lottery-run requests concurrently. Exactly one request must succeed and the other must receive `409 Conflict`; the database must contain only one persisted winner set.

## CI behavior

GitHub Actions runs `mvn clean verify` on an Ubuntu runner. Testcontainers uses the runner's Docker daemon to start PostgreSQL, so CI does not need a separately configured PostgreSQL service.

## Rules for future tests

1. Prefer API-level setup for business data instead of inserting rows directly.
2. Use direct SQL assertions only for properties that belong to the persistence contract, such as constraints, snapshots, migrations, and audit records.
3. Give each test unique email addresses and form names so tests do not depend on execution order.
4. Do not use the developer PostgreSQL database from integration tests.
5. Keep test execution sequential unless the suite is deliberately redesigned for parallel Testcontainers execution.
