# Real-Time FX Rate Platform

## Project Purpose

The Real-Time FX Rate Platform is a backend-focused case project designed to process real-time foreign exchange rate updates. 

The intended final system will ingest FX rate messages through RabbitMQ, validate and process them in a Spring Boot backend, store the latest rate state in Hazelcast, and stream live updates to frontend clients through WebSocket connections.

## Current Status

The backend currently supports RabbitMQ ingestion, Hazelcast caching, REST snapshot APIs, and local-instance WebSocket streaming. 

Implemented so far:

- Spring Boot backend project structure
- Maven configuration (com.fxrate:fx-rate-hub)
- Java 17 setup
- Health check endpoint (`/api/health`)
- Docker Compose infrastructure for RabbitMQ and Hazelcast
- **RabbitMQ Queue and Consumer**: Ingestion layer for `rate.input.queue` is implemented.
- **Message Validation**: Incoming rates are validated for business rules (positive values, valid spread, etc.).
- **Rate Processing & Calculations**: For valid messages, spread and alarm calculations are performed (alarm triggers if spread > `app.rate.spread-alarm-threshold`).
- **Hazelcast Caching**: The latest valid rate per currency pair is cached in a Hazelcast IMap named `"rates"`.
- **Timestamp Concurrency & Ordering**: Only updates with a newer timestamp than the currently cached rate are processed; stale or duplicate messages are ignored.
- **Concurrency Control**: Pair-level locking is used on the IMap to prevent race conditions during updates.
- **Logging**: Clean logging for validation failures, updates, and stale-message drops.
- **REST Snapshot API**:
  - `GET /api/rates` returns all cached latest rates sorted alphabetically by pair.
  - `GET /api/rates/{base}/{quote}` returns the latest rate for the resolved pair (normalized to uppercase).
  - Returns standard `404` errors with `ErrorResponse` if the pair is not found.
- **WebSocket Live Streaming**:
  - Exposes `ws://localhost:8080/ws/rates` endpoint for local WebSocket clients to subscribe and stream live rate updates.

Not implemented yet:

- Hazelcast Topic multi-instance broadcast
- Frontend
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

#### REST Snapshot API

* **Get All Rates**: `GET /api/rates`
* **Get Single Rate**: `GET /api/rates/{base}/{quote}` (e.g. `/api/rates/EUR/USD`)

#### WebSocket API

* **Endpoint**: `ws://localhost:8080/ws/rates`
* **Subscription Request**:
  ```json
  {
    "type": "SUBSCRIBE",
    "pairs": ["EUR/USD"]
  }
  ```

### Build Commands

To clean and package the backend application:

> [!IMPORTANT]
> Running the test suite (i.e. running the build without `-DskipTests`) requires the RabbitMQ and Hazelcast Docker services to be active (`docker compose up -d`). If the Docker services are not running, the integration tests will fail.

**Linux / Mac:**
```bash
cd backend
./mvnw clean package
```

**Windows:**
```powershell
cd backend
.\mvnw.cmd clean package
```

To skip the Docker-dependent integration tests during build:

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

### Manual Testing (RabbitMQ Ingestion, Caching, REST API, and WebSocket)

1. Start the infrastructure: `docker compose up -d`
2. Start the backend: `cd backend` then `.\mvnw.cmd spring-boot:run`
3. Connect a WebSocket client (e.g. using `wscat` or a browser extension) to:
   `ws://localhost:8080/ws/rates`
4. Send a subscription message:
   ```json
   {
     "type": "SUBSCRIBE",
     "pairs": ["EUR/USD"]
   }
   ```
   You should receive an acknowledgment:
   ```json
   {
     "type": "SUBSCRIBED",
     "pairs": ["EUR/USD"]
   }
   ```
5. Open RabbitMQ Management UI at [http://localhost:15672](http://localhost:15672).
6. Navigate to **Queues** -> `rate.input.queue`.
7. Under **Publish message**, paste a valid JSON:
   ```json
   {
     "provider": "LP1",
     "pair": "EUR/USD",
     "bid": 1.0845,
     "ask": 1.0847,
     "timestamp": 1710000000123
   }
   ```
8. The WebSocket client will receive:
   ```json
   {
     "type": "RATE_UPDATE",
     "data": {
       "provider": "LP1",
       "pair": "EUR/USD",
       "bid": 1.0845,
       "ask": 1.0847,
       "spread": 0.0002,
       "alarm": false,
       "timestamp": 1710000000123,
       "receivedAt": 1710000009999
     }
   }
   ```
   *(Note: receivedAt is generated by the backend at processing time, so the value changes between runs).*
9. Perform REST API requests to query snapshots:
   - To get all rates:
     `curl http://localhost:8080/api/rates`
   - To get a single pair:
     `curl http://localhost:8080/api/rates/eur/usd`
10. Publish an older message for the same pair:
    ```json
    {
      "provider": "LP1",
      "pair": "EUR/USD",
      "bid": 1.0840,
      "ask": 1.0842,
      "timestamp": 1710000000000
    }
    ```
    Verify it is ignored by looking at the logs:
    `[RATE_STALE_IGNORED] pair=EUR/USD incomingTimestamp=1710000000000 currentTimestamp=1710000000123`
    *(Note: Stale updates are NOT broadcasted to WebSocket).*
11. Publish an invalid message (e.g., `ask` lower than `bid`):
    ```json
    {
      "provider": "LP1",
      "pair": "EUR/USD",
      "bid": 1.0850,
      "ask": 1.0840,
      "timestamp": 1710000000123
    }
    ```
    Verify it is rejected by looking at the logs:
    `[RATE_REJECTED] reason=ASK_LESS_THAN_BID payload=...`
    *(Note: Rejected rates are NOT broadcasted to WebSocket).*

### Limitations
- **Local instance only**: The WebSocket broadcaster currently only runs on the local application instance processing the RabbitMQ message. Multi-instance WebSocket synchronization via a shared Hazelcast Topic will be implemented in a subsequent phase.

## Next Steps

- Implement inter-instance signaling via Hazelcast Topic.
- Add a simple rate producer/load simulator.
- Develop the Frontend UI to display real-time rate changes.
