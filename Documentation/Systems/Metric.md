# Metrics System: Architecture & Implementation

The Metrics system provides real-time observability into the application's performance, health, and business operations. It is built on a **Micrometer** core backed by a **Prometheus** registry, utilizing a "Pull" (Scrape) architecture.

## 1. Architecture Overview

```mermaid
graph LR
    Code[Application Code] -->|Records| Micro[Micrometer Registry]
    Micro -->|Formats| Endpoint[/metrics Endpoint]
    Prom[Prometheus] -->|Scrapes (15s)| Endpoint
    Prom -->|Evaluates Rules| Alert[AlertManager]
```

The system does not "push" metrics to a server. Instead, it aggregates data in-memory and exposes it via HTTP for Prometheus to collect.

## 2. Implementation Details

### A. The Core Engine ([AppLauncher.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/main/AppLauncher.java))
We utilize **Micrometer**, a vendor-neutral application metrics facade. Code instrumentation happens *before* the Vert.x instance starts to ensure the entire lifecycle is captured.

#### 1. Enabling Prometheus & Labels
We configure the `VertxPrometheusOptions` to automatically capture standard HTTP traffic data.
```java
options.setMetricsOptions(
    new MicrometerMetricsOptions()
        .setPrometheusOptions(new VertxPrometheusOptions().setEnabled(true))
        .setLabels(EnumSet.of(Label.HTTP_METHOD, Label.HTTP_CODE, Label.HTTP_ROUTE))
        .setEnabled(true));
```
*   **Significance**: The `setLabels` call allows us to slice data by route (e.g., `route="/login"`), essential for isolating performance issues.

#### 2. Server-Side Histograms (SLI Buckets)
Standard Prometheus summaries cannot be aggregated across multiple pods/instances. To solve this, we define explicit **Histogram Buckets** in `AppLauncher`.
```java
.serviceLevelObjectives(0.1, 0.5, 1.0, 5.0, 10.0)
```
*   **What this does**: It groups every request latency into specific buckets (100ms, 500ms, 1s, etc.).
*   **Benefit**: This allows Prometheus to calculate accurate **P95 and P99 Latency** using the `histogram_quantile` function using the raw bucket data.

### B. Exposure Layer ([HttpVerticle.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/HttpVerticle.java))
Metrics are exposed via a standard HTTP endpoint.
```java
router.route("/metrics").handler(PrometheusScrapingHandler.create());
```
*   **Endpoint**: `GET /metrics`
*   **Format**: Prometheus Exposition Format (Text-based).
*   **Access**: Public (Internal network).

### C. Custom Instrumentation ([AuthController.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/AuthController.java))
Beyond standard HTTP tracking, we instrument specific **Business Logic** using the internal Micrometer registry.

```java
// Incrementing a custom counter
io.vertx.micrometer.backends.BackendRegistries.getDefaultNow()
    .counter("api_auth_attempts_total", "result", "success").increment();
```
*   **Why**: HTTP status codes don't tell the whole story. A `401 Unauthorized` is an HTTP success (the server worked), but a business failure (login failed). This counter tracks the actual business outcome.

## 3. Metrics Reference

| Metric Name | Type | Key Labels | Description |
| :--- | :--- | :--- | :--- |
| `vertx_http_server_requests_seconds` | Histogram | `method`, `code`, `route` | **Golden Signal**. Tracks throughput (count) and latency (sum/buckets) of all HTTP requests. |
| `api_auth_attempts_total` | Counter | `result` (`success`/`failure`) | Tracks the volume and success rate of user login attempts. |
| `circuit_breaker_state` | Gauge | `name` | **Resilience**. `0`=Closed (Reference), `1`=Open (Failing), `2`=Half-Open. |

## 4. The Scrape Pipeline

1.  **Event Occurs**: A user logs in.
2.  **Micrometer Records**: The Java Application increments the `api_auth_attempts_total` counter in memory.
3.  **Endpoint**: The `/metrics` page output dynamically updates to show the new count.
4.  **Prometheus Scrapes**: Every 15 seconds (configured in `prometheus.yml`), Prometheus requests `/metrics`.
5.  **TSDB Storage**: Prometheus stores the timestamped value.
6.  **Alerting**: If the rate of change `rate(api_auth_attempts_total...[5m])` indicates a spike in failures, AlertManager triggers a notification.

## 5. Alerting Configuration
*   **Rules File**: `Metrics/Prometheus/alert_rules.yml`
*   **Key Rules**:
    *   **HighErrorRate**: `rate(5xx errors) > 5%`
    *   **HighLatency**: `P95 Latency > 500ms`
    *   **InstanceDown**: Target unreachable for > 30s.
