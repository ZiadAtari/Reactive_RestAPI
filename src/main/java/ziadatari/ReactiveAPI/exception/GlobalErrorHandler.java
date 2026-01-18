package ziadatari.ReactiveAPI.exception;

import io.vertx.core.json.DecodeException;

import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.dto.ApiError;

/**
 * Centralized error handling utility.
 * Maps different types of exceptions to specific ErrorCodes and sends
 * consistent JSON error responses.
 */
public class GlobalErrorHandler {

  private static final Logger logger = LoggerFactory.getLogger(GlobalErrorHandler.class);

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
      reply(ctx, se.getErrorCode(), se.getMessage());
    }

    // Case 2: JSON parsing failures
    else if (cause instanceof DecodeException) {
      reply(ctx, ErrorCode.INVALID_JSON_FORMAT);
    }

    // Case 3: Unexpected system-level failures
    else {
      logger.error("CRITICAL FAILURE", cause);
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
    reply(ctx, code, code.getMessage());
  }

  /**
   * Helper method to send the error response to the client with a custom message.
   *
   * @param ctx           the current routing context
   * @param code          the ErrorCode defining the response details
   * @param customMessage a specific error message to override the default
   */
  private static void reply(RoutingContext ctx, ErrorCode code, String customMessage) {
    ApiError response = new ApiError(code, customMessage);

    ctx.response()
        .setStatusCode(code.getHttpStatus())
        .putHeader("content-type", "application/json")
        .end(response.toJson().encode());
  }
}
