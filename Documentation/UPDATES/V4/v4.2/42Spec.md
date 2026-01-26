# v4.2 Specification: Visualization & Alerting

**Version:** 4.2.1
**Focus:** Observability Visualization, Intelligent Alerting, & Metrics Diagnosis
**Status:** In Progress
**Dependencies:** Builds on v4.1 (Metrics)

---

## 1. Overview

With metrics collection established in v4.1, v4.2 focuses on **making those metrics useful**. We will implement **Grafana** for real-time visualization and **Prometheus AlertManager** for proactive incident response.

Operators should not need to query Prometheus manually. They should have a "Single Pane of Glass" dashboard and receive notifications when critical thresholds are breached.

---

## 2. Business Requirements (BR)

* **BR-01 Operational Dashboard:** Provide a visual dashboard showing real-time System Health, Traffic utilization, and Error Rates.
* **BR-02 Proactive Alerting:** The system must notify operators when:
    * The application goes down (Liveness failure).
    * Error rates exceed 5% (Quality of Service degradation).
    * Latency spikes above 500ms (Performance degradation).
* **BR-03 Integration:** All visualization and alerting tools must run in the existing Docker Compose environment.
* **BR-04 Metrics Repair:** Ensure all critical metrics (specifically Latency/Response Time) are correctly exposed in a format suitable for aggregation (Histograms).

---

## 3. Scope

### In Scope
* **Infrastructure:**
    * Add **Grafana** to `docker-compose.yml`.
    * Add **AlertManager** to `docker-compose.yml`.
* **Configuration:**
    * Grafana Provisioning (Datasources & Dashboards).
    * Prometheus Alert Rules.
    * **[NEW] Update `AppLauncher`** to enable Histogram Buckets.
* **Dashboard:**
    * Create "Reactive API Overview" dashboard.

### Out of Scope
* **Email/Slack Integration:** For this version, Alerts will be verified via the AlertManager UI/API.

---

## 4. Functional Requirements

### FR-01: Grafana Dashboard
The dashboard must include:
1.  **Global Status:** Up/Down indicator.
2.  **Traffic:** Request rate (RPS) broken down by method.
3.  **Latency:** p95 Response time (calculated via `histogram_quantile`).
4.  **Business Logic:** Login Success/Failure Monitor (`api_auth_attempts_total`).
5.  **Circuit Breaker:** Current state of the Breaker.

### FR-02: Critical Alerts
Define the following Prometheus Rules:
1.  `InstanceDown`: Triggers if `up == 0` for > 30s.
2.  `HighErrorRate`: Triggers if 5xx responses > 5% of total traffic.
3.  `HighLatency`: Triggers if p95 latency > 500ms.

---

## 5. Diagnosis: Missing Response Times

### Issue
Users report that "Response Time" metrics are returning nothing in Prometheus.

### Explanation (Root Cause)
The current implementation in `AppLauncher.java` enables `setPublishQuantiles(true)`. In Micrometer/Prometheus, this generates **Client-Side Summaries**.
*   **Result:** You get pre-calculated quantiles (e.g., `quantile="0.95"`).
*   **Problem:** Most standard Grafana dashboards and aggregation queries use `histogram_quantile` which requires **Server-Side Histograms** (buckets). You cannot aggregate Summaries across multiple instances (e.g., if we scale to 3 replicas).

### Solution
We must update `AppLauncher.java` to configure a `MeterFilter` that enables **SLA Boundaries (Buckets)** or **Percentile Histograms**.
Ideally, we should emit `_bucket` metrics:
*   `vertx_http_server_requests_seconds_bucket{le="0.1"}`
*   `vertx_http_server_requests_seconds_bucket{le="0.5"}`
*   ...

This change is required to satisfy **BR-04**.

---

## 5.1 Metrics Contract (Post-Update)

After the `AppLauncher` update, the following metrics will be exposed. **Bold** indicates new metrics enabling Grafana.

| Metric Name | Type | Labels | Description |
| :--- | :--- | :--- | :--- |
| **`vertx_http_server_requests_seconds_bucket`** | **Histogram** | `le`, `method`, `code`, `route` | **(NEW)** Critical for `histogram_quantile` (Response Time). Buckets: `[0.1, 0.5, 1.0, 5.0, 10.0]` |
| `vertx_http_server_requests_seconds_count` | Counter | `method`, `code`, `route` | Total request count (Traffic). |
| `vertx_http_server_requests_seconds_sum` | Counter | `method`, `code`, `route` | Total duration sum (used for Average Latency). |
| `api_auth_attempts_total` | Counter | `result` | Login success/failure count. |
| `process_uptime_seconds` | Gauge | *none* | Logic for "Instance Down" alerts. |
| `jvm_memory_used_bytes` | Gauge | `area`, `id` | Memory usage (Heap/Non-Heap). |
| `jvm_gc_pause_seconds` | Summary | `action`, `cause` | Garbage Collection pauses. |

---

## 6. Technical Stack & Maintenance

*   **Grafana:** Latest OSS Version.
*   **AlertManager:** Latest OSS Version.
*   **Code Change:** Update `AppLauncher.java` to inject `DistributionStatisticConfig`.
