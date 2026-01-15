# Exceptions System

The Exceptions system provides a centralized, uniform way to handle errors across all layers and return consistent JSON responses to the client.

## Components

### [GlobalErrorHandler](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/exception/GlobalErrorHandler.java)
- **Purpose**: Static utility for mapping exceptions to HTTP responses.
- **Handling Strategy**:
    - **ServiceException**: Maps custom business errors (e.g., `EMPLOYEE_NOT_FOUND`) to their defined `ErrorCode`.
    - **DecodeException**: Handles JSON parsing failures (`REQ_001`).
    - **Generic Exceptions**: Catches unexpected crashes (`GEN_001`) and returns 500 Internal Server Error.
- **Response Mechanism**: Uses the `ApiError` DTO to ensure a consistent JSON structure, including a `success: false` flag and the internal code.

### [ErrorCode](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/exception/ErrorCode.java)
- **Purpose**: A centralized registry of all possible system errors.
- **Fields per Entry**:
    - `httpStatus`: Standard HTTP code (e.g., 404, 429).
    - `code`: Unique internal identifier (e.g., `EMP_001`, `SEC_001`) for debugging.
    - `message`: Default human-readable description.

### [ServiceException](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/exception/ServiceException.java)
- **Purpose**: Custom `RuntimeException` used throughout the service and repository layers.
- **Role**: Allows logic to "fail fast" with a specific `ErrorCode`, which the web layer can then easily translate for the client.

## Standardized Error Response ([ApiError](file:///c:/Users/zatari/Desktop/Projects/Reactive_RestAPI/src/main/java/ziadatari/ReactiveAPI/dto/ApiError.java))

All errors follow this unified JSON format:

```json
{
  "code": "EMP_001",
  "message": "Employee with the provided ID was not found.",
  "timestamp": "2026-01-15T14:45:00Z"
}
```

## Error Propagation Flow
1. **Source**: An error occurs in the Repository, Service, or Handler.
2. **Signal**: A `ServiceException` (or other) is thrown or a `Future` is failed.
3. **Capture**: 
    - The `Controller` catches the failure from the Event Bus and maps it back to a `ServiceException`.
    - Vert.x Web `failureHandler` or explicit try-catch in handlers calls `GlobalErrorHandler.handle()`.
4. **Resolution**: `GlobalErrorHandler` serializes the `ApiError` and sends it to the client with the correct HTTP status.
