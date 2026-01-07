package ziadatari.ReactiveAPI.exception;

public enum ErrorCode {

  // --- GENERAL SERVER ERRORS ---
  INTERNAL_SERVER_ERROR(500, "GEN_001", "An unexpected error occurred. Please contact support."),
  SERVICE_UNAVAILABLE(503, "GEN_002", "The service is currently unavailable."),

  // --- DATABASE ERRORS ---
  DB_CONNECTION_ERROR(503, "DB_001", "Unable to connect to the database."),
  DB_QUERY_ERROR(500, "DB_002", "Failed to execute database query."),

  // --- EMPLOYEE BUSINESS LOGIC ERRORS ---
  EMPLOYEE_NOT_FOUND(404, "EMP_001", "Employee with the provided ID was not found."),
  EMPLOYEE_ID_REQUIRED(400, "EMP_002", "Employee ID is required for this operation."),

  // --- VALIDATION ERRORS ---
  VALIDATION_ERROR(400, "VAL_000", "Validation failed."),
  MISSING_NAME(400, "VAL_001", "Employee name is required and cannot be empty."),
  MISSING_SALARY(400, "VAL_002", "Employee salary is required."),
  NEGATIVE_SALARY(400, "VAL_003", "Salary cannot be negative."),
  INVALID_DEPARTMENT(400, "VAL_004", "Department name is invalid."),
  DUPLICATE_EMPLOYEE(409, "EMP_003", "Active employee already exists with this name and department."),

  // --- INPUT / JSON ERRORS ---
  INVALID_JSON_FORMAT(400, "REQ_001", "Request body contains invalid JSON."),
  EMPTY_BODY(400, "REQ_002", "Request body cannot be empty.");

  private final int httpStatus;
  private final String code;
  private final String message;

  ErrorCode(int httpStatus, String code, String message) {
    this.httpStatus = httpStatus;
    this.code = code;
    this.message = message;
  }

  public int getHttpStatus() {
    return httpStatus;
  }

  public String getCode() {
    return code;
  }

  public String getMessage() {
    return message;
  }
}
