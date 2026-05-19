# System Architecture

## Overview
The "Real-Time FX Rate Platform" follows a reactive, event-driven architecture to ensure low-latency delivery of exchange rate updates.

## Flow
1. **Rate Producer**: An external or internal service publishes FX rate updates to a **RabbitMQ** queue (e.g., `rate.input.queue`).
2. **RabbitMQ**: Routes messages to queues consumed by the **Spring Boot Rate Hub**.
3. **Spring Boot Rate Hub (Ingestion & Validation)**:
    - Consumes messages from RabbitMQ using `@RabbitListener`.
    - Performs business validation (e.g., positive rates, valid spread).
    - Invalid messages are logged as `[RATE_REJECTED]` and discarded (not requeued) to prevent infinite retry loops.
    - Malformed JSON messages that fail conversion are also not requeued due to `defaultRequeueRejected=false` configuration.
    - Valid messages are processed and then cached if they are newer than the current cached value.
4. **Processing & Caching**:
    - Converts `RateMessage` to `Rate` and calculates spread and alarm status.
    - Updates the **Hazelcast IMap** named `"rates"` with the latest rate for each symbol.
    - Compares incoming and current cached timestamps to ensure strict timestamp ordering.
    - Uses pair-level locking with `lock(pair)` / `unlock(pair)` to avoid race conditions during updates.
5. **WebSocket Broadcasting**:
    - If a cache update succeeds (it is not stale or rejected), the backend instance broadcasts the update to all its locally connected **WebSocket** clients subscribed to that pair.
6. **Broadcasting (Future)**:
    - Publishes the update to a **Hazelcast Topic** to support multi-instance environments.
7. **Inter-instance Signaling (Future)**: All running instances of the Rate Hub will subscribe to the Hazelcast Topic.
8. **WebSocket Broadcast Sync (Future)**: Upon receiving a message from the Hazelcast Topic, other instances will broadcast the update to their locally connected WebSocket clients.
9. **Frontend (Future)**: Receives real-time updates via WebSockets and displays them to the user.

## REST Snapshot API
- **Purpose**: Before establishing a WebSocket subscription, clients query `GET /api/rates` to fetch the current snapshot of all exchange rates. This ensures the UI is instantly populated.
- **Implementation**: The REST endpoints query the Hazelcast `IMap` named `"rates"` directly.
- **Sorting**: Rates returned from `GET /api/rates` are sorted alphabetically by currency pair.

## Why Hazelcast Topic?
In a multi-instance deployment, a WebSocket client is connected to only one specific instance. When an update arrives from RabbitMQ, it might be consumed by *any* instance. By using Hazelcast Topic, the consuming instance can notify *all* other instances of the update, ensuring that every connected client receives the data regardless of which instance they are connected to.

---
*Note: This is the intended final architecture. Ingesting, validation, processing, Hazelcast state caching, the REST Snapshot API, and local WebSocket live streaming are fully implemented. Hazelcast Topic multi-instance synchronization is not yet implemented.*
