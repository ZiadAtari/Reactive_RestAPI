# 3.4 Schema Validation Plan

## 1. Introduction
This document leads the implementation of JSON Schema validation for the ReactiveAPI. The goal is to enforce strict data contracts for incoming requests (`POST /login` and `POST /employees`), enabling the system to fail fast on invalid input before reaching business logic.

## 2. Architecture & Design

### 2.1 Dependencies
We will leverage **Vert.x Web Validation** (or `vertx-json-schema`) to handle schema compilation and validation.
- **Library**: `io.vertx:vertx-json-schema`

### 2.2 Component Design
We will introduce a new utility class to centralize schema logic.

#### `ziadatari.ReactiveAPI.util.SchemaValidator`
- **Purpose**: Singleton/Static utility to load schemas and validate objects.
- **Responsibilities**:
    - Define JSON Schemas programmatically.
    - Provide public validation methods: `validateLogin(JsonObject)` and `validateEmployee(JsonObject)`.
    - Map validation failures to the system's `ServiceException`.

### 2.3 Detailed Design & Interactions

#### A. New Class: `SchemaValidator.java`
```java
public class SchemaValidator {
  
  // Define Schemas as JsonObjects
  // ... LOGIN_SCHEMA (as above) ...

  // Single Employee Schema (Items)
  private static final JsonObject EMPLOYEE_SCHEMA = new JsonObject()
    .put("type", "object")
    .put("properties", new JsonObject()
      .put("name", new JsonObject().put("type", "string").put("minLength", 1))
      .put("department", new JsonObject().put("type", "string").put("minLength", 1))
      .put("salary", new JsonObject().put("type", "number").put("minimum", 0))
      .put("active", new JsonObject().put("type", "boolean")))
    .put("required", new JsonArray().add("name").add("department").add("salary"));

  // Batch Employee Schema (Array) - NEW for Batch Support
  private static final JsonObject EMPLOYEE_BATCH_SCHEMA = new JsonObject()
    .put("type", "array")
    .put("items", EMPLOYEE_SCHEMA)
    .put("minItems", 1)
    .put("maxItems", 100); // Limit batch size for safety

  // Validators
  private static final Validator loginValidator = Validator.create(..., LOGIN_SCHEMA);
  private static final Validator employeeValidator = Validator.create(..., EMPLOYEE_SCHEMA);
  private static final Validator employeeBatchValidator = Validator.create(..., EMPLOYEE_BATCH_SCHEMA);

  public static void validateLogin(JsonObject body) { ... }

  public static void validateEmployee(JsonObject body) {
    OutputUnit result = employeeValidator.validate(body);
    if (!result.getValid()) throw new ServiceException(ErrorCode.VALIDATION_ERROR, ...);
  }

  // NEW: Validate Array of Employees
  public static void validateEmployeeBatch(JsonArray body) {
    OutputUnit result = employeeBatchValidator.validate(body);
    if (!result.getValid()) {
      // Map schema error to specific ErrorCode if possible
      throw mapError(result.getError());
    }
  }

  // Helper to Preserve Granular Error Codes
  private static ServiceException mapError(SchemaError error) {
      String msg = error.getMessage(); // e.g., "required property 'name' not found"
      
      if (msg.contains("name")) return new ServiceException(ErrorCode.MISSING_NAME);
      if (msg.contains("salary") && msg.contains("minimum")) return new ServiceException(ErrorCode.NEGATIVE_SALARY);
      if (msg.contains("salary")) return new ServiceException(ErrorCode.MISSING_SALARY);
      if (msg.contains("department")) return new ServiceException(ErrorCode.INVALID_DEPARTMENT);
      
      // Fallback for generic structure errors
      return new ServiceException(ErrorCode.VALIDATION_ERROR, "Schema Violation: " + msg);
  }
}
```
*Note: The actual implementation will use the correct Vert.x JSON Schema API.*

#### B. Interaction in `AuthController.java`
(Unchanged)

#### C. Interaction in `EmployeeController.java`
**Current**:
```java
JsonObject body = ctx.body().asJsonObject();
// ...
```
**New (with Batch Support)**:
```java
Object body = ctx.body().asJson(); // Get as generic Object (or handle Parse checks)

if (body instanceof JsonArray) {
   // 1. Batch Validation
   JsonArray batch = (JsonArray) body;
   SchemaValidator.validateEmployeeBatch(batch);
   
   // 2. Batch Processing (Service Layer needs update)
   List<EmployeeDTO> dtos = batch.stream().map(o -> Builder.build((JsonObject)o)).collect(Collectors.toList());
   service.createBatch(dtos).onSuccess(...);

} else if (body instanceof JsonObject) {
   // 1. Single Validation
   JsonObject json = (JsonObject) body;
   SchemaValidator.validateEmployee(json);

   // 2. Single Processing
   // ... existing builder logic ...
} else {
   throw new ServiceException(ErrorCode.INVALID_JSON_FORMAT);
}
```

## 3. Implementation Steps

### Step 1: Dependency Management
- Update `pom.xml` to include `vertx-json-schema`.

### Step 2: Create Schema Validator
- Implement `SchemaValidator.java` with `LOGIN`, `EMPLOYEE`, and `EMPLOYEE_BATCH` schemas.

### Step 3: Service Layer Updates (New)
- Update `EmployeeService.java` to include `createBatch(List<EmployeeDTO>)`.
- Use `CompositeFuture` to orchestrate multiple creations or `executeBatch` if optimizing logic.

### Step 4: Integrate with Controllers
- Modify `EmployeeController.java` to detect `JsonArray` vs `JsonObject` and route accordingly.

### Step 5: Verification
- Manual testing via `curl` to verify error responses for invalid payloads.
    - `curl -X POST /login -d '{}'` -> Expect 400
    - `curl -X POST /employees -d '{"name": "John", "salary": -100}'` -> Expect 400
    - `curl -X POST /employees -d '[{"name": "A", "salary": 100}, {"name": "B"}]'` -> Expect 400 (B missing fields)

## 4. Rollback Plan
Revert changes if library issues occur.
