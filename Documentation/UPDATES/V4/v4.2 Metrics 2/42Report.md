# v4.2 Update Report: Metric Hygiene & Native Alerting

The v4.2 update transforms the observation layer of the Reactive API from "Client-Side Summaries" to "Prometheus Native Histograms". This shift ensures that metrics are clean, aggregatable, and accurate for both visualization and alerting.

## 1. Key Changes

### A. Metric Sanitization ([AppLauncher.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/main/AppLauncher.java))
*   **Removed**: Client-side quantiles (`setPublishQuantiles(true)`). These were pre-calculated "summaries" that could not be averaged across multiple server instances.
*   **Added**: Server-side Histograms (Buckets).
*   **SLA Configuration**: Defined explicit boundaries (`0.1s`, `0.5s`, `1.0s`, `5.0s`, `10.0s`) for the `vertx_http_server_response_time_seconds` metric.
*   **Benefit**: Allows using the `histogram_quantile` function in Prometheus for precise P95/P99 latency tracking.

### B. Alerting Infrastructure ([docker-compose.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Prometheus/docker-compose.yml))
*   **Service**: Added **AlertManager** to the Docker stack (Port 9093).
*   **Rules**: Created **[alert_rules.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Prometheus/alert_rules.yml)**.
*   **Critical Alerts**:
    1.  `InstanceDown`: Triggers if the API is unreachable for 30s.
    2.  `HighErrorRate`: Triggers if 5xx errors exceed 5% of traffic.
    3.  `HighLatency`: Triggers if P95 latency exceeds 500ms.

### C. Technical Documentation
*   **[42exp.md](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Documentation/UPDATES/V4/v4.2/42exp.md)**: A complete catalog of metrics categorized by Business, HTTP, Database, and internal health, including a guide on **Filtering** and **Aggregation**.

---

## 2. Verification Guide (PromQL)

Use these queries in the Prometheus "Graph" tab to verify the update. All time-based queries are scaled to **Milliseconds** for better readability.

### Response Time (P95 Latency)
Displays the value below which 95% of server responses fall.
```promql
histogram_quantile(0.95, sum(rate(vertx_http_server_response_time_seconds_bucket[5m])) by (le)) * 1000
```

### True Error Rate (%)
Excludes scraper noise (`/metrics`) to show the actual failure rate of business logic.
```promql
(
  sum(rate(vertx_http_server_response_time_seconds_count{code=~"5..", route!="/metrics"}[1m]))
  /
  sum(rate(vertx_http_server_response_time_seconds_count{route!="/metrics"}[1m]))
) * 100
```

### Circuit Breaker Status
Shows if specific system domains are currently protecting the backend from overload.
```promql
# Login pipeline status
max(circuit_breaker_state{name="auth-login"})

# V3 verification status
max(circuit_breaker_state{name="v3-verify"})

# V1 verification status
max(circuit_breaker_state{name="v1-verify"})
```
*(0 = Closed/OK, 1 = Open/Failing, 2 = Half-Open)*
