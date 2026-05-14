# Real-Time FX Rate Platform

## Project Purpose

The Real-Time FX Rate Platform is a backend-focused case project designed to process real-time foreign exchange rate updates. 

The intended final system will ingest FX rate messages through RabbitMQ, validate and process them in a Spring Boot backend, store the latest rate state in Hazelcast, and stream live updates to frontend clients through WebSocket connections.

## Current Status

This repository is currently in the **Initial Scaffolding** phase. 

Implemented so far:

- Spring Boot backend project structure
- Maven configuration (com.fxrate:fx-rate-hub)
- Java 17 setup
- Health check endpoint (`/api/health`)
- Docker Compose infrastructure for RabbitMQ and Hazelcast
- Initial architecture documentation in `docs/architecture.md`

Not implemented yet:

- RabbitMQ rate consumer
- Rate validation and business processing
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

## Next Steps

- Implement RabbitMQ Producer/Consumer logic for FX rates.
- Integrate Hazelcast for distributed caching of latest rates.
- Implement inter-instance signaling via Hazelcast Topic.
- Implement WebSocket broadcasting to handle live client updates.
- Develop the Frontend UI to display real-time rate changes.
