package ziadatari.ReactiveAPI.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.exception.ServiceException;

/**
 * Controller for authentication-related endpoints (Login).
 */
public class AuthController {

    private final Vertx vertx;

    public AuthController(Vertx vertx) {
        this.vertx = vertx;
    }

    /**
     * Handles user login (POST /login).
     * 1. Validates input.
     * 2. Authenticates user (Event Bus: users.authenticate).
     * 3. Generates Token (Event Bus: auth.token.issue).
     */
    public void login(RoutingContext ctx) {
        JsonObject body = ctx.body().asJsonObject();
        if (body == null || body.isEmpty()) {
            GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.EMPTY_BODY));
            return;
        }

        String username = body.getString("username");
        String password = body.getString("password");

        if (username == null || password == null) {
            GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.CREDENTIALS_REQUIRED));
            return;
        }

        // Step 1: Authenticate User
        vertx.eventBus().request("users.authenticate",
                new JsonObject().put("username", username).put("password", password), authReply -> {
                    if (authReply.succeeded()) {
                        // Step 2: Issue Token
                        vertx.eventBus().request("auth.token.issue", new JsonObject().put("username", username),
                                tokenReply -> {
                                    if (tokenReply.succeeded()) {
                                        String token = (String) tokenReply.result().body();
                                        ctx.response()
                                                .putHeader("content-type", "application/json")
                                                .end(new JsonObject().put("token", token).encode());
                                    } else {
                                        GlobalErrorHandler.handle(ctx, tokenReply.cause());
                                    }
                                });
                    } else {
                        GlobalErrorHandler.handle(ctx, authReply.cause());
                    }
                });
    }
}
