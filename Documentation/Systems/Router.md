# Router System

The Router system manages the HTTP interface, request routing, and middleware integration using Vert.x Web.

## Components

### [HttpVerticle](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/HttpVerticle.java)
- **Purpose**: Server lifecycle management and route orchestration.
- **Deployment**: Deployed with multiple instances (`Runtime.getRuntime().availableProcessors()`) to utilize all CPU cores.
- **Key Features**:
    - Registers global error handlers and body parsers.
    - Mounts versioned API routes (`/v1/*` for legacy, `/v3/*` for authenticated).
    - **Observability**: Exposes Prometheus metrics at `/metrics`.
    - Uses a `Future.all` during startup to track deployment status.
    - **Performance**: Configures the `WebClient` with a connection pool (max 100 connections) for high-concurrency external verification.

### [EmployeeController](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/EmployeeController.java)
- **Purpose**: Maps HTTP requests to Event Bus messages and standardizes success responses.
- **Workflow**:
    1. Extracts parameters (path, query, body).
    2. **Validation**: Performs "fail-fast" JSON Schema validation (`SchemaValidator`) and checks for batch vs. single inputs.
    3. Constructs safe DTOs using the Builder pattern (injecting audit data).
    4. Requests action from `EmployeeVerticle` via Event Bus.
    5. Formats success response with operation metadata and timestamp.

### [AuthController](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/AuthController.java)
- **Purpose**: Handles user login requests (`POST /login`).
- **Workflow**:
    1. **Validation**: Validates request body using `SchemaValidator` (ensures `username`/`password` presence).
    2. requests authentication from `UserVerticle` passing the DTO JSON.
    3. requests token issuance from `AuthVerticle`.
    4. Returns JWT token to client.

### Middleware Handlers
- **[JwtAuthHandler](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/JwtAuthHandler.java)**:
    - Protects `/v3/*` mutation routes.
    - Verifies `Authorization: Bearer <token>` header using local RSA Public Key.
    - Fails with `UNAUTHORIZED` if invalid.
- **[RateLimitHandler](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/auth/RateLimitHandler.java)**:
    - Implements a fixed-window rate limiting algorithm.
    - Uses Vert.x `SharedData` (`LocalMap`) to track request counts across event loops safely.
- **[VerificationHandler](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/auth/VerificationHandler.java)**:
    - Performs service-to-service IP verification using an external "Demo" API.
    - Decouples token retrieval using the **Event Bus**.

### [SchemaValidator](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/util/SchemaValidator.java)
- **Purpose**: Centralized JSON Schema validation utility (Part of 3.4 Update).
- **Functionality**:
    - Pre-compiles schemas for Login, Employee, and Employee Batch.
    - Provides synchronous "fail-fast" validation methods.
    - Maps validation errors to specific system `ErrorCode`s.

## Resilience: [CustomCircuitBreaker](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/CustomCircuitBreaker.java)
- **Purpose**: Protective wrapper for the `WebClient` during external service calls.
- **Configuration**:
    - **Execution Timeout**: 500ms.
    - **Reset Timeout**: 800ms.
    - **Max Failures**: 5.
- **Features**:
    - Simple implementation of the Circuit Breaker pattern.
    - Transitions between `CLOSED`, `OPEN`, and `HALF_OPEN` states to isolate external failures.

### Health Checks
- **HealthCheckHandler**:
    - Exposes standard Liveness Probe at `/health/live`.
    - Returns `200 OK` {"outcome": "UP"} to indicate the server is running.

## Request Flow
1. **Entry**: Request hits the server on port `8888`.
2. **Middleware Layer**:
    - `BodyHandler` parses JSON.
    - `RateLimitHandler` checks IP quotas.
    - `VerificationHandler` verifies IP with Demo service (guarded by `CustomCircuitBreaker`).
    - `HealthCheckHandler` responds to `/health/live`.
3. **Controller Layer**: `EmployeeController` routes the request based on URL and Verb.
4. **Service Layer**: Event Bus message sent to `EmployeeVerticle`.
5. **Response**: Success response formatted by controller or failure handled by `GlobalErrorHandler`.
