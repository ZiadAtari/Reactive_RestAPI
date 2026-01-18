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

    private Future<String> generateNewToken(long now) {
        // Expiration: 1 hour (3600 seconds) from now
        int expiresInSeconds = 3600;

        JWTOptions options = new JWTOptions()
                .setAlgorithm("RS256")
                .setExpiresInSeconds(expiresInSeconds)
                .setIgnoreExpiration(false);

        JsonObject claims = new JsonObject()
                .put("sub", "ReactiveAPI-Client");

        try {
            String newToken = jwtAuth.generateToken(claims, options);

            // Update cache
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
}
