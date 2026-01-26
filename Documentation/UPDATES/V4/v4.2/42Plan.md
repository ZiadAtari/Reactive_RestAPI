# v4.2 Implementation Plan: Visualization & Alerting

## Goal Description
Implement Grafana for visualization and AlertManager for alerting to complete the Observability stack started in v4.1.

## User Review Required
> [!IMPORTANT]
> This update adds two new containers to the `docker-compose.yml` stack. Ensure Docker resources are sufficient.

## Proposed Changes

### Codebase Changes
#### [MODIFY] [AppLauncher.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/main/AppLauncher.java)
- **CRITICAL**: Configure `MeterFilter` to enable Histogram Buckets for `http.server.requests`.
- Switch from Client-Side Summaries to Server-Side Histograms to support Aggregation.

### Infrastructure
#### [MODIFY] [docker-compose.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Prometheus/docker-compose.yml)
- Add `grafana` service (Port 3000).
- Add `alertmanager` service (Port 9093).
- Configure networks to allow Prometheus to talk to AlertManager.

### Configuration
#### [NEW] [grafana/provisioning/datasources/datasource.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Grafana/provisioning/datasources/datasource.yml)
- Auto-provision Prometheus as the default data source.

#### [NEW] [grafana/provisioning/dashboards/dashboard.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Grafana/provisioning/dashboards/dashboard.yml)
- Auto-load dashboards from the JSON file.

#### [NEW] [grafana/dashboards/reactive-api-overview.json](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Grafana/dashboards/reactive-api-overview.json)
- The JSON definition of the dashboard.

#### [NEW] [prometheus/alert_rules.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Prometheus/alert_rules.yml)
- Define `InstanceDown`, `HighErrorRate`, etc.

#### [MODIFY] [prometheus.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Metrics/Prometheus/prometheus.yml)
- Uncomment and point to `alert_rules.yml`.
- Uncomment and point to `alertmanager` target.

---

## Verification Plan

### Automated Verification
* None (Infrastructure/Config changes).

### Manual Verification
1.  **Bring up Stack:** `docker-compose up -d`
2.  **Verify Grafana:**
    * Open `http://localhost:3000` (User/Pass: admin/admin).
    * Check "Reactive API Overview" dashboard is auto-loaded.
    * Verify graphs show data (Login, Liveness, etc.).
3.  **Verify Alerting:**
    * Stop the API: `Ctrl+C` or kill the process.
    * Wait 30s.
    * Check `http://localhost:9090/alerts` (Prometheus) shows `InstanceDown` as FIRING.
    * Check `http://localhost:9093` (AlertManager) shows the received alert.
