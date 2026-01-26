# v4.0 Update Report

## 1. Overview
Version 4.0.0 focuses on **Observability** and **Codebase Consolidation**. The primary addition is a standard Liveness Probe to support container orchestration. Additionally, the `domain` package was consolidated into the `repository` package to reduce redundancy.

## 2. Key Features

### 2.1 Liveness Probe (`/health/live`)
*   **Purpose**: Allows external orchestrators (Kubernetes, Docker) to check if the application is running.
*   **Mechanism**: Uses `vertx-health-check` to expose a standard lightweight endpoint.
*   **Response**: Returns HTTP 200 `{"outcome": "UP"}` when the server is responsive.

### 2.2 Domain Package Consolidation
*   **Change**: The `ziadatari.ReactiveAPI.domain` package was removed.
*   **Action**: `User.java` was moved to `ziadatari.ReactiveAPI.repository`.
*   **Impact**: Simplified project structure by grouping the User entity with its associated Repository and Verticle.

## 3. Technical Changes

### Dependencies
*   **Added**: `io.vertx:vertx-health-check:4.5.23`

### Source Code
*   **`HttpVerticle.java`**:
    *   Initialize `HealthCheckHandler`.
    *   Register `server-live` procedure.
    *   Mount `GET /health/live`.
*   **Refactoring**:
    *   Moved `src/main/java/ziadatari/ReactiveAPI/domain/User.java` -> `src/main/java/ziadatari/ReactiveAPI/repository/User.java`.
    *   Updated imports in `UserRepository.java` and `UserVerticle.java`.

## 4. Verification
The update was verified by compiling the codebase (`mvn clean compile`) and can be tested manually:

```bash
curl -v http://localhost:8888/health/live
```
