package ziadatari.ReactiveAPI.exception;

import io.vertx.core.json.DecodeException;

import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.dto.ApiError;

public class GlobalErrorHandler {

  public static void handle(RoutingContext ctx, Throwable cause) {

    // Case 1: Business logic
    if (cause instanceof ServiceException) {
      ServiceException se = (ServiceException) cause;
      reply(ctx, se.getErrorCode());
    }

    // Case 2: Bad JSON
    else if (cause instanceof DecodeException) {
      reply(ctx, ErrorCode.INVALID_JSON_FORMAT);
    }

    // Case 3: Unknown system failure
    else {
      System.err.println("CRITICAL FAILURE:" + cause.getMessage());
      cause.printStackTrace();
      reply(ctx, ErrorCode.INTERNAL_SERVER_ERROR);
    }

  }

  private static void reply(RoutingContext ctx, ErrorCode code) {
    ApiError response = new ApiError(code);

    ctx.response()
        .setStatusCode(code.getHttpStatus())
        .putHeader("content-type", "application/json")
        .end(response.toJson().encode());
  }
}
