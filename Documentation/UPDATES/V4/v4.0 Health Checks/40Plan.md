# v4.0 Implementation Plan

## Goal Description
Implement standard Liveness (`/health/live`) probe to support container orchestration. 

> [!NOTE]
> Readiness probe (`/health/ready`) and Database Health checks have been removed from the scope of v4.0.

## User Review Required
> [!NOTE]
> The health endpoint is **public**. No sensitive data will be exposed.

## Proposed Changes

### Build Configuration
#### [MODIFY] [pom.xml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/pom.xml)
- Add `vertx-health-check` dependency.

### Web Layer
#### [MODIFY] [HttpVerticle.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/HttpVerticle.java)
- Initialize `HealthCheckHandler`.
- Register `server-live` check (always UP).
- Mount route:
    - `GET /health/live`

## Verification Plan

### Automated Tests
- **Manual Verification (curl)**: Since this is a new infrastructure feature, we will verify it manually using `curl` against the running application.

### Manual Verification
1.  **Start the Application**:
    ```bash
    mvn clean compile exec:java
    ```

2.  **Verify Liveness (Server UP)**:
    ```bash
    curl -v http://localhost:8888/health/live
    ```
    *   **Expected**: HTTP 200 `{"outcome": "UP", ...}`
