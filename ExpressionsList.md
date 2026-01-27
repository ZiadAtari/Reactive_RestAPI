# v4.2 Metrics Catalog & Usage Guide

This document organizes the available Prometheus metrics and provides instructions on how to query, filter, and aggregate them.

## 0. Query Fundamentals (Must Read)

### A. Units & Scaling
*   **Time**: All `_seconds` metrics are in **Seconds**.
    *   To view in **Milliseconds**, multiply by 1000: `metric * 1000`.
*   **Percentages**: Ratios are `0.0-1.0`.
    *   To view as **Percentage**, multiply by 100: `metric * 100`.

### B. Filtering (Labels)
Metrics are multidimensional. Narrow down data by appending `{label="value"}`.
*   **By Method**: `vertx_http_server_response_time_seconds_count{method="POST"}`
*   **By Route**: `vertx_http_server_response_time_seconds_count{route="/login"}`
*   **By Code**: `vertx_http_server_response_time_seconds_count{code="500"}`
*   **Regex**: `vertx_http_server_response_time_seconds_count{code=~"5.."}` (Any 5xx error)

### C. Aggregation (Summing)
When running multiple instances (or just to remove labels), use `sum` and `rate`.
*   **Global Total**: `sum(rate(metric[1m]))` -> One line.
*   **Grouped**: `sum(rate(metric[1m])) by (route)` -> One line per route.

---

## 1. High Priority (Golden Signals)
**Focus**: Operational Health & Business Criticality.

| Metric | Type | Labels | Description |
| :--- | :--- | :--- | :--- |
| **`up`** | Gauge | `job`, `instance` | `1` if reachable, `0` if down. |
| **`api_auth_attempts_total`** | Counter | `result` | Login attempts. Filter by `result="failure"` to detect attacks. |
| **`circuit_breaker_state`** | Gauge | `name` | `0`=Closed, `1`=Open, `2`=Half-Open. <br> Names: `auth-login`, `v1-verify`, `v3-verify` |

### Example Queries
*   **Current Failure Count**: `sum(api_auth_attempts_total{result="failure"})`
*   **Login Breaker Status**: `max(circuit_breaker_state{name="auth-login"})`
*   **V3 Verification Status**: `max(circuit_breaker_state{name="v3-verify"})`
*   **V1 Verification Status**: `max(circuit_breaker_state{name="v1-verify"})`

---

## 2. HTTP Server (Inbound Traffic)
**Focus**: User Experience (Latency & Errors).
**Labels**: `method`, `code`, `route`, `instance`

| Metric | Type | Description |
| :--- | :--- | :--- |
| **`vertx_http_server_response_time_seconds_bucket`** | Histogram | **CRITICAL**: Latency distribution buckets (`0.1`, `0.5`, `1.0`...). Use for P95/P99. |
| `vertx_http_server_response_time_seconds_count` | Histogram | Total count of observed requests. **Preferred over `requests_total`** as it shares labels with latency metrics. |
| `vertx_http_server_response_time_seconds_sum` | Histogram | Total time (in seconds) spent serving requests. |
| **`vertx_http_server_active_requests`** | Gauge | Current in-flight requests (Concurrency). |
| `vertx_http_server_active_connections` | Gauge | Current open TCP connections. |
| `vertx_http_server_bytes_written_total` | Counter | Total response size in bytes. |

### Example Queries
*   **Global RPS (Success Only, No Noise)**:
    `sum(rate(vertx_http_server_response_time_seconds_count{code="200", route!="/metrics"}[1m]))`
    
*   **Error Rate % (Business Only, 5xx)**: 
    ```promql
    (sum(rate(vertx_http_server_response_time_seconds_count{code=~"5..", route!="/metrics"}[1m])) 
    / 
    sum(rate(vertx_http_server_response_time_seconds_count{route!="/metrics"}[1m]))) * 100
    ```

*   **Average Latency (ms)**:
    ```promql
    (sum(rate(vertx_http_server_response_time_seconds_sum[1m])) 
    / 
    sum(rate(vertx_http_server_response_time_seconds_count[1m]))) * 1000
    ```

*   **P95 Latency (ms)**: 
    ```promql
    histogram_quantile(0.95, sum(rate(vertx_http_server_response_time_seconds_bucket[5m])) by (le)) * 1000
    ```

---

## 3. Database (Connection Pool)
**Focus**: Backend Bottlenecks.
**Labels**: `pool_name`, `pool_type`

| Metric | Type | Description |
| :--- | :--- | :--- |
| **`vertx_pool_ratio`** | Gauge | Pool usage (0.0 - 1.0). > 0.8 is critical. |
| **`vertx_pool_queue_pending`** | Gauge | Requests waiting for a connection. Must be 0. |
| `vertx_pool_in_use` | Gauge | Active connections. |
| `vertx_pool_usage_seconds_bucket` | Histogram | Time spent holding a connection. |
| `vertx_pool_completed_total` | Counter | Total connections returned to pool. |

### Example Queries
*   **Pool Saturation**: `max(vertx_pool_ratio)`
*   **Pending Queries**: `sum(vertx_pool_queue_pending)`

---

## 4. HTTP Client (Outbound Traffic)
**Focus**: Dependency Health (DemoAPI).
**Labels**: `method`, `code`, `host`

| Metric | Type | Description |
| :--- | :--- | :--- |
| **`vertx_http_client_response_time_seconds_bucket`** | Histogram | Latency of dependency calls. |
| `vertx_http_client_requests_total` | Counter | Total outgoing requests. |
| `vertx_http_client_active_requests` | Gauge | Pending external calls. |

### Example Queries
*   **Dependency Latency (P95 ms)**: 
    `histogram_quantile(0.95, sum(rate(vertx_http_client_response_time_seconds_bucket[5m])) by (le)) * 1000`

---

## 5. Event Bus (Internal Messaging)
**Focus**: Internal component communication.
**Labels**: `address`

| Metric | Type | Description |
| :--- | :--- | :--- |
| `vertx_eventbus_pending` | Gauge | Messages queued but not processed. High = Blocked Event Loop. |
| `vertx_eventbus_processed_total` | Counter | Message throughput. |
| `vertx_eventbus_discarded_total` | Counter | Messages dropped due to overflow. |

### Example Queries
*   **Event Loop Backlog**: `sum(vertx_eventbus_pending) by (address)`
