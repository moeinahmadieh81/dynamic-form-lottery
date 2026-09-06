# Dynamic Form Lottery

Backend MVP for a dynamic form builder and lottery system.

## Stack

- Java 21
- Spring Boot 4.1.1
- Spring MVC
- Spring Security + JWT
- Spring Data JPA / Hibernate
- PostgreSQL 17
- Flyway
- Maven

## Run

```bash
docker compose up -d
mvn clean test
mvn spring-boot:run
```

The application runs on `http://localhost:8080`.

On an existing database that is currently on V4, Flyway automatically applies:

```text
V5__add_read_api_indexes.sql
```

## Local development admin

```text
email: admin@local.dev
password: ChangeMe123!
```

This account is only for local development.

```bash
export BOOTSTRAP_ADMIN_ENABLED=false
export JWT_SECRET='replace-with-a-long-random-secret-at-least-32-bytes'
```

## Get tokens

```bash
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@local.dev","password":"ChangeMe123!"}' | jq -r '.accessToken')
```

For an existing user:

```bash
USER_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ali@example.com","password":"StrongPass123!"}' | jq -r '.accessToken')
```

## Form lifecycle

The current MVP lifecycle is:

```text
DRAFT -> PUBLISHED -> CLOSED -> DRAWN
```

- `DRAFT`: editable by ADMIN
- `PUBLISHED`: USER submissions are accepted
- `CLOSED`: no more submissions; lottery may be created
- `DRAWN`: lottery completed

### Close a published form

Only ADMIN can close a form:

```bash
curl -X POST http://localhost:8080/api/forms/1/close \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

After this call, new submissions for the form are rejected.

## Lottery model

Creating a lottery snapshots the currently eligible participants into `lottery_entries`.

Eligible means:

- submission belongs to the target form
- submission status is `SUBMITTED`
- submission has an authenticated `user_id`

Old development submissions with `user_id = NULL` are intentionally excluded.

The snapshot is important because later changes to submissions must not rewrite the historical participant set of an already-created lottery.

## Prepare more than one participant

If form 1 currently has only Ali as an authenticated participant, register another user **before closing the form**:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "sara@example.com",
    "displayName": "Sara Ahmadi",
    "password": "StrongPass123!"
  }'
```

```bash
SARA_TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"sara@example.com","password":"StrongPass123!"}' | jq -r '.accessToken')
```

Submit while the form is still `PUBLISHED`:

```bash
curl -X POST http://localhost:8080/api/forms/1/submissions \
  -H "Authorization: Bearer $SARA_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "answers": {
      "fullName": "Sara Ahmadi",
      "age": 31,
      "city": "tabriz"
    }
  }'
```

Then close the form.

## Create a lottery

Only a `CLOSED` form can have a lottery created.

```bash
curl -X POST http://localhost:8080/api/forms/1/lotteries \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "winnerCount": 1
  }'
```

Example response:

```json
{
  "id": 1,
  "formId": 1,
  "status": "READY",
  "winnerCount": 1,
  "participantCount": 2,
  "createdBy": 1,
  "createdAt": "...",
  "startedAt": null,
  "completedAt": null,
  "winners": []
}
```

At creation time, eligible submissions are copied into `lottery_entries`.

MVP rule: a form can have only one non-cancelled lottery.

## Run the lottery

```bash
curl -X POST http://localhost:8080/api/lotteries/1/run \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Example response:

```json
{
  "id": 1,
  "formId": 1,
  "status": "COMPLETED",
  "winnerCount": 1,
  "participantCount": 2,
  "createdBy": 1,
  "createdAt": "...",
  "startedAt": "...",
  "completedAt": "...",
  "winners": [
    {
      "position": 1,
      "userId": 2,
      "submissionId": 2,
      "selectedAt": "..."
    }
  ]
}
```

Winner selection uses `SecureRandom` plus a partial Fisher-Yates shuffle. Winners cannot repeat inside the same lottery.

Running the same lottery a second time returns `409 Conflict`.

The run operation is transactional and uses a pessimistic write lock on the lottery row, preventing two admins from producing two independent results concurrently.

When the draw completes, the form moves from `CLOSED` to `DRAWN`.

## Read lottery result

ADMIN only:

```bash
curl http://localhost:8080/api/lotteries/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

## Database checks

```bash
docker exec -it dynamic-form-lottery-postgres-1 psql \
  -U postgres \
  -d dynamic_form_lottery
```

Lottery:

```sql
select id, form_id, status, winner_count, created_by, created_at, started_at, completed_at
from lotteries;
```

Snapshot:

```sql
select id, lottery_id, submission_id, user_id, created_at
from lottery_entries
order by id;
```

Winners:

```sql
select id, lottery_id, lottery_entry_id, position, selected_at
from lottery_winners
order by position;
```

Form state:

```sql
select id, name, status, current_version
from forms
where id = 1;
```

Audit trail:

```sql
select id, actor_id, action, entity_type, entity_id, metadata, created_at
from audit_logs
order by id;
```

Expected audit actions in this phase include:

```text
FORM_CLOSED
LOTTERY_CREATED
LOTTERY_COMPLETED
```

## Concurrency and integrity safeguards

This phase intentionally has safeguards at both the application and database layers:

- form row lock when creating a lottery
- lottery row `PESSIMISTIC_WRITE` lock when running it
- `@Version` optimistic version column on lotteries as an additional consistency mechanism
- one active/completed lottery per form enforced by a partial unique index
- unique `(lottery_id, submission_id)` entries
- unique `(lottery_id, user_id)` entries
- unique winner position per lottery
- a lottery entry can win only once in a lottery
- the entire winner-selection operation runs in one transaction

## Migrations

```text
V1__create_form_tables.sql
V2__create_submissions_table.sql
V3__create_users_and_secure_submissions.sql
V4__create_lottery_and_audit_tables.sql
```

# Read API phase

This version adds the read side needed by the React frontend. No business state is changed by these endpoints.

On a database already at V4, Flyway applies:

```text
V5__add_read_api_indexes.sql
```

The migration adds composite indexes for the main paginated queries:

```text
forms(status, updated_at)
submissions(form_id, submitted_at)
submissions(user_id, submitted_at)
lotteries(form_id, status, created_at)
```

## Pagination contract

List endpoints return an explicit page DTO instead of exposing Spring Data's `Page` JSON format directly:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

`size` must be between 1 and 100. Sort fields are explicitly whitelisted per endpoint.

## List forms

Both USER and ADMIN may call:

```bash
curl -s 'http://localhost:8080/api/forms?page=0&size=20&sort=updatedAt&direction=desc' \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Optional status filter:

```bash
curl -s 'http://localhost:8080/api/forms?status=PUBLISHED&page=0&size=10' \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

Role visibility is different:

- ADMIN sees all form statuses, including `DRAFT` and `ARCHIVED`.
- USER sees only `PUBLISHED`, `CLOSED`, and `DRAWN` forms.
- A regular USER requesting a DRAFT form by id gets `404`, so draft existence is not exposed.

ADMIN example:

```bash
curl -s 'http://localhost:8080/api/forms?page=0&size=20' \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

## Read a form

```bash
curl -s http://localhost:8080/api/forms/1 \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

The detail response includes the current dynamic schema. The list response intentionally uses a smaller summary DTO and does not include the schema for every form.

## Admin: list form submissions

Only ADMIN can inspect all submissions for a form:

```bash
curl -s 'http://localhost:8080/api/forms/1/submissions?page=0&size=20&sort=submittedAt&direction=desc' \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Example item:

```json
{
  "id": 2,
  "formId": 1,
  "formVersion": 2,
  "user": {
    "id": 2,
    "email": "ali@example.com",
    "displayName": "Ali Ahmadi"
  },
  "status": "SUBMITTED",
  "answers": {
    "fullName": "Ali Ahmadi",
    "age": 28,
    "city": "tehran"
  },
  "submittedAt": "..."
}
```

A development submission created before authentication was introduced has `user: null`.

## Current user: my submissions

```bash
curl -s 'http://localhost:8080/api/me/submissions?page=0&size=20' \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

The response includes useful form context for the user dashboard:

```json
{
  "id": 2,
  "formId": 1,
  "formName": "Summer Lottery",
  "formStatus": "DRAWN",
  "formVersion": 2,
  "status": "SUBMITTED",
  "answers": {},
  "submittedAt": "..."
}
```

The `userId` is never accepted as a query parameter. It is derived from the JWT subject.

## User-safe lottery result by form

Both USER and ADMIN can call:

```bash
curl -s http://localhost:8080/api/forms/1/lottery \
  -H "Authorization: Bearer $USER_TOKEN" | jq
```

For a regular USER, a lottery becomes visible only after it is `COMPLETED`.

Example:

```json
{
  "id": 1,
  "formId": 1,
  "status": "COMPLETED",
  "winnerCount": 1,
  "participantCount": 1,
  "completedAt": "...",
  "winners": [
    {
      "position": 1,
      "displayName": "Ali Ahmadi",
      "currentUser": true,
      "selectedAt": "..."
    }
  ]
}
```

This endpoint deliberately does not expose winner email, internal submission id, or database user id to regular users.

## Admin: full lottery data

Existing endpoint:

```bash
curl -s http://localhost:8080/api/lotteries/1 \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

Admin-only winner details:

```bash
curl -s http://localhost:8080/api/lotteries/1/winners \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq
```

The admin response includes user id, email, display name, submission id, position, and selection time.

## Read API authorization summary

| Endpoint | USER | ADMIN |
|---|---:|---:|
| `GET /api/forms` | yes, visible states only | yes, all states |
| `GET /api/forms/{id}` | yes, visible states only | yes |
| `GET /api/forms/{id}/submissions` | no | yes |
| `GET /api/me/submissions` | yes | yes |
| `GET /api/forms/{id}/lottery` | completed only | yes |
| `GET /api/lotteries/{id}` | no | yes |
| `GET /api/lotteries/{id}/winners` | no | yes |

## Migrations now

```text
V1__create_form_tables.sql
V2__create_submissions_table.sql
V3__create_users_and_secure_submissions.sql
V4__create_lottery_and_audit_tables.sql
V5__add_read_api_indexes.sql
```

---

## Integration tests and GitHub CI

The project now has two test layers:

- **Unit tests**: run during Maven's `test` phase.
- **Integration tests**: classes ending in `IT`, run by Maven Failsafe during `verify`.

Integration tests start a disposable **PostgreSQL 17** instance with Testcontainers. The application starts with the real Spring Boot context, Flyway runs all migrations from scratch, Hibernate validates the generated schema, and tests call the HTTP API through an embedded server.

### Prerequisites for integration tests

- Java 21
- Maven 3.9+
- Docker Desktop / Docker Engine running

You do **not** need to start the project's local `compose.yml` database before the tests. Testcontainers owns its own temporary PostgreSQL container.

### Run only unit tests

```bash
mvn clean test
```

### Run unit + integration tests

```bash
mvn clean verify
```

The current integration suite covers:

1. Flyway builds the expected PostgreSQL schema from an empty database.
2. Authentication and role-based authorization (`401` / `403`).
3. Draft forms are hidden from regular users.
4. Dynamic submission validation and duplicate-submission protection.
5. Full business flow: create → publish → submit → close → create lottery → run → verify winner.
6. Lottery participant snapshot, persisted winner, `DRAWN` form transition, and audit records.
7. Two concurrent lottery-run requests result in exactly one success and one conflict.

### Test profile

Integration-only configuration is in:

```text
src/test/resources/application-it.yml
```

It contains test-only JWT/admin credentials. These values are isolated to the test runtime and must not be reused for production.

### GitHub Actions

The workflow is:

```text
.github/workflows/backend-ci.yml
```

For every push or pull request targeting `main`, GitHub Actions:

```text
checkout
   ↓
Java 21
   ↓
Maven dependency cache
   ↓
Docker availability check
   ↓
mvn clean verify
   ↓
unit tests + PostgreSQL/Testcontainers integration tests
   ↓
upload Surefire/Failsafe reports
```

The integration tests use Testcontainers directly, so the GitHub workflow intentionally does not define a separate PostgreSQL service container.

### Suggested GitHub branch protection

After the first successful workflow run, protect `main` in GitHub and require the **Unit + Integration Tests** status check before merging pull requests.

More detail: [`docs/integration-testing.md`](docs/integration-testing.md)
