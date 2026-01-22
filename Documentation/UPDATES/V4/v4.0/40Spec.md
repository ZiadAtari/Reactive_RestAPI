---

# v4.0 Specification: Industry-Standard Health Checks

**Version:** 4.0.0
**Focus:** Observability (Liveness & Readiness Probes)
**Status:** Planned
**Previous Version:** v3.4.0 (JSON Schema & Batch Updates)

---

## 1. Overview

The goal of v4.0 is to implement a standard "Liveness" API. This endpoint enables container orchestrators (like Kubernetes or Docker Swarm) to automatically monitor if the application is running.

---

## 2. Business Requirements (BR)

* **BR-01 Orchestration Support:** The API must provide a standard endpoint that allows external systems to determine if the container should be restarted (Liveness).
* **BR-03 Operational Visibility:** Support teams must have a quick way to verify system status.

---

## 3. Scope & Exclusions

### In Scope (v4.0)

* **Dependencies:** Integration of `vertx-health-check`.
* **Liveness Probe:** `/health/live`

### Out of Scope (Defer to v4.1)

* **Metrics & Telemetry:** Prometheus/Micrometer integration.


---

## 4. Functional Requirements (FR)

### FR-01: Liveness Probe (`/health/live`)

* **Logic:** The endpoint must return `200 OK` if the HTTP server is running and the Event Loop is not blocked.
* **Failure:** If the server is crashed or deadlocked, the request will time out or fail (orchestrator handles the restart).
* **Response:**
    * **Success:** HTTP 200 `{"outcome": "UP"}`
    * **Failure:** HTTP 503 (or no response)

---

## 5. Technical Specification

### 5.1 Architecture Update

The health check logic is contained within the Web Layer.

**Flow:**
`Client` -> `GET /health/live` -> `HttpVerticle` -> `HealthCheckHandler`

### 5.2 Dependency Changes (`pom.xml`)

Add the official Vert.x Health Check module.

```xml
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>vertx-health-check</artifactId>
</dependency>
```

### 5.3 Component Specifications

#### **A. `HttpVerticle` (Web Layer)**

* **File:** `src/main/java/ziadatari/ReactiveAPI/web/HttpVerticle.java`
* **Role:** The "Exposer" of health endpoints.
* **Initialization:** Create `HealthCheckHandler` instance in `start()`.
* **Registration:**
    * Register `server-live` procedure (always returns Promise complete).
* **Routes:**
    * `router.get("/health/live").handler(livenessHandler)`

---

## 6. API Contract

| Endpoint | Method | Auth Required | Status Code | Condition |
| --- | --- | --- | --- | --- |
| `/health/live` | GET | No | **200 OK** | Server is responding. |
| `/health/live` | GET | No | **503 / Timeout** | Server is overloaded or crashed. |

**Sample Success Response (`/health/ready`):**

```json
{
  "checks": [
    {
      "id": "database-ready",
      "status": "UP"
    }
  ],
  "outcome": "UP"
}

```

---

## 8. Migration & Deployment Notes

* **Environment Variables:** No new variables required.
* **Security:** These endpoints are **public**. Do not expose sensitive data (e.g., DB connection strings) in the health response. The standard `vertx-health-check` implementation is safe by default.