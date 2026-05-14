# System Architecture

## Overview
The "Real-Time FX Rate Platform" follows a reactive, event-driven architecture to ensure low-latency delivery of exchange rate updates.

## Flow
1. **Rate Producer**: An external or internal service publishes FX rate updates to a **RabbitMQ** exchange (e.g., `rate.input.queue`).
2. **RabbitMQ**: Routes messages to queues consumed by the **Spring Boot Rate Hub**.
3. **Spring Boot Rate Hub (Ingestion & Validation)**:
    - Consumes messages from RabbitMQ using `@RabbitListener`.
    - Performs business validation (e.g., positive rates, valid spread).
    - Invalid messages are logged as `[RATE_REJECTED]` and discarded to prevent infinite retry loops.
    - Valid messages are logged as `[RATE_ACCEPTED]` and proceed to the next phase.
4. **Processing (Future)**:
    - Updates the **Hazelcast IMap** with the latest rate for each symbol (ensuring timestamp ordering).
    - Publishes the update to a **Hazelcast Topic**.
5. **Inter-instance Signaling**: All running instances of the Rate Hub subscribe to the Hazelcast Topic.
6. **WebSocket Broadcasting**: Upon receiving a message from the Hazelcast Topic, each instance broadcasts the update to all its locally connected **WebSocket** clients.
7. **Frontend**: Receives real-time updates via WebSockets and displays them to the user.

## Why Hazelcast Topic?
In a multi-instance deployment, a WebSocket client is connected to only one specific instance. When an update arrives from RabbitMQ, it might be consumed by *any* instance. By using Hazelcast Topic, the consuming instance can notify *all* other instances of the update, ensuring that every connected client receives the data regardless of which instance they are connected to.

---
*Note: This is the intended final architecture. The current implementation is scaffolding only.*
