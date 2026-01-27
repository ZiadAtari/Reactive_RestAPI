# Implementation Plan - v4.4 Containerization & Secure Secret Management

This plan details the transition to a container-native architecture with secure secret management, complying with `44Spec.md`.

## User Review Required

> [!NOTE]
> **Consistency Update**: In addition to `DB_PASSWORD` and `RSA_PRIVATE_KEY` (mandated by the spec), I will also apply the "File-Based Loading" pattern to `RSA_PUBLIC_KEY`. This ensures a uniform configuration strategy across the entire application.

## Proposed Changes

### Core Application
#### [MODIFY] [MainVerticle.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/main/MainVerticle.java)
We will refactor the configuration loading logic to utilize a "Check File First" strategy.
-   **New Helper Method**: `private String loadSecret(String key)`
    -   Checks `System.getenv(key + "_FILE")`.
    -   If present, reads the file content from that path.
    -   If absent, falls back to `System.getenv(key)`.
-   **Updates**:
    -   Replace direct `System.getenv("DB_PASSWORD")` with `loadSecret("DB_PASSWORD")`.
    -   Replace direct `System.getenv("RSA_PRIVATE_KEY")` with `loadSecret("RSA_PRIVATE_KEY")`.
    -   Replace direct `System.getenv("RSA_PUBLIC_KEY")` with `loadSecret("RSA_PUBLIC_KEY")`.
-   **Cleanup**: Remove insecure hardcoded fallback keys (or wrap them strictly for local-dev-only warning logs).

### Infrastructure
#### [NEW] [Dockerfile](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/Dockerfile)
A multi-stage Dockerfile for production-ready builds.
-   **Build Stage**: `maven:3.8-openjdk-17` to compiling the project and shade the Fat JAR.
-   **Run Stage**: `openjdk:17-slim` (or `eclipse-temurin`) for a minimal runtime footprint.
-   **Config**: Exposes port `8888`.

#### [NEW] [docker-compose.yml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/docker-compose.yml)
Orchestration for running the stack locally with Docker Secrets.
-   **Services**:
    -   `app`: The Reactive API.
    -   `mysql`: Database Container.
-   **Secrets**:
    -   `db_password`: Mounted to `/run/secrets/db_password`.
    -   `rsa_private_key`: Mounted to `/run/secrets/rsa_private_key`.
-   **Environment**:
    -   Sets `DB_PASSWORD_FILE` to `/run/secrets/db_password`.
    -   Sets `RSA_PRIVATE_KEY_FILE` to `/run/secrets/rsa_private_key`.

## Verification Plan

### Automated Verification
1.  **Docker Build**:
    ```bash
    docker build -t reactive-api:4.4 .
    ```
2.  **Compose Startup**:
    ```bash
    docker-compose up -d
    ```

### Manual Verification
1.  **Log Inspection**: Verify `MainVerticle` logs indicate successful connection to MySQL and deployment of verticles.
2.  **Liveness Check**:
    -   GET `http://localhost:8888/health/live` -> Expect `200 OK`.
3.  **Authentication Flow**:
    -   POST `/login` with valid credentials. This confirms the `RSA_PRIVATE_KEY` was loaded correctly from the secret file and signed the token.
4.  **Legacy Fallback**:
    -   Run the app via IntelliJ/Maven *without* file variables to ensure it still falls back to standard environment variables (Developer Experience).
