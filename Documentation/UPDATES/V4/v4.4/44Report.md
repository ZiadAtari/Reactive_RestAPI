# v4.4 Update Report: Containerization & Secure Secret Management

## 1. Executive Summary
The v4.4 update successfully transitioned the Reactive REST API into a Cloud-Native, containerized application. Key achievements include the implementation of secure Docker Secrets, a robust multi-container orchestration setup (App + MySQL + Demo API), and the resolution of environment-specific networking and race conditions. The application now follows 12-Factor App principles regarding configuration and backing services.

## 2. Architectural Changes

### 2.1 Secure Secret Management
*   **Problem**: Previous versions relied on plain-text environment variables for sensitive data (DB passwords, RSA Keys).
*   **Solution**: Implemented a "File-First" precedence strategy in `MainVerticle.java`.
    *   The application now checks for `*_FILE` environment variables (e.g., `DB_PASSWORD_FILE`) first.
    *   If present, it reads the secret from the mounted file (Docker Secret standard).
    *   Does a `.trim()` on the content to prevent common whitespace errors from file mounting.
    *   Falls back to standard environment variables for local development simplicity.

### 2.2 Dynamic Configuration
*   **Refactoring**: Hardcoded `localhost` references were removed from `MainVerticle.java`, `HttpVerticle.java`, and `VerificationHandler.java`.
*   **New Environment Variables**:
    *   `DB_HOST` / `DB_PORT`: Configures database connection.
    *   `VERIFICATION_HOST` / `VERIFICATION_PORT`: Configures the connection to the Demo/Verification API.
    *   `APP_URL`: Configures the self-reference URL for the API.

### 2.3 Database Schema & Initialization
*   **Initialization**: Created `init.sql` to automatically seed the database on container startup.
*   **Schema Fixes**:
    *   Changed `users.id` from `INT` to `VARCHAR(255)`/`INT` (Auto-increment maintained for users, but UUID logic clarified). *Correction: `employees.id` was changed to `VARCHAR(36)` to match Java UUID generation.*
    *   Changed `last_modified_at` to `VARCHAR(64)` to resolve Java `String` vs MySQL `TIMESTAMP` casting issues.
*   **Seeding**: Included a default admin user (`admin` / `password123`) with a valid, pre-verified BCrypt hash.

## 3. Infrastructure (Docker & Orchestration)

### 3.1 Dockerfiles
*   **Reactive API**: Multi-stage build (Maven Build -> `eclipse-temurin:17-jre` Runtime). Runs as a non-root user (`appuser`).
*   **Demo API**: Containerized using `eclipse-temurin:17-jre` (replaced deprecated `openjdk:17-slim`).

### 3.2 Docker Compose Orchestration
The stack consists of three services with the following networking configuration:

| Service | Internal Port | Host Port | Description |
| :--- | :--- | :--- | :--- |
| **app** | 8888 | 8888 | Main Reactive API |
| **mysql** | 3306 | 3307 | Primary Database (Mapped to 3307 to avoid local conflict) |
| **demo-api**| 8080 | 8081 | Verification Service (Mapped to 8081 to avoid local conflict) |

### 3.3 Stability Improvements
*   **Startup Race Conditions**: Implemented a `healthcheck` for the MySQL service (`mysqladmin ping`).
*   **Dependency Management**: Configured the `app` service to wait for `mysql` to be `service_healthy` before starting, eliminating "Connection Refused" errors during startup.

## 4. Final Verification
*   **Build**: `docker-compose up -d --build` successfully builds all images.
*   **Connectivity**: The Application successfully connects to MySQL and the Demo API within the Docker network.
*   **Functionality**:
    *   Authentication (Login) works with the seeded credentials.
    *   CRUD operations on Employees work with correct UUID persistence.
    *   Verification Service integration (v3 routes) functions correctly.

## 5. Decision Log
*   **Decision**: Use `eclipse-temurin` instead of `openjdk` images.
    *   *Reason*: `openjdk` images are deprecated on Docker Hub; `eclipse-temurin` provides a stable, production-ready JDK.
*   **Decision**: Map internal ports to different external ports (3307, 8081).
    *   *Reason*: To allow the Docker stack to run alongside local development instances without port conflicts.
*   **Decision**: Host/Port configuration for Verification Service.
    *   *Reason*: Essential for the API to address the Demo container by service name (`demo-api`) instead of `localhost`.

## 6. Next Steps
*   Deploy to staging environment.
*   Consider implementing `Flyway` or `Liquibase` for more robust database schema migrations in the future (replacing `init.sql`).
