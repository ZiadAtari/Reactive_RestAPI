package ziadatari.ReactiveAPI.web;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.dto.LoginRequestDTO;
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
     * Handles user login requests (POST /login).
     * <p>
     * <b>Orchestration Flow:</b>
     * 1. Validates schema (username/password presence) via
     * {@link ziadatari.ReactiveAPI.util.SchemaValidator}.
     * 2. Sends a message to {@code users.authenticate} (UserVerticle) to verify
     * credentials.
     * 3. Upon success, sends a message to {@code auth.token.issue} (AuthVerticle)
     * to generate a valid JWT.
     * 4. Returns the JWT to the client.
     * </p>
     *
     * @param ctx the routing context
     */
    public void login(RoutingContext ctx) {
        // Step 1: Parse and Validate Request
        try {
            JsonObject body = ctx.body().asJsonObject();
            // Schema Validation (Fails fast)
            ziadatari.ReactiveAPI.util.SchemaValidator.validateLogin(body);

            LoginRequestDTO loginRequest = new LoginRequestDTO(body);

            // Step 2: Authenticate User
            vertx.eventBus().request("users.authenticate", loginRequest.toJson(), authReply -> {
                if (authReply.succeeded()) {
                    // Step 3: Issue Token
                    vertx.eventBus().request("auth.token.issue",
                            new JsonObject().put("username", loginRequest.getUsername()),
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
        } catch (Exception e) {
            GlobalErrorHandler.handle(ctx, e);
        }
    }
}
