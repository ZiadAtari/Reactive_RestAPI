# v4.2 Specification: Native Metrics & Alerting

**Version:** 4.2.3
**Focus:** Native Prometheus Visualization, Data Hygiene, & Alerting
**Status:** In Progress

---

## 1. Overview

v4.2 focuses on **cleaning and sanitizing exposed metrics** to ensure they can be effectively visualized using **Prometheus's built-in Graphing capabilities**.

We will move away from pre-calculated client-side summaries (which obscure data distribution) and implement **Server-Side Histograms**. This allows operators to run valid `histogram_quantile` queries directly in Prometheus to analyze response times and failure rates.

---

## 2. Business Requirements (BR)

* **BR-01 Native Visualization:** Metric data must be structured (Buckets) such that it can be graphed natively in Prometheus without external tools like Grafana.
* **BR-02 Data Hygiene:** Ensure response time metrics are clean and aggregatable across multiple instances.
* **BR-03 Proactive Alerting:** The system must notify operators via AlertManager when:
    * The application goes down (Liveness failure).
    * Error rates exceed 5%.
    * Latency spikes above 500ms.

---

## 3. Scope

### In Scope
* **Infrastructure:**
    * Add **AlertManager** to `docker-compose.yml`.
* **Configuration:**
    * **[CRITICAL] Update `AppLauncher`** to emit clean Histogram Buckets.
    * Prometheus Alert Rules.
* **Documentation:**
    * Provide the exact PromQL expressions needed to graph the user's requested data points.

### Out of Scope
* **Grafana:** External dashboarding tools are explicitly excluded. Visualization will rely on Prometheus Native Graphs.

---

## 4. Functional Requirements

### FR-01: Metric Data Hygiene (The "Clean & Sanitize" Requirement)
The application must stop emitting "black box" pre-calculated quantiles (`quantile="0.95"`).
Instead, it must emission **SLA Buckets** (`le="..."`) for the metric `vertx_http_server_requests_seconds`.

**Required Buckets:**
- `0.1s` (100ms) - Fast
- `0.5s` (500ms) - SLA Boundary
- `1.0s` (1s) - Slow
- `5.0s` (5s) - Very Slow
- `10.0s`: Timeout

### FR-02: Native Graphing Capabilities
The following data points must be queryable via Prometheus Expression Browser:
1.  **Avg Response Time**: `rate(sum)/rate(count)`
2.  **P95 Response Time**: `histogram_quantile(0.95, rate(bucket))`
3.  **Failure Rate**: `rate(count{code=~"5.."}) / rate(count)`

---

## 5. Required Changes

### `AppLauncher.java`
- **Disable**: `setPublishQuantiles(true)` (Removes dirty/un-aggregatable data).
- **Enable**: `DistributionStatisticConfig` with explicit bucket boundaries (Adds clean/graphable data). 
