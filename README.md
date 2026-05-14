# Real-Time FX Rate Platform

## Project Purpose

The Real-Time FX Rate Platform is a backend-focused case project designed to process real-time foreign exchange rate updates. 

The intended final system will ingest FX rate messages through RabbitMQ, validate and process them in a Spring Boot backend, store the latest rate state in Hazelcast, and stream live updates to frontend clients through WebSocket connections.

## Current Status

This repository is currently in the **RabbitMQ Ingestion Foundation** phase. 

Implemented so far:

- Spring Boot backend project structure
- Maven configuration (com.fxrate:fx-rate-hub)
- Java 17 setup
- Health check endpoint (`/api/health`)
- Docker Compose infrastructure for RabbitMQ and Hazelcast
- **RabbitMQ Queue and Consumer**: Ingestion layer for `rate.input.queue` is implemented.
- **Message Validation**: Incoming rates are validated for business rules (positive values, valid spread, etc.).
- **Logging**: Valid rates are logged as `[RATE_ACCEPTED]`, and invalid rates as `[RATE_REJECTED]`.
- **Error Handling**:
    - Business-invalid messages are validated inside the consumer and logged as rejected without throwing exceptions.
    - Malformed or unconvertible messages are not requeued forever because the RabbitMQ listener container is configured with `defaultRequeueRejected=false`.
    - A future production improvement could route such messages to a Dead Letter Queue.

Not implemented yet:

- Hazelcast IMap cache integration
- Hazelcast Topic based multi-instance event distribution
- WebSocket live broadcasting
- Frontend live rate screen
- Producer/load simulation

## Intended Architecture

```text
Rate Producer
    |
    v
RabbitMQ
    |
    v
Spring Boot Rate Hub - Multi Instance
    |
    v
Hazelcast IMap + Hazelcast Topic
    |
    v
WebSocket
    |
    v
Frontend Live Rate Screen
```

For more details, see [docs/architecture.md](docs/architecture.md).

## Project Structure

```text
fx-rate-platform/
├── backend/                # Spring Boot application
│   ├── src/main/java/      # Java source files (com.fxrate.platform)
│   ├── src/main/resources/ # Application configuration
│   ├── pom.xml             # Maven dependencies
│   ├── Dockerfile          # Backend containerization
│   └── mvnw / mvnw.cmd     # Maven wrapper
├── docs/                   # Architectural documentation
├── frontend/               # Frontend placeholder
├── docker-compose.yml      # Infrastructure (RabbitMQ, Hazelcast)
└── README.md               # Project overview
```

## Getting Started

### API Documentation

#### Health Check
`GET /api/health`

**Response:**
```json
{
  "status": "UP",
  "service": "fx-rate-hub"
}
```

### Build Commands

To clean and package the backend application:

**Linux / Mac:**
```bash
cd backend
./mvnw clean package -DskipTests
```

**Windows:**
```powershell
cd backend
.\mvnw.cmd clean package -DskipTests
```

### Local Run Commands

To run the backend application locally:

**Linux / Mac:**
```bash
cd backend
./mvnw spring-boot:run
```

**Windows:**
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

### Infrastructure (Docker)

To start RabbitMQ and Hazelcast services:

```bash
docker compose up -d
```

- **RabbitMQ Management UI**: [http://localhost:15672](http://localhost:15672) (guest / guest)
- **Hazelcast Instance**: `localhost:5701`

### Manual Testing (RabbitMQ Ingestion)

1. Start the infrastructure: `docker compose up -d`
2. Start the backend: `cd backend` then `.\mvnw.cmd spring-boot:run`
3. Open RabbitMQ Management UI at [http://localhost:15672](http://localhost:15672).
4. Navigate to **Queues** -> `rate.input.queue`.
5. Under **Publish message**, paste a valid JSON:
   ```json
   {
     "provider": "LP1",
     "pair": "EUR/USD",
     "bid": 1.0845,
     "ask": 1.0847,
     "timestamp": 1710000000123
   }
   ```
6. Check backend logs for: `[RATE_ACCEPTED] provider=LP1 pair=EUR/USD bid=1.0845 ask=1.0847 timestamp=1710000000123`
7. Publish an invalid message (e.g., `ask` lower than `bid`):
   ```json
   {
     "provider": "LP1",
     "pair": "EUR/USD",
     "bid": 1.0850,
     "ask": 1.0840,
     "timestamp": 1710000000123
   }
   ```
8. Check backend logs for: `[RATE_REJECTED] reason=ASK_LESS_THAN_BID payload=...`

## Next Steps

- Integrate Hazelcast for distributed caching of latest rates (IMap).
- Implement inter-instance signaling via Hazelcast Topic.
- Implement WebSocket broadcasting to handle live client updates.
- Develop the Frontend UI to display real-time rate changes.
