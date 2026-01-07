package ziadatari.ReactiveAPI.dto;

import io.vertx.core.json.JsonObject;
import ziadatari.ReactiveAPI.exception.ErrorCode;

import java.time.Instant;

public class ApiError {

  private final String errorCode;
  private final String message;
  private final int status;
  private final String timestamp;

  public ApiError(ErrorCode code) {
    this.errorCode = code.getCode();
    this.message = code.getMessage();
    this.status = code.getHttpStatus();
    this.timestamp = Instant.now().toString();
  }

  // Construcor for messages
  public ApiError(ErrorCode code, String customMessage) {
    this.errorCode = code.getCode();
    this.message = customMessage;
    this.status = code.getHttpStatus();
    this.timestamp = Instant.now().toString();
  }

  // Convert vert.x jsonobject
  public JsonObject toJson() {
    return new JsonObject()
      .put("success", false)
      .put("error_code", errorCode)
      .put("message", message)
      .put("status", status)
      .put("timestamp", timestamp);
  }
}
