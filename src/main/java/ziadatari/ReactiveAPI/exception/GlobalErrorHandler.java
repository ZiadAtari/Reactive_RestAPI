package ziadatari.ReactiveAPI.exception;

import io.vertx.core.json.DecodeException;

import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.dto.ApiError;

/**
 * Centralized error handling utility.
 * Maps different types of exceptions to specific ErrorCodes and sends
 * consistent JSON error responses.
 */
public class GlobalErrorHandler {

  /**
   * Routes an exception to the appropriate handling logic.
   *
   * @param ctx   the current routing context
   * @param cause the exception/error that occurred
   */
  public static void handle(RoutingContext ctx, Throwable cause) {

    // Case 1: Custom application exceptions (Business Logic failures)
    if (cause instanceof ServiceException) {
      ServiceException se = (ServiceException) cause;
      reply(ctx, se.getErrorCode());
    }

    // Case 2: JSON parsing failures
    else if (cause instanceof DecodeException) {
      reply(ctx, ErrorCode.INVALID_JSON_FORMAT);
    }

    // Case 3: Unexpected system-level failures
    else {
      System.err.println("CRITICAL FAILURE: " + cause.getMessage());
      cause.printStackTrace();
      reply(ctx, ErrorCode.INTERNAL_SERVER_ERROR);
    }

  }

  /**
   * Helper method to send the error response to the client.
   *
   * @param ctx  the current routing context
   * @param code the ErrorCode defining the response details
   */
  private static void reply(RoutingContext ctx, ErrorCode code) {
    ApiError response = new ApiError(code);

    ctx.response()
        .setStatusCode(code.getHttpStatus())
        .putHeader("content-type", "application/json")
        .end(response.toJson().encode());
  }
}
