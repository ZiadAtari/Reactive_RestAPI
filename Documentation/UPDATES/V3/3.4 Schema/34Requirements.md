3.4 Requirements

JSON Schema which defines the structure of the request messages for the API.
Allows for early detection of potential issues in the call and fails fast to conserve resources.
Should be fully compatible with current error and exception handling.

## 1. Objectives
- Implement **JSON Schema Validation** for all incoming request bodies (`POST`, `PUT`).
- Ensure validation occurs **before** business logic execution.
- Integrate with `GlobalErrorHandler` to return standardized `ApiError` responses.

## 2. Technical Specifications

### 2.1 Dependencies
- Add `vertx-web-validation` or `vertx-json-schema` to `pom.xml`.

### 2.2 Schema Definitions

#### A. Login Request Schema (`POST /login`)
- **Required Fields**:
  - `username` (String, minLength: 1)
  - `password` (String, minLength: 1)
- **Validation Error Code**: `VAL_005` (Credentials Required) or `REQ_001` (Invalid JSON)

#### B. Employee Request Schema (`POST /employees`)
- **Required Fields**:
  - `name` (String, minLength: 1)
  - `department` (String, minLength: 1)
  - `salary` (Number, minimum: 0)
- **Optional Fields**:
  - `active` (Boolean)
- **Validation Error Code**: `VAL_000` (Validation Error) with specific details.

### 2.3 Implementation Details

#### `SchemaValidator` Class
- **Location**: `ziadatari.ReactiveAPI.util`
- **Responsibility**: 
  - Load schemas on startup.
  - Provide `validate(InternalSchema, JsonObject)` method.
  - Throw `ServiceException` on failure.

#### Integration Points
- `AuthController.java`: Validate `LoginRequestDTO` JSON.
- `EmployeeController.java`: Validate `EmployeeDTO` JSON.

## 3. Error Handling
- Invalid JSON Structure -> `GlobalErrorHandler` catches `DecodeException` -> `REQ_001`.
- Schema Violation -> `SchemaValidator` throws `ServiceException(VAL_000)` -> `GlobalErrorHandler` returns `400 Bad Request`.
