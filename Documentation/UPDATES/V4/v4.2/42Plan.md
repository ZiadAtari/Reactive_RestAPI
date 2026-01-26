# v4.2 Implementation Plan: Native Metrics

## Goal Description
Refine `AppLauncher.java` to emit sanitized **Histogram Buckets** instead of summaries. This enables native graphing of percentiles (P95, Max) directly in Prometheus, satisfying the requirement to visualize data without Grafana.

## User Review Required
> [!IMPORTANT]
> This change fundamentally alters the metric format. Old `quantile` queries will stop working. Use `histogram_quantile` instead.

## Proposed Changes

### 1. Codebase Changes (Metrics Hygiene)
#### [MODIFY] [AppLauncher.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/main/AppLauncher.java)
- **Objective**: "Clean and Sanitize" metrics for Prometheus.
- **Actions**:
    - **REMOVE**: `setPublishQuantiles(true)`. (Source of dirty, un-aggregatable data).
    - **ADD**: `MeterFilter` for `http.server.requests`.
    - **ADD**: `DistributionStatisticConfig` with `percentileHistogram(false)` and `sla(0.1, 0.5, 1.0, 5.0, 10.0)`.
    - **Reasoning**: This forces Micrometer to output `_bucket` series, which are the raw material Prometheus needs to draw histograms.

### 2. Infrastructure
#### [MODIFY] [docker-compose.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Prometheus/docker-compose.yml)
- Add `alertmanager` service (Port 9093).
- Ensure `grafana` is **actions excluded**.

### 3. Configuration (Alerting)
#### [NEW] [prometheus/alert_rules.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Prometheus/alert_rules.yml)
- Define standard alerts (`InstanceDown`, `HighLatency`).

#### [MODIFY] [prometheus.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Prometheus/prometheus.yml)
- Configure AlertManager target.

---

## Verification Plan

### Manual Verification (Prometheus Native)
1.  **Generate Traffic**: `curl localhost:8888/login` (Generate success/fail calls).
2.  **Verify Clean Data**:
    *   Query: `vertx_http_server_requests_seconds_bucket`
    *   Confirm `le` labels exist (0.1, 0.5, etc.).
3.  **Verify Visualizations**:
    *   Open Prometheus Graph Tab.
    *   **Graph P95 Latency**: `histogram_quantile(0.95, sum(rate(vertx_http_server_requests_seconds_bucket[5m])) by (le))`
    *   Confirm the graph renders a valid line.
