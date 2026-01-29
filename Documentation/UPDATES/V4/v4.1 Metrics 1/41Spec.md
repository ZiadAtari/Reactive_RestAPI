Here is the updated **v4.1 Specification & Requirements Sheet**.

I have incorporated your feedback to explicitly require a **Login Metric** (tracking authentication attempts) and **Health Check Liveness Monitoring** (tracking the behavior/uptime of the liveness probe itself).

---

# v4.1 Specification: Application Metrics & Monitoring

**Version:** 4.1.2
**Focus:** Telemetry, Observability, & Liveness Tracking
**Status:** Planned
**Dependencies:** Builds on v4.0 (Health Checks)

---

## 1. Overview

The goal of v4.1 is to implement a comprehensive **Metrics Collection System** using **Micrometer** and **Prometheus**.

This update specifically ensures that critical operational flows—**User Login** and **System Liveness**—are observable. Operators must be able to see *if* the system is alive (Liveness) and *how* users are authenticating (Login) without parsing logs.

---

## 2. Business Requirements (BR)

* **BR-01 Login Observability:** The system must track every login attempt to identify security threats (e.g., brute force attacks) or authentication failures.
* **BR-02 Liveness Monitoring:** The system must track the activity of the Health Check API (`/health/live`) to confirm that the orchestrator (Kubernetes/Docker) is successfully probing the application.
* **BR-03 CRUD Performance:** The system must track latency and throughput for all HTTP verbs (GET, POST, PUT, DELETE).
* **BR-04 Resource Saturation:** The system must report Event Loop delays and Database Pool usage.

---

## 3. Scope & Exclusions

### In Scope (v4.1)

* **Endpoints:** `/metrics` (Prometheus format).
* **Specific Flows:**
* **Login:** `POST /login` (Count & Outcome).
* **Liveness:** `GET /health/live` (Frequency & Latency).


* **HTTP Verbs:** GET, POST, PUT, DELETE.
* **Infrastructure:** Event Loop, DB Pool, JVM.

---

## 4. Functional Requirements (FR)

### FR-01: Metrics Endpoint

* **Route:** `/metrics`
* **Access:** Public (or internal network restricted).
* **Format:** Prometheus Text Format.

### FR-02: HTTP Method & Route Tracking

The system must automatically tag and track the following critical routes with `method`, `code`, and `route` labels:

1. **Login (Auth):**
* **Target:** `POST /login`
* **Goal:** Track latency and error rates of the authentication logic.


2. **Liveness (Health):**
* **Target:** `GET /health/live`
* **Goal:** Confirm the endpoint is being called (Liveness) and is responding fast (Latency).


3. **Domain CRUD:**
* **GET:** `/v3/employees`, `/v3/employees/:id`
* **POST:** `/v3/employees` (Create, Batch)
* **PUT:** `/v3/employees/:id`
* **DELETE:** `/v3/employees/:id`



### FR-03: Custom Login Metric (Granular)

While HTTP metrics track the *request*, we require a dedicated counter for the *business result* of the login.

* **Metric Name:** `api_auth_attempts_total`
* **Trigger:** Inside `AuthController.java`.
* **Tags:**
* `result="success"` (Valid Credentials)
* `result="failure"` (Invalid Credentials / User not found)
* `result="error"` (System error)



### FR-04: System Liveness Metrics

In addition to the HTTP route, the system must export standard JVM uptime metrics to prove the process is alive.

* **Metric:** `process_uptime_seconds` (Standard Micrometer).

---

## 5. Metric Definitions (The "Contract")

The implementation **must** guarantee these metrics are available for scraping:

| Metric Name | Type | Critical Tags | Description | Source |
| --- | --- | --- | --- | --- |
| `api_auth_attempts_total` | **Counter** | `result` | **(NEW)** Count of login successes vs failures. Essential for security monitoring. | **Custom** (`AuthController`) |
| `vertx_http_server_requests_seconds` | Summary | `method="POST"`<br>

<br>`route="/login"` | **(NEW)** HTTP performance of the Login endpoint. | **Built-in** |
| `vertx_http_server_requests_seconds` | Summary | `method="GET"`<br>

<br>`route="/health/live"` | **(NEW)** **Liveness Check.** Confirms K8s is probing the pod. | **Built-in** |
| `vertx_http_server_requests_seconds` | Summary | `method="DELETE"` | Performance of Soft Delete operations. | **Built-in** |
| `vertx_event_loop_delay_seconds` | Gauge | `pool` | Critical Reactive Health indicator. | **Built-in** |
| `process_uptime_seconds` | Gauge | *none* | Time since the JVM started. | **Built-in** |

---

## 6. Implementation Notes

### A. Login Metric Implementation

Modify `AuthController.java`:

```java
// Inside login() method
userVerticle.authenticate(loginReq)
  .onSuccess(user -> {
      registry.counter("api_auth_attempts_total", "result", "success").increment();
      // ... generate token ...
  })
  .onFailure(err -> {
      registry.counter("api_auth_attempts_total", "result", "failure").increment();
      // ... fail request ...
  });

```

### B. Liveness Implementation

Ensure `MainVerticle` configures Micrometer to **allow** the `/health/live` route (sometimes defaults exclude health checks to reduce noise).

* **Config:** `VertxPrometheusOptions.setLabelMatch(EnumSet.of(Label.HTTP_METHOD, Label.HTTP_CODE, Label.HTTP_ROUTE))`