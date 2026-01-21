# Implementation Plan - Update 3.3 (DTO Architecture)

## Goal Description
Implement a secure and strongly-typed Data Transfer Object (DTO) architecture for the authentication flow. This will replace the current usage of raw `JsonObject` for sensitive data, ensuring that credentials are validated, strictly typed, and safely handled (redacted in logs).

## User Review Required
> [!IMPORTANT]
> This refactor changes the internal data handling of the `login` flow. While the external API contract (JSON body) remains the same, the internal event bus messages will now rely on DTO serialization.

## Proposed Changes

### 1. DTO Package
Create new DTO classes to encapsulate authentication data.

#### [NEW] [LoginRequestDTO.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/dto/LoginRequestDTO.java)
- **Purpose**: Encapsulates `username` and `password`.
- **Key Features**:
    - Constructor accepting `JsonObject`.
    - `isValid()` method for centralized validation.
    - `toString()` override to redact password.

#### [NEW] [UserContextDTO.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/dto/UserContextDTO.java)
- **Purpose**: Encapsulates authenticated user info for responses.
- **Key Features**:
    - Fields for `username` and `role`.
    - `toJson()` method for Event Bus replies.

### 2. Web Layer
Refactor the controller to use the new DTO for validation and passing data.

#### [MODIFY] [AuthController.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/web/AuthController.java)
- **Changes**:
    - Remove manual `getString` calls.
    - Instantiate `LoginRequestDTO` from request body.
    - Call `dto.isValid()` before processing.
    - Pass `dto.toJson()` to the Event Bus.

### 3. Repository Layer
Update the worker verticle to consume the secure DTO structure.

#### [MODIFY] [UserVerticle.java](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/repository/UserVerticle.java)
- **Changes**:
    - Deserialize incoming Event Bus message to `LoginRequestDTO`.
    - Use DTO getters for authentication.
    - Construct reply using `UserContextDTO` instead of raw JSON.

## Verification Plan

### Automated Tests
*   We will rely on existing integration tests (if any) or manual verification since unit tests are not currently in scope for this task.

### Manual Verification
1.  **Valid Login**: Send `POST /login` with correct credentials. Verify 200 OK and Token.
2.  **Invalid Login**: Send `POST /login` with wrong password. Verify 401 Unauthorized.
3.  **Missing Fields**: Send `POST /login` with missing username/password. Verify 400 Bad Request (triggered by `isValid()`).
4.  **Security Check**: Temporarily add a log statement `logger.info("Request: " + dto)` in `AuthController` and verify in the console that the password is printed as `[PROTECTED]`.
