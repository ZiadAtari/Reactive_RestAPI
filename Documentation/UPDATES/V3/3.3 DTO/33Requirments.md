# 3.3 DTO Implementation Requirements & Specifications

## 1. Overview
The goal of this update is to enforce a secure and strictly typed data flow for authentication-related operations. Currently, raw `JsonObject` structures are used for passing sensitive credentials (username/password), which poses security risks (accidental logging) and maintainability issues (lack of validation, string-typing).

## 2. Functional Requirements

### 2.1 Login Request Handling
*   **REQ-001**: The system MUST accept login requests containing `username` and `password`.
*   **REQ-002**: The system MUST validate that both fields are present and non-empty *before* processing execution logic.
*   **REQ-003**: The system MUST NOT rely on manual string extraction (e.g., `json.getString("username")`) within the Controller or Service logic; it should use a dedicated DTO.

### 2.2 User Context Propagation
*   **REQ-004**: Upon successful authentication, the system MUST return a structured user context (username, roles) to the caller.
*   **REQ-005**: The system MUST NOT pass the raw password hash or sensitive DB fields back to the controller or client.

## 3. Non-Functional Requirements

### 3.1 Security (Logging Safety)
*   **NFR-001**: The DTOs containing passwords MUST override `toString()` to redact sensitive information (e.g., return `[PROTECTED]`).
*   **NFR-002**: This ensures that accidental logging of the request object in debug modes or error handlers does not leak credentials.

### 3.2 Maintainability
*   **NFR-003**: The code MUST use strong typing. Changes to field names should trigger compile-time errors rather than runtime null pointer exceptions.

## 4. Technical Specifications

### 4.1 Class: `LoginRequestDTO`
*   **Package**: `ziadatari.ReactiveAPI.dto`
*   **Fields**:
    *   `private String username`
    *   `private String password`
*   **Methods**:
    *   `constructor(JsonObject json)`: handling safe extraction.
    *   `toJson()`: for Event Bus serialization.
    *   `isValid()`: returns boolean; checks for null/empty.
    *   `toString()`: **MUST REDACT PASSWORD**.

### 4.2 Class: `UserContextDTO`
*   **Package**: `ziadatari.ReactiveAPI.dto`
*   **Fields**:
    *   `private String username`
    *   `private String role` (Default: "user")
*   **Methods**:
    *   `toJson()`: for Event Bus reply.
    *   `constructor(String username, String role)`

### 4.3 Modified Components
1.  **AuthController**:
    *   Convert `RoutingContext` body -> `LoginRequestDTO`.
    *   Call `dto.isValid()` -> 400 Bad Request if false.
    *   Send `dto.toJson()` to `users.authenticate`.
2.  **UserVerticle**:
    *   Receive Json -> Convert to `LoginRequestDTO`.
    *   Use getters for DB lookup.
    *   Reply with `UserContextDTO.toJson()`.
3.  **AuthVerticle**:
    *   Update to expect/utilize the standard keys defined in DTOs (though it mainly needs username).

## 5. Success Criteria
*   Login flow works identical to user experience.
*   `toString()` test confirms password is not visible.
*   Blank credentials return 400 Bad Request immediately.
