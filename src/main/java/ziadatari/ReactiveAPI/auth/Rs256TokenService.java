package ziadatari.ReactiveAPI.auth;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.ServiceException;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for generating and caching RS256 JWT tokens.
 */
public class Rs256TokenService implements TokenService {

    private static final Logger logger = LoggerFactory.getLogger(Rs256TokenService.class);
    private final JWTAuth jwtAuth;
    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final AtomicLong expirationTime = new AtomicLong(0);
    private final String initErrorMessage;

    // Refresh 5min b4 expiry
    private static final long REFRESH_BUFFER_SECONDS = 300;

    public Rs256TokenService(JWTAuth jwtAuth) {
        this.jwtAuth = jwtAuth;
        this.initErrorMessage = null;
    }

    public Rs256TokenService(String initErrorMessage) {
        this.jwtAuth = null;
        this.initErrorMessage = initErrorMessage;
    }

    /**
     * Retrieves a valid service token, utilizing caching to reduce signing
     * overhead.
     * <p>
     * Logic:
     * 1. Check if a cached token exists.
     * 2. Ensure the token is not within the "Refresh Buffer" (e.g., 5 minutes
     * before expiry).
     * 3. If valid, return cached token immediately.
     * 4. If expired or missing, generate a new one.
     * </p>
     *
     * @return a Future containing the JWT string
     */
    @Override
    public Future<String> getToken() {
        if (initErrorMessage != null) {
            return Future.failedFuture(new ServiceException(ErrorCode.AUTH_SETUP_ERROR,
                    "Authentication service failed to initialize: " + initErrorMessage));
        }

        long now = Instant.now().getEpochSecond();
        String currentToken = cachedToken.get();
        long currentExp = expirationTime.get();

        // High-performance read: Return cached token if it's still valid (with buffer)
        if (currentToken != null && now < (currentExp - REFRESH_BUFFER_SECONDS)) {
            return Future.succeededFuture(currentToken);
        }

        // Token is missing or near expiration, generate a new one
        return generateNewToken(now);
    }

    /**
     * Generates a new Service Token signed with the private key.
     *
     * @param now current epoch time in seconds
     * @return a Future containing the new token
     */
    private Future<String> generateNewToken(long now) {
        // Expiration: 1 hour (3600 seconds) from now
        int expiresInSeconds = 3600;

        JWTOptions options = new JWTOptions()
                .setAlgorithm("RS256")
                .setExpiresInSeconds(expiresInSeconds)
                .setIgnoreExpiration(false);

        // Subject identifies the client application itself
        JsonObject claims = new JsonObject()
                .put("sub", "ReactiveAPI-Client");

        try {
            String newToken = jwtAuth.generateToken(claims, options);

            // Update cache atomically
            cachedToken.set(newToken);
            expirationTime.set(now + expiresInSeconds);

            logger.info("Generated new JWT Token. Expires in {} seconds", expiresInSeconds);

            return Future.succeededFuture(newToken);
        } catch (Exception e) {
            logger.error("Failed to generate JWT token", e);
            return Future.failedFuture(new ServiceException(ErrorCode.AUTH_SETUP_ERROR,
                    "Token generation failed: " + e.getMessage()));
        }
    }

    /**
     * Generates a unique User Token for an authenticated user.
     * <p>
     * These tokens are:
     * - Short-lived (15 minutes) for security.
     * - Include the user's username as the 'sub' claim.
     * - Include a 'role' claim for authorization (currently static "user").
     * - NOT cached, as they are specific to a user session.
     * </p>
     *
     * @param username the authenticated username
     * @return a Future containing the user JWT
     */
    @Override
    public Future<String> generateUserToken(String username) {
        if (initErrorMessage != null) {
            return Future.failedFuture(new ServiceException(ErrorCode.AUTH_SETUP_ERROR,
                    "Authentication service failed to initialize: " + initErrorMessage));
        }

        // Token valid for 15 minutes
        int expiresInSeconds = 900;

        JWTOptions options = new JWTOptions()
                .setAlgorithm("RS256")
                .setExpiresInSeconds(expiresInSeconds)
                .setIgnoreExpiration(false);

        JsonObject claims = new JsonObject()
                .put("sub", username)
                .put("role", "user"); // Simple role model for now

        try {
            String newToken = jwtAuth.generateToken(claims, options);
            logger.info("Generated User JWT for '{}'. Expires in {} seconds", username, expiresInSeconds);
            return Future.succeededFuture(newToken);
        } catch (Exception e) {
            logger.error("Failed to generate User JWT", e);
            return Future.failedFuture(new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "Token generation failed: " + e.getMessage()));
        }
    }
}
