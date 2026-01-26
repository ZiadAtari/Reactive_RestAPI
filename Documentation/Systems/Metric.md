# Metrics System

The Metrics system provides real-time observability into the application's performance, health, and business operations using **Micrometer** backed by a **Prometheus** registry.

## Components

### [AppLauncher](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/main/AppLauncher.java)
- **Purpose**: Initializes the Vert.x instance with metrics options.
- **Config**:
    - Enables Prometheus reporting.
    - Sets labels: `HTTP_METHOD`, `HTTP_CODE`, `HTTP_ROUTE`.
    - This is the entry point for the application.

### [HttpVerticle](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/HttpVerticle.java)
- **Endpoint**: `/metrics`
- **Handler**: `PrometheusScrapingHandler`.
- **Access**: Public (exposed on the main HTTP port).

## Metric Dimensions

### 1. HTTP Traffic
Automatically tracked by Vert.x Web.
*   **Metric**: `vertx_http_server_requests_seconds`
*   **Type**: Summary (Count + Sum)
*   **Labels**: `method` (GET/POST/etc), `code` (200/404/etc), `route` (e.g., `/v3/employees`).

### 2. Business Metrics
Custom metrics instrumented in the code.
*   **Login Attempts**:
    *   **Metric**: `api_auth_attempts_total` (Counter)
    *   **Label**: `result` ("success" or "failure")
    *   **Source**: `AuthController.java`

### 3. Resilience Metrics
*   **Circuit Breaker State**:
    *   **Metric**: `circuit_breaker_state` (Gauge)
    *   **Values**: `0` = CLOSED (Healthy), `1` = OPEN (Failing), `2` = HALF_OPEN (Probing).
    *   **Source**: `CustomCircuitBreaker.java`

## Scrape Configuration
Prometheus should be configured to scrape the target at `http://<host>:8888/metrics` every 15-60 seconds.
