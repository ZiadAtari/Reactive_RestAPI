# v4.5 Implementation Plan: Swagger OpenAPI 3.0

## Goal Description
Implement OpenAPI 3.0 (Swagger) for the Reactive API to standardize documentation and enforce contract-based validation. This involves migrating to a Design-First approach using `vertx-web-openapi` and exposing Swagger UI.

## User Review Required
> [!IMPORTANT]
> **Strict Validation**: The new RouterBuilder will reject requests that do not strictly match the spec (e.g., extra parameters, wrong types), which might break lenient clients formerly allowed by manual validation.

## Proposed Changes

### 1. Preparation & Configuration
#### [MODIFY] [pom.xml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/pom.xml)
- Add `vertx-web-openapi` dependency.
- (Optional) Clean up unused validation libraries if possible.

### 2. Specification Creation
#### [NEW] [openapi.yaml](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/resources/openapi.yaml)
- **Info Object**: Title "Reactive API", Version "4.5".
- **Servers**: `http://localhost:8888`.
- **Components**:
    - `SecuritySchemes`: `BearerAuth` (HTTP Bearer JWT).
    - `Schemas`: `LoginRequest`, `Employee`, `EmployeeBatch`, `ApiError`.
- **Paths**:
    - `POST /login`: Tags [Auth], OpID `login`.
    - `/v1/employees`:
        - `GET`: Tags [V1], OpID `getAllEmployeesV1`, Security [].
        - `POST`: Tags [V1], OpID `createEmployeeV1`, Security [].
    - `/v1/employees/{id}`:
        - `PUT`: Tags [V1], OpID `updateEmployeeV1`.
        - `DELETE`: Tags [V1], OpID `deleteEmployeeV1`.
    - `/v3/employees`:
        - `GET`: Tags [V3], OpID `getAllEmployeesV3`, Security [BearerAuth].
        - `POST`: Tags [V3], OpID `createEmployeeV3`, Security [BearerAuth].
    - `/v3/employees/{id}`:
        - `PUT`: Tags [V3], OpID `updateEmployeeV3`.
        - `DELETE`: Tags [V3], OpID `deleteEmployeeV3`.

### 3. Core Implementation (Web Layer)
#### [MODIFY] [HttpVerticle.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/HttpVerticle.java)
- **Refactor `start()`**: Change signature to use `start(Promise<Void>)` if not already (it is).
- **Initialize RouterBuilder**:
    ```java
    RouterBuilder.create(vertx, "openapi.yaml").onSuccess(builder -> { ... });
    ```
- **Mount Handlers**:
    - `builder.operation("login").handler(authController::login)`
    - `builder.operation("getAllEmployeesV1").handler(controller::getAll)`
    - ... map all operations.
- **Middleware Integration**:
    - **Global**: Ensure `RateLimitHandler` is applied to the generated router or sub-router.
    - **Security**: Configure `builder.securityHandler("BearerAuth", jwtAuthHandler)`.
    - **Verification**: Manually attach `VerificationHandler` to the router *after* build but *before* mounting, or using specific route matches on the generated router.
- **Infrastructure**:
    - Manually mount `/metrics` and `/health/live` on the main router (merging if necessary).
- **Swagger UI**:
    - Serve static files from `webroot/swagger` at `/swagger/*`.

#### [MODIFY] [EmployeeController.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/EmployeeController.java)
- **Remove Defaults**: Remove `SchemaValidator` calls.
- **Update Logic**: Trust `RoutingContext` body (it's already validated).

#### [MODIFY] [AuthController.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/AuthController.java)
- Remove `SchemaValidator.validateLogin`.

### 4. Utilities & Error Handling
#### [MODIFY] [GlobalErrorHandler.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/exception/GlobalErrorHandler.java)
- Add catch block for `ValidationException` (from `vertx-web-openapi`).
- Map to `400 Bad Request` with `ApiError` code `VALIDATION_ERROR`.

## Verification Plan

### Automated Tests
Run existing tests to ensure no regression in core logic:
```bash
mvn test
```

### Manual Verification
1.  **Schema Enforcement**:
    - Send `POST /login` with `{ "user": "admin" }` (missing "name").
    - **Expect**: `400 Bad Request` (Automatic validation failure).
2.  **V3 Security**:
    - Send `GET /v3/employees` without header.
    - **Expect**: `401 Unauthorized`.
3.  **Swagger UI**:
    - Navigate to `http://localhost:8888/swagger`.
    - **Action**: Try "Authorize" with a token and execute `GET /v3/employees`.
    - **Expect**: `200 OK` with JSON list.
4.  **Legacy V1**:
    - Send `GET /v1/employees`.
    - **Expect**: `200 OK` (No Auth required).
