# WebSocket & Message Broker Configuration

## Overview

Ampairs supports two message broker modes for WebSocket/STOMP messaging:

1. **SimpleBroker** - In-memory broker (single instance, no external dependencies)
2. **RabbitMQ Relay** - External RabbitMQ STOMP relay (multi-instance, production-ready)

## Configuration Options

### Message Broker Types

Configure via `websocket.message-broker.type` in `application.yml` or `WEBSOCKET_BROKER_TYPE` environment variable:

| Type | Description | Use Case |
|------|-------------|----------|
| `SIMPLE` | In-memory broker | Development, single-instance deployments |
| `RABBITMQ` | External RabbitMQ | Production, multi-instance/horizontal scaling |
| `AUTO` | Auto-detect (default) | Tries RabbitMQ, falls back to SIMPLE if unavailable |

### Configuration Examples

#### 1. Development (In-Memory Broker)

**application.yml:**
```yaml
websocket:
  message-broker:
    type: SIMPLE
    heartbeat-interval: 0  # Disable STOMP heartbeats (default: 0)
```

**Environment Variables:**
```bash
export WEBSOCKET_BROKER_TYPE=SIMPLE
export WEBSOCKET_HEARTBEAT_INTERVAL=0  # Disabled (recommended)
```

**Benefits:**
- ✅ No external dependencies
- ✅ Zero configuration
- ✅ Fast startup
- ✅ Uses application-level heartbeats via /app/heartbeat
- ❌ Single instance only
- ❌ Messages lost on restart

#### 2. Production (RabbitMQ)

**application.yml:**
```yaml
websocket:
  message-broker:
    type: RABBITMQ
    rabbitmq:
      host: rabbitmq.example.com
      port: 61613  # STOMP port
      username: ampairs
      password: ${RABBITMQ_PASSWORD}
      virtual-host: /ampairs
```

**Environment Variables:**
```bash
export WEBSOCKET_BROKER_TYPE=RABBITMQ
export RABBITMQ_HOST=rabbitmq.example.com
export RABBITMQ_STOMP_PORT=61613
export RABBITMQ_USER=ampairs
export RABBITMQ_PASSWORD=secure_password
export RABBITMQ_VHOST=/ampairs
```

**Benefits:**
- ✅ Multi-instance support
- ✅ Horizontal scaling
- ✅ Message persistence
- ✅ Load balancing
- ❌ Requires RabbitMQ server
- ❌ Additional infrastructure

#### 3. Auto-Detect (Recommended)

**application.yml:**
```yaml
websocket:
  message-broker:
    type: AUTO  # Default
    rabbitmq:
      host: localhost
      port: 61613
      username: guest
      password: guest
```

**Behavior:**
1. On startup, attempts to connect to RabbitMQ
2. If connection succeeds → Uses RabbitMQ STOMP relay
3. If connection fails → Falls back to SimpleBroker
4. Logs the decision at startup

**Benefits:**
- ✅ Works in all environments
- ✅ No config changes needed between dev/prod
- ✅ Graceful degradation

## RabbitMQ Setup

### Install RabbitMQ with STOMP Plugin

**Docker (Recommended for Development):**
```bash
docker run -d --name rabbitmq \
  -p 5672:5672 \
  -p 61613:61613 \
  -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=ampairs \
  -e RABBITMQ_DEFAULT_PASS=ampairs123 \
  rabbitmq:3-management

# Enable STOMP plugin
docker exec rabbitmq rabbitmq-plugins enable rabbitmq_stomp
```

**Docker Compose:**
```yaml
services:
  rabbitmq:
    image: rabbitmq:3-management
    ports:
      - "5672:5672"    # AMQP
      - "61613:61613"  # STOMP
      - "15672:15672"  # Management UI
    environment:
      RABBITMQ_DEFAULT_USER: ampairs
      RABBITMQ_DEFAULT_PASS: ampairs123
    command: >
      bash -c "rabbitmq-server &
      sleep 10 &&
      rabbitmq-plugins enable rabbitmq_stomp &&
      wait"
```

**Kubernetes (Production):**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: rabbitmq
spec:
  selector:
    app: rabbitmq
  ports:
    - name: amqp
      port: 5672
    - name: stomp
      port: 61613
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: rabbitmq
spec:
  serviceName: rabbitmq
  replicas: 3
  selector:
    matchLabels:
      app: rabbitmq
  template:
    metadata:
      labels:
        app: rabbitmq
    spec:
      containers:
      - name: rabbitmq
        image: rabbitmq:3-management
        ports:
        - containerPort: 5672
        - containerPort: 61613
        env:
        - name: RABBITMQ_DEFAULT_USER
          valueFrom:
            secretKeyRef:
              name: rabbitmq-secret
              key: username
        - name: RABBITMQ_DEFAULT_PASS
          valueFrom:
            secretKeyRef:
              name: rabbitmq-secret
              key: password
```

## Troubleshooting

### Check Current Broker Type

Look for log messages at startup:

**Using RabbitMQ:**
```
INFO  [WebSocketConfig] Using RabbitMQ STOMP relay at localhost:61613 (configured)
```

**Using SimpleBroker:**
```
WARN  [WebSocketConfig] RabbitMQ STOMP relay at localhost:61613 unavailable. Falling back to SimpleBroker.
INFO  [WebSocketConfig] SimpleBroker STOMP heartbeat disabled (using application-level heartbeats)
```

### Common Issues

#### 1. WebSocket Heartbeat Configuration

**Two Heartbeat Mechanisms:**

1. **STOMP Protocol Heartbeats** (controlled by `heartbeat-interval`)
   - Built into STOMP protocol spec
   - Server and client negotiate heartbeat intervals during CONNECT
   - **SimpleBroker Limitation**: Poor/unreliable STOMP heartbeat support
   - **RabbitMQ**: Properly supports STOMP heartbeats

2. **Application-Level Heartbeats** (via `/app/heartbeat` endpoint)
   - Custom message-based heartbeats
   - Client sends to `/app/heartbeat`, handled by `HeartbeatController`
   - More flexible, allows custom heartbeat logic and session tracking
   - **Works reliably with both SimpleBroker and RabbitMQ**

**Current Configuration (SimpleBroker Strategy):**

**✅ RECOMMENDED: Disable STOMP heartbeats for SimpleBroker**

SimpleBroker has **poor STOMP heartbeat implementation** that causes connection drops (Code 1006).
Use application-level heartbeats instead:

**Server (application.yml):**
```yaml
websocket:
  message-broker:
    type: SIMPLE
    heartbeat-interval: 0  # DISABLE STOMP heartbeats for SimpleBroker
```

**Client (EventManager.kt):**
```kotlin
private val stompClient = StompClient(KtorWebSocketClient(httpClient)) {
    // Disable STOMP protocol heartbeats - SimpleBroker doesn't handle them well
    heartBeat = HeartBeat(0.seconds, 0.seconds)  // Disabled
    connectionTimeout = 30.seconds
}

// Application heartbeat every 15 seconds for session tracking
private fun startHeartbeat() {
    delay(15_000L)
    stompSession?.send(StompSendHeaders("/app/heartbeat"), FrameBody.Text(""))
}
```

**Backend Handler:**
```kotlin
@MessageMapping("/heartbeat")
fun handleHeartbeat(headerAccessor: SimpMessageHeaderAccessor) {
    deviceStatusService.updateHeartbeat(sessionId)  // Track online/offline status
}
```

**Heartbeat Strategy Comparison:**

| Broker | STOMP Heartbeats | Application Heartbeats | Recommended Config |
|--------|------------------|------------------------|-------------------|
| **SimpleBroker** | ❌ Unreliable (causes Code 1006) | ✅ Reliable | Disable STOMP, use app-level |
| **RabbitMQ** | ✅ Fully supported | ✅ Reliable | Enable both for redundancy |

**SimpleBroker Configuration:**
```yaml
websocket:
  message-broker:
    type: SIMPLE
    heartbeat-interval: 0  # ✅ Disable STOMP heartbeats
```

**RabbitMQ Configuration:**
```yaml
websocket:
  message-broker:
    type: RABBITMQ
    heartbeat-interval: 10000  # ✅ Enable STOMP heartbeats (10s)
```

**Why SimpleBroker STOMP Heartbeats Fail:**

1. SimpleBroker sends heartbeats to client, but doesn't properly handle heartbeats FROM client
2. Client sends STOMP heartbeat frames → SimpleBroker doesn't acknowledge them
3. Connection appears stale to one side → closes with Code 1006 (abnormal closure)
4. Results in: "Subscription collection failed - connection likely dropped"
5. Sessions disconnect before registration: "No device session found for disconnecting session"
6. Heartbeats arrive after disconnect: "Heartbeat received for unknown session"

**⚠️ WARNING: Do NOT Enable STOMP Heartbeats for SimpleBroker**

```yaml
# ❌ WRONG - causes Code 1006 disconnects with SimpleBroker
websocket:
  message-broker:
    type: SIMPLE
    heartbeat-interval: 10000  # SimpleBroker can't handle this properly
```

**What happens when STOMP heartbeats are enabled for SimpleBroker:**
1. Client connects successfully (HTTP 101, STOMP CONNECTED)
2. Client and server exchange STOMP heartbeat frames
3. SimpleBroker mishandles heartbeat frames from client
4. Connection drops with Code 1006 (abnormal closure without close frame)
5. Session never completes registration ("No device session found")
6. Application heartbeat arrives after disconnect ("Heartbeat received for unknown session")
7. Infinite reconnection loop begins

**✅ SOLUTION: Use application-level heartbeats only with SimpleBroker**

#### 2. RabbitMQ Connection Timeout

**Symptom:**
```
DEBUG [WebSocketConfig] STOMP broker probe failed for localhost:61613 -> Connection timed out
WARN  [WebSocketConfig] RabbitMQ STOMP relay at localhost:61613 unavailable...
```

**Solutions:**
- Verify RabbitMQ is running: `docker ps | grep rabbitmq`
- Check STOMP plugin enabled: `docker exec rabbitmq rabbitmq-plugins list | grep stomp`
- Verify port 61613 is accessible: `telnet localhost 61613`
- Check firewall rules
- Increase connection timeout: `RABBITMQ_CONNECTION_TIMEOUT=2000`

#### 2. Authentication Failed

**Symptom:**
```
ERROR [StompBrokerRelayMessageHandler] Failed to connect to STOMP broker: Access refused for user 'guest'
```

**Solutions:**
- Verify credentials match RabbitMQ configuration
- Check RabbitMQ user permissions:
  ```bash
  docker exec rabbitmq rabbitmqctl list_users
  docker exec rabbitmq rabbitmqctl set_permissions -p / ampairs ".*" ".*" ".*"
  ```

#### 3. STOMP Plugin Not Enabled

**Symptom:**
```
ERROR Failed to connect to STOMP broker: Connection refused
```

**Solution:**
```bash
docker exec rabbitmq rabbitmq-plugins enable rabbitmq_stomp
docker restart rabbitmq
```

## Migration Guide

### From Environment Variables Only → Spring Configuration

**Before (environment variables only):**
```bash
export RABBITMQ_HOST=localhost
export RABBITMQ_STOMP_PORT=61613
export WEBSOCKET_SIMPLE_BROKER=false
```

**After (Spring configuration):**
```yaml
websocket:
  message-broker:
    type: AUTO
    rabbitmq:
      host: ${RABBITMQ_HOST:localhost}
      port: ${RABBITMQ_STOMP_PORT:61613}
      username: ${RABBITMQ_USER:guest}
      password: ${RABBITMQ_PASSWORD:guest}
```

**Benefits:**
- ✅ Configuration visible in application.yml
- ✅ Type-safe with Spring Boot properties
- ✅ IDE autocomplete support
- ✅ Validation at startup
- ✅ Can still override with environment variables

## Performance Considerations

### SimpleBroker

- **Throughput**: ~10,000 messages/second
- **Memory**: Minimal (messages not persisted)
- **Latency**: Sub-millisecond
- **Scaling**: Single instance only

### RabbitMQ Relay

- **Throughput**: ~100,000 messages/second (clustered)
- **Memory**: Configurable (persistent messages)
- **Latency**: 1-5ms (network overhead)
- **Scaling**: Horizontal (multiple app instances + RabbitMQ cluster)

## Architecture Diagrams

### SimpleBroker (Single Instance)

```
┌──────────────────┐
│   Client App     │
│   (Browser/KMP)  │
└────────┬─────────┘
         │ WebSocket
         │ /ws
         v
┌──────────────────┐
│  Spring Boot     │
│  ┌────────────┐  │
│  │ In-Memory  │  │
│  │   Broker   │  │
│  └────────────┘  │
└──────────────────┘
```

### RabbitMQ Relay (Multi-Instance)

```
┌─────────────┐     ┌─────────────┐
│  Client 1   │     │  Client 2   │
└──────┬──────┘     └──────┬──────┘
       │ WebSocket         │ WebSocket
       v                   v
┌─────────────┐     ┌─────────────┐
│ Spring App1 │     │ Spring App2 │
└──────┬──────┘     └──────┬──────┘
       │ STOMP             │ STOMP
       │ (61613)           │ (61613)
       └───────┬───────────┘
               v
       ┌───────────────┐
       │   RabbitMQ    │
       │  STOMP Relay  │
       └───────────────┘
```

## Best Practices

1. **Use AUTO in all environments** - Allows seamless transition from dev to prod
2. **Never commit passwords** - Use environment variables for sensitive data
3. **Monitor RabbitMQ** - Use management UI (port 15672) to track queues/connections
4. **Enable SSL in production** - Use `amqps://` and secure STOMP connections
5. **Set resource limits** - Configure RabbitMQ memory/disk limits appropriately
6. **Test failover** - Verify SimpleBroker fallback works when RabbitMQ is down

## See Also

- [WebSocket Events Documentation](./README.md)
- [RabbitMQ STOMP Plugin](https://www.rabbitmq.com/stomp.html)
- [Spring WebSocket Reference](https://docs.spring.io/spring-framework/reference/web/websocket.html)
