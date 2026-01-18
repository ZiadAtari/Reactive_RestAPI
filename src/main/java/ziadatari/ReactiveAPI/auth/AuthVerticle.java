package ziadatari.ReactiveAPI.auth;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;

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
        privateKey = normalizePem(privateKey);

        try {
            JWTAuthOptions jwtOptions = new JWTAuthOptions()
                    .addPubSecKey(new PubSecKeyOptions()
                            .setAlgorithm("RS256")
                            .setBuffer(privateKey));

            JWTAuth jwtAuth = JWTAuth.create(vertx, jwtOptions);
            this.tokenService = new Rs256TokenService(jwtAuth);

            // Register Event Bus consumer
            vertx.eventBus().consumer("auth.token.get", this::handleTokenRequest);

            logger.info("AuthVerticle deployed successfully.");
            startPromise.complete();

        } catch (Exception e) {
            logger.error("Failed to initialize AuthVerticle", e);
            startPromise.fail(e);
        }
    }

    /**
     * Normalizes the PEM string by handling escaped newlines and ensuring proper
     * PEM headers/footers with internal newlines.
     */
    private String normalizePem(String pem) {
        if (pem == null)
            return null;

        // 1. Convert escaped newlines to actual newlines
        pem = pem.replace("\\n", "\n").replace("\\r", "\r");

        // 2. If it's a one-liner but contains headers, we must ensure headers/footers
        // are on their own lines as required by many PEM parsers.
        String header = "-----BEGIN PRIVATE KEY-----";
        String footer = "-----END PRIVATE KEY-----";

        if (pem.contains(header) && pem.contains(footer)) {
            // Clean up any double headers if they exist
            int startIdx = pem.indexOf(header);
            String content = pem.substring(startIdx + header.length(), pem.indexOf(footer)).trim();

            // Remove any existing logical newlines to re-wrap or just ensure basic
            // structure
            content = content.replaceAll("\\s+", "");

            // Reconstruct with guaranteed newlines
            return header + "\n" + content + "\n" + footer;
        }

        return pem;
    }

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
}
