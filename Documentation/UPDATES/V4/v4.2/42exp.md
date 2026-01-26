# v4.2 Metrics Catalog

This document organizes the available Prometheus metrics by domain and importance.

## 1. High Priority (Golden Signals)
These metrics are critical for dashboards and alerting.

| Metric | Type | Description |
| :--- | :--- | :--- |
| **`up`** | Unknown | Returns `1` if the instance is reachable, `0` otherwise. Used for `InstanceDown` alerts. |
| **`api_auth_attempts_total`** | Counter | **Business Metric**: Tracks login attempts. Labels: `result=success|failure`. |
| **`circuit_breaker_state`** | Gauge | **Health Metric**: State of the circuit breaker (`0=CLOSED`, `1=OPEN`, `2=HALF_OPEN`). |

---

## 2. HTTP Server (Inbound Traffic)
Metrics related to requests received by the ReactiveAPI.

### Latency & Traffic
| Metric | Type | Description |
| :--- | :--- | :--- |
| **`vertx_http_server_requests_total`** | Counter | Total number of processed requests. Use `rate()` to calculate RPS. |
| **`vertx_http_server_active_requests`** | Gauge | Current number of in-flight requests. |
| **`vertx_http_server_active_connections`** | Gauge | Current number of open TCP connections. |
| **`vertx_http_server_response_time_seconds_bucket`** | Histogram | **CRITICAL**: Latency distribution buckets (`0.1`, `0.5`, `1.0`...). Use for P95/P99. |
| `vertx_http_server_response_time_seconds_count` | Histogram | Total count of observed requests (same as `requests_total`). |
| `vertx_http_server_response_time_seconds_sum` | Histogram | Total sum of all request durations. Used for Average Latency. |
| `vertx_http_server_response_time_seconds_max` | Gauge | The slowest request observed in the current window. |

### Low Priority (Payload Sizes)
*   `vertx_http_server_request_bytes_*`: Size of incoming bodies.
*   `vertx_http_server_response_bytes_*`: Size of outgoing bodies.

---

## 3. Database (Connection Pool)
Metrics related to the MySQL Reactive Client.

| Metric | Type | Description |
| :--- | :--- | :--- |
| **`vertx_pool_ratio`** | Gauge | Pool utilization ratio (0.0 to 1.0). High ratio = Pool exhaustion risk. |
| **`vertx_pool_in_use`** | Gauge | Number of connections currently borrowed. |
| **`vertx_pool_queue_pending`** | Gauge | Number of queries waiting for a connection. Should be 0. |
| `vertx_pool_queue_time_seconds_bucket` | Histogram | Time spent waiting for a connection. |
| `vertx_pool_usage_seconds_bucket` | Histogram | Time a connection was held by a query. |

---

## 4. HTTP Client (Outbound Traffic)
Metrics related to calls made **to** external services (e.g., DemoAPI).

| Metric | Type | Description |
| :--- | :--- | :--- |
| **`vertx_http_client_requests_total`** | Counter | Total outgoing requests. |
| `vertx_http_client_response_time_seconds_bucket` | Histogram | Latency of external service calls. |
| `vertx_http_client_active_requests` | Gauge | Number of external requests waiting for response. |

---

## 5. Event Bus (Internal Messaging)
Metrics for the Vert.x Event Bus (communication between Verticles).

| Metric | Type | Description |
| :--- | :--- | :--- |
| `vertx_eventbus_handlers` | Gauge | Number of registered consumers. |
| `vertx_eventbus_pending` | Gauge | Messages queued but not yet processed. High value = Event Loop Blockage. |
| `vertx_eventbus_processed_total` | Counter | Throughput of internal messages. |
| `vertx_eventbus_reply_failures_total` | Counter | timed-out or failed internal RPCs. |

---

## 6. Internal / Noise
These metrics are generally for debugging the scraper itself.

*   `scrape_duration_seconds`
*   `scrape_samples_scraped`
*   `scrape_samples_post_metric_relabeling`
*   `scrape_series_added`