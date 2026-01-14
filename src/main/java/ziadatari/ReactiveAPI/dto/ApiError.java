package ziadatari.ReactiveAPI.dto;

import io.vertx.core.json.JsonObject;
import ziadatari.ReactiveAPI.exception.ErrorCode;

import java.time.Instant;

/**
 * Data Transfer Object representing an API error response.
 * Encapsulates error details like code, message, HTTP status, and timestamp.
 */
public class ApiError {

  /** The internal error code string (e.g., "GEN_001"). */
  private final String errorCode;
  /** A human-readable message describing the error. */
  private final String message;
  /** The HTTP status code associated with the error (e.g., 404, 500). */
  private final int status;
  /** The timestamp when the error occurred (ISO-8601 format). */
  private final String timestamp;

  /**
   * Constructs an ApiError using an ErrorCode enum.
   *
   * @param code the ErrorCode containing error details
   */
  public ApiError(ErrorCode code) {
    this.errorCode = code.getCode();
    this.message = code.getMessage();
    this.status = code.getHttpStatus();
    this.timestamp = Instant.now().toString();
  }

  /**
   * Constructs an ApiError using an ErrorCode enum and a custom message.
   *
   * @param code          the ErrorCode containing default error details
   * @param customMessage a specific error message to override the default
   */
  public ApiError(ErrorCode code, String customMessage) {
    this.errorCode = code.getCode();
    this.message = customMessage;
    this.status = code.getHttpStatus();
    this.timestamp = Instant.now().toString();
  }

  /**
   * Converts the ApiError object to a Vert.x JsonObject.
   *
   * @return a JsonObject representation of the error
   */
  public JsonObject toJson() {
    return new JsonObject()
        .put("success", false)
        .put("error_code", errorCode)
        .put("message", message)
        .put("status", status)
        .put("timestamp", timestamp);
  }
}
