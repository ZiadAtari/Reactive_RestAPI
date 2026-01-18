package ziadatari.ReactiveAPI.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;
import ziadatari.ReactiveAPI.util.PemUtils;

/**
 * Verticle responsible for JWT authentication tasks.
 * It manages the RS256 token service and processes token requests via the Event
 * Bus.
 */
public class AuthVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(AuthVerticle.class);
    private TokenService tokenService;

    @Override
    public void start(Promise<Void> startPromise) {
        // Retrieve RSA Private Key from config (passed from MainVerticle)
        String privateKey = config().getString("rsa_private_key");

        if (privateKey == null || privateKey.isBlank()) {
            logger.error("Failed to start AuthVerticle: rsa_private_key is missing in configuration.");
            startPromise.fail(new ServiceException(ErrorCode.AUTH_SETUP_ERROR, "RSA Private Key missing"));
            return;
        }

        // Normalize key to handle escaped newlines or one-liner formats from env vars
        privateKey = PemUtils.normalizePem(privateKey, "-----BEGIN PRIVATE KEY-----", "-----END PRIVATE KEY-----");

        try {
            JWTAuthOptions jwtOptions = new JWTAuthOptions()
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm("RS256")
                            .setBuffer(privateKey));

            JWTAuth jwtAuth = JWTAuth.create(vertx, jwtOptions);
            this.tokenService = new Rs256TokenService(jwtAuth);

            // Register Event Bus consumers
            vertx.eventBus().consumer("auth.token.get", this::handleTokenRequest);
            vertx.eventBus().consumer("auth.token.issue", this::handleTokenIssue);

            logger.info("AuthVerticle deployed successfully.");
            startPromise.complete();

        } catch (Exception e) {
            logger.error("Failed to initialize AuthVerticle", e);
            startPromise.fail(e);
        }
    }

    /**
     * Handles 'auth.token.get' requests from the Event Bus.
     * <p>
     * This handler retrieves a "Service Token" used for machine-to-machine
     * communication
     * (e.g., verifying IPs with the Demo API).
     * The token is cached by the {@link TokenService} to minimize overhead.
     * </p>
     *
     * @param message the Event Bus message (empty body expected)
     */
    private void handleTokenRequest(Message<Object> message) {
        tokenService.getToken()
                .onSuccess(token -> {
                    message.reply(token);
                })
                .onFailure(err -> {
                    logger.error("Failed to generate token for request", err);
                    if (err instanceof ServiceException) {
                        message.fail(((ServiceException) err).getErrorCode().ordinal(), err.getMessage());
                    } else {
                        message.fail(ErrorCode.INTERNAL_SERVER_ERROR.ordinal(), err.getMessage());
                    }
                });
    }

    /**
     * Handles 'auth.token.issue' requests from the Event Bus.
     * <p>
     * This handler generates a "User Token" for an authenticated user.
     * The token includes the username as the subject and a default "user" role.
     * Unlike service tokens, these are generated on-demand and valid for a shorter
     * duration.
     * </p>
     *
     * @param message the Event Bus message containing a JSON body with "username"
     */
    private void handleTokenIssue(Message<Object> message) {
        JsonObject body = (JsonObject) message.body();
        String username = body.getString("username");

        tokenService.generateUserToken(username)
                .onSuccess(token -> {
                    message.reply(token);
                })
                .onFailure(err -> {
                    logger.error("Failed to issue token for user: " + username, err);
                    if (err instanceof ServiceException) {
                        message.fail(((ServiceException) err).getErrorCode().getHttpStatus(), err.getMessage());
                    } else {
                        message.fail(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus(), err.getMessage());
                    }
                });
    }
}
