# v4.1 Update Report: Metrics & Monitoring

## 1. Overview
This update introduces a comprehensive **Metrics Collection System** using **Micrometer** and **Prometheus**. The goal was to provide observability into critical operational flows, specifically User Login, System Liveness, and Circuit Breaker state, without relying solely on logs.

## 2. Key Changes

### Infrastructure
*   **Custom Launcher (`AppLauncher.java`)**:
    *   Replaced the default `io.vertx.core.Launcher` with a custom implementation.
    *   **Reason**: Vert.x Metrics options must be configured *before* the Vert.x instance is created.
    *   **Configuration**: Enabled Prometheus with `HTTP_METHOD`, `HTTP_CODE`, and `HTTP_ROUTE` labels.
*   **Build Configuration**:
    *   Updated `pom.xml` to use `ziadatari.ReactiveAPI.main.AppLauncher` as the default launcher.
    *   Updated `.vscode/launch.json` to use the new launcher for debugging.

### Instrumentation
*   **Web Layer (`HttpVerticle`)**:
    *   Added a scrape endpoint at `/metrics`.
*   **Authentication (`AuthController`)**:
    *   Instrumented the login flow to track attempts.
    *   **Metric**: `api_auth_attempts_total` (Counter) with `result` tag (`success` or `failure`).
*   **Resilience (`CustomCircuitBreaker`)**:
    *   Added observability to the circuit breaker state.
    *   **Metric**: `circuit_breaker_state` (Gauge) where 0=CLOSED, 1=OPEN, 2=HALF_OPEN.

### Bug Fixes
*   **RSA Key Corruption**:
    *   Identified and fixed corrupted default RSA keys in `MainVerticle.java` that were causing "extra data at the end" errors during startup.

## 3. Metrics Reference

| Metric Name | Type | Tags | Description |
| :--- | :--- | :--- | :--- |
| `vertx_http_server_requests_seconds` | Summary | `method`, `code`, `route` | Standard HTTP throughput and latency. |
| `api_auth_attempts_total` | Counter | `result` | Custom business metric for login attempts. |
| `circuit_breaker_state` | Gauge | *none* | Custom gauge for circuit breaker health (0=OK). |

## 4. Verification
The update was verified using manual tests and a temporary integration test (`TestMetrics.java`).
*   **Endpoint Check**: `GET /metrics` returns Prometheus-formatted text.
*   **Flow Check**: Login attempts correctly increment the counter.
