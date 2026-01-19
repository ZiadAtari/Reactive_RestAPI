# DTO Architecture Review

## Overview
This review analyzes the current handling of sensitive data within the authentication flow and identifies areas where introducing Data Transfer Objects (DTOs) would improve security, maintainability, and code clarity.

## Current State Analysis

### 1. `AuthController` (Login Endpoint)
*   **Current Implementation**:
    *   Accepts a raw `JsonObject` from the request body.
    *   Manually extracts `username` and `password` strings using `getString()`.
    *   Constructs a new `JsonObject` to pass these credentials to the Event Bus (`users.authenticate`).
*   **Issues**:
    *   **Lack of Validation**: No dedicated validation logic (e.g., checking if fields are blank happens manually in the controller).
    *   **Security Risk**: `JsonObject.toString()` might inadvertently log the password if the object is logged.
    *   **Typing**: String literals ("username", "password") are scattered across files, leading to potential typos.

### 2. `UserVerticle` (Authentication Logic)
*   **Current Implementation**:
    *   Consumes a `JsonObject` from the Event Bus.
    *   Parses fields by key.
    *   Replies with a `JsonObject` containing `"username"`.
*   **Issues**:
    *   **Contract Ambiguity**: The message format is implicit. A clearer contract is needed for what the `users.authenticate` address expects.

### 3. `AuthVerticle` (Token Issuance)
*   **Current Implementation**:
    *   Receives a `JsonObject` with just `"username"` for `auth.token.issue`.
*   **Issues**:
    *   Less critical here, but standardizing the message payload (e.g., `TokenRequestDTO`) would be consistent.

---

## Proposed DTO Architecture

We recommend introducing the following DTOs in the `ziadatari.ReactiveAPI.dto` package.

### 1. `LoginRequestDTO`
Used to capture the initial login payload.

```java
public class LoginRequestDTO {
    private String username;
    private String password;

    public LoginRequestDTO(JsonObject json) {
        this.username = json.getString("username");
        this.password = json.getString("password");
    }

    public JsonObject toJson() {
        return new JsonObject()
            .put("username", username)
            .put("password", password);
    }

    // Getters

    // IMPORTANT: Override toString to REDACT password
    @Override
    public String toString() {
        return "LoginRequestDTO{username='" + username + "', password='[PROTECTED]'}";
    }
}
```

### 2. `UserContextDTO` (or `AuthResponseDTO`)
Used to pass authenticated user information back from `UserVerticle` and eventually to the client (excluding the password).

```java
public class UserContextDTO {
    private String username;
    private String role; // Future-proofing

    public UserContextDTO(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public JsonObject toJson() {
        return new JsonObject()
            .put("username", username)
            .put("role", role);
    }
}
```

## Implementation Plan

1.  **Create DTOs**: Create `LoginRequestDTO` in the `dto` package.
2.  **Refactor `AuthController`**:
    *   Parse the request body into `LoginRequestDTO`.
    *   Validate the DTO (e.g., `isValid()` method).
    *   Pass `dto.toJson()` to the Event Bus.
3.  **Refactor `UserVerticle`**:
    *   Deserialize the incoming message into `LoginRequestDTO`.
    *   Use the getters for authentication logic.
4.  **Benefits**:
    *   **Centralized Validation**: `LoginRequestDTO` can enforce rules (e.g., password length).
    *   **Safety**: `toString()` is safe to log.
    *   **Clarity**: The code explicitly states it's handling a login request, not just "some JSON".

## Conclusion
Adopting these DTOs aligns the authentication flow with the rest of the application (like `EmployeeDTO`), ensuring a consistent and secure architecture for handling sensitive data.
