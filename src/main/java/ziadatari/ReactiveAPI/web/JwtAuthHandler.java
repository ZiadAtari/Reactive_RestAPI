package ziadatari.ReactiveAPI.web;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.exception.ServiceException;
import ziadatari.ReactiveAPI.util.PemUtils;

/**
 * Middleware handler for verifying JWT Bearer tokens on protected routes.
 * Using local RSA Public Key validation.
 */
public class JwtAuthHandler implements Handler<RoutingContext> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthHandler.class);
    private final JWTAuth jwtAuth;

    /**
     * Constructs a new JwtAuthHandler.
     * <p>
     * Initializes the JWT authentication provider using the RSA Public Key from
     * configuration.
     * This key is used to verify signatures of incoming tokens.
     * </p>
     *
     * @param vertx  the Vert.x instance
     * @param config the configuration JSON containing "rsa_public_key"
     * @throws IllegalStateException if the RSA public key is missing from
     *                               configuration
     */
    public JwtAuthHandler(Vertx vertx, JsonObject config) {
        String publicKey = config.getString("rsa_public_key");
        if (publicKey == null) {
            throw new IllegalStateException("RSA Public Key not configured");
        }

        // Normalize public key to ensure it has correct headers and line breaks
        // This handles cases where the key is passed as a single line env var
        publicKey = PemUtils.normalizePem(publicKey, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");

        // Configure JWT options with the RS256 algorithm and the public key buffer
        JWTAuthOptions options = new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("RS256")
                        .setBuffer(publicKey));

        this.jwtAuth = JWTAuth.create(vertx, options);
    }

    /**
     * Handles the HTTP request by intercepting and validating the Authorization
     * header.
     * <p>
     * Flow:
     * 1. Check for presence of "Authorization" header.
     * 2. Ensure it follows the "Bearer <token>" schema.
     * 3. Authenticate the token using the configured {@link JWTAuth} provider.
     * 4. If valid, proceed to the next handler.
     * 5. If invalid/expired, terminate request with appropriate 401 error.
     * </p>
     *
     * @param ctx the routing context of the request
     */
    @Override
    public void handle(RoutingContext ctx) {
        String authHeader = ctx.request().getHeader("Authorization");
        logger.debug("JwtAuthHandler hit for path: {}. Auth Header: {}", ctx.request().path(),
                (authHeader != null ? "Present" : "Missing"));

        // 1. Validation: Check Header Presence and Format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Request missing Bearer token for protected route: {}", ctx.request().path());
            GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.TOKEN_MISSING));
            return;
        }

        // 2. Extraction: Isolate the token string
        String token = authHeader.substring(7);

        // 3. Authentication: Verify signature and claims
        jwtAuth.authenticate(new JsonObject().put("token", token))
                .onSuccess(user -> {
                    // Token is valid; proceed to the protected controller
                    ctx.next();
                })
                .onFailure(err -> {
                    logger.warn("JWT Verification failed: {}", err.getMessage());
                    // 4. Error Mapping: Distinguish between expired and malformed tokens
                    ErrorCode errorCode = err.getMessage().contains("Expired") ? ErrorCode.TOKEN_EXPIRED
                            : ErrorCode.TOKEN_INVALID;
                    GlobalErrorHandler.handle(ctx, new ServiceException(errorCode));
                });
    }
}
