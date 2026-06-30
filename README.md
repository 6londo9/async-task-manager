# Async Task Manager

Async Task Manager is a small Spring Boot service for submitting tasks and processing them asynchronously.

The main flow is:

```text
REST request -> Kafka task topic -> task worker -> PostgreSQL task result
             -> notification row -> Debezium CDC -> Kafka Streams -> notification inbox
```

The project demonstrates REST APIs, Kafka, PostgreSQL, optimistic/pessimistic locking, scheduled workers, retry handling, Debezium CDC, Kafka Streams, and a transactional inbox-style notification flow.

## Tech Stack

- Java 21
- Spring Boot
- PostgreSQL
- Kafka
- Kafka Streams
- Debezium Connect
- Liquibase
- Docker Compose
- Maven

## Running The Project

Prerequisites:

- Docker / Docker Compose
- Java 21
- Maven
- `make`

### The Makefile wraps the common commands:

#### Builds the application jar with tests skipped:

```bash
make package
```

#### Builds the jar and starts the full local stack from source using the `dev` Docker Compose profile:

```bash
make run-dev
```

#### Starts the full stack using the `prod` Docker Compose profile. This profile uses the `6londo9/async-task-manager:latest` image from `docker-compose.yml`:

```bash
make run-prod
```

#### Stops the compose stack:

```bash
make stop
```

#### Stops the stack and removes Docker volumes. Use this when you want a clean PostgreSQL/Kafka state:

```bash
make cleanup
```

#### Other useful commands:

```bash
make restart-dev
make restart-prod
make restart-app-dev
make restart-app-prod
```

## Local URLs

After `make run-dev`, these services are available:

- Application: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Kafka UI: `http://localhost:8090`
- pgAdmin: `http://localhost:5050`
- Debezium Connect: `http://localhost:8083`
- PostgreSQL: `localhost:5432`
- Kafka external listener: `localhost:29092`

Default database credentials:

```text
database: tasksdb
user: user
password: password
```

Default pgAdmin credentials:

```text
email: pgadmin4@pgadmin.org
password: admin
```

## Authentication

The API uses a simple user header:

```text
X-User-Id: 1
```

Use `X-User-Id: 0` as the admin user. Admin can read all tasks and call actuator endpoints.

## API Examples

### Create A Task

```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -H "X-User-Id: 1" \
  -d '{
    "name": "demo-task",
    "duration": 1000
  }'
```

Expected response:

```json
{
  "message": "Задача принята в обработку"
}
```

Task processing is asynchronous, so the task may not be visible or completed immediately.

`duration` is optional. If it is omitted, the application generates a random duration.

### List Current User Tasks

```bash
curl -s http://localhost:8080/api/tasks \
  -H "X-User-Id: 1" | jq
```

Example response:

```json
[
  {
    "id": 1,
    "name": "demo-task",
    "status": "COMPLETED",
    "result": "Task completed successfully",
    "userId": 1
  }
]
```

### Get One Task

```bash
curl -s http://localhost:8080/api/tasks/1 \
  -H "X-User-Id: 1" | jq
```

### List All Tasks As Admin

```bash
curl -s http://localhost:8080/api/tasks \
  -H "X-User-Id: 0" | jq
```

### Check Application Health

```bash
curl -s http://localhost:8080/actuator/health \
  -H "X-User-Id: 0" | jq
```

## How To Check The Async Flow

### 1. Check REST Result

Submit a task, wait a second or two, then list tasks:

```bash
curl -s http://localhost:8080/api/tasks \
  -H "X-User-Id: 1" | jq
```

The task should move from `NEW` to `IN_PROGRESS` to `COMPLETED`.

### 2. Check PostgreSQL

Open psql inside the PostgreSQL container:

```bash
docker exec -it postgres psql -U user -d tasksdb
```

Useful queries:

```sql
select id, name, status, result, user_id, retry_count
from tasks
order by id desc;

select id, user_id, message
from notifications
order by id desc;

select notification_id, is_processed, started_at
from notifications_inbox
order by notification_id desc;
```

After a task completes, you should see:

- one task row in `tasks`
- one notification row in `notifications`
- one inbox row in `notifications_inbox`
- `notifications_inbox.is_processed = true` after the inbox worker sends/processes it

### 3. Check Kafka UI

Open:

```text
http://localhost:8090
```

Useful topics:

- `tasks` - task creation messages
- `cdc.public.notifications` - Debezium events from the `notifications` table
- `notifications` - mapped notification messages produced by Kafka Streams

### 4. Check Debezium Connector

The application registers the Debezium connector on startup.

```bash
curl -s http://localhost:8083/connectors | jq
```

Expected connector name:

```json
[
  "postgres-cdc-connector"
]
```

You can inspect it with:

```bash
curl -s http://localhost:8083/connectors/postgres-cdc-connector/status | jq
```

## Notes

- Schedulers are enabled by default and can be disabled with `app.scheduler.enabled=false`.
- `POST /api/tasks` only publishes a Kafka message; persistence happens asynchronously when the Kafka listener consumes it.
- User `0` is treated as admin.
- Duplicate task names for the same user are rejected during task saving.
- The README can later include a terminal GIF or video here.
