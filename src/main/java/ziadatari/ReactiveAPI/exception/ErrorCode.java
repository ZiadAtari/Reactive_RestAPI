package ziadatari.ReactiveAPI.exception;

/**
 * Enumeration of system-wide error codes.
 * Each constant defines an HTTP status code, a unique internal error code,
 * and a human-readable default message.
 */
public enum ErrorCode {

  // --- GENERAL SERVER ERRORS ---
  /** Unexpected internal server failure. */
  INTERNAL_SERVER_ERROR(500, "GEN_001", "An unexpected error occurred. Please contact support."),
  /** Service is temporarily unavailable (e.g., maintenance). */
  SERVICE_UNAVAILABLE(503, "GEN_002", "The service is currently unavailable."),

  // --- DATABASE ERRORS ---
  /** Failure to establish a connection with MySQL. */
  DB_CONNECTION_ERROR(503, "DB_001", "Unable to connect to the database."),
  /** Failure during the execution of a SQL query. */
  DB_QUERY_ERROR(500, "DB_002", "Failed to execute database query."),

  // --- EMPLOYEE BUSINESS LOGIC ERRORS ---
  /** Requested employee record does not exist. */
  EMPLOYEE_NOT_FOUND(404, "EMP_001", "Employee with the provided ID was not found."),
  /** Operation requires an ID that was not provided. */
  EMPLOYEE_ID_REQUIRED(400, "EMP_002", "Employee ID is required for this operation."),

  // --- VALIDATION ERRORS ---
  /** Generic validation failure. */
  VALIDATION_ERROR(400, "VAL_000", "Validation failed."),
  /** Required name field is missing or empty. */
  MISSING_NAME(400, "VAL_001", "Employee name is required and cannot be empty."),
  /** Required salary field is missing. */
  MISSING_SALARY(400, "VAL_002", "Employee salary is required."),
  /** Salary value is negative. */
  NEGATIVE_SALARY(400, "VAL_003", "Salary cannot be negative."),
  /** Department name does not meet validation criteria. */
  INVALID_DEPARTMENT(400, "VAL_004", "Department name is invalid."),
  /** Attempt to create an employee that already exists. */
  DUPLICATE_EMPLOYEE(409, "EMP_003", "Active employee already exists with this name and department."),

  // --- INPUT / JSON ERRORS ---
  /** Request body is not valid JSON. */
  INVALID_JSON_FORMAT(400, "REQ_001", "Request body contains invalid JSON."),
  /** Request body is empty when a payload is expected. */
  EMPTY_BODY(400, "REQ_002", "Request body cannot be empty."),
  /** Rate limit exceeded for the client IP. */
  TOO_MANY_REQUESTS(429, "REQ_003", "Too many requests. Please try again later."),

  // --- SECURITY / VERIFICATION ERRORS ---
  /** Client IP is not authorized to access the resource. */
  IP_VERIFICATION_FAILED(403, "SEC_001", "Client IP verification failed."),
  /** Authentication service initialization failure. */
  AUTH_SETUP_ERROR(500, "SEC_002", "Failed to initialize authentication service. Please check RSA key configuration."),
  /** Invalid credentials provided. */
  INVALID_CREDENTIALS(401, "SEC_003", "Invalid username or password."),
  /** Required credentials are missing. */
  CREDENTIALS_REQUIRED(400, "VAL_005", "Username and password are required."),
  /** Authentication token is missing. */
  TOKEN_MISSING(401, "SEC_004", "Authentication token is missing."),
  /** Authentication token is invalid or malformed. */
  TOKEN_INVALID(401, "SEC_005", "Authentication token is invalid or malformed."),
  /** Authentication token has expired. */
  TOKEN_EXPIRED(401, "SEC_006", "Authentication token has expired.");

  /** The HTTP status code to be returned to the client (e.g., 404). */
  private final int httpStatus;
  /**
   * A unique internal code for easier debugging and tracking (e.g., "EMP_001").
   */
  private final String code;
  /** A default, human-readable error message. */
  private final String message;

  /**
   * Internal constructor for ErrorCode.
   *
   * @param httpStatus the HTTP status code to return to the client
   * @param code       the unique internal string code for this error
   * @param message    the default message describing the error
   */
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
