package ziadatari.ReactiveAPI.service;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.jwt.JWTAuth;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class Rs256TokenService implements TokenService {

    private final JWTAuth jwtAuth;
    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final AtomicLong expirationTime = new AtomicLong(0);

    // Refresh token 5 minutes before expiration
    private static final long REFRESH_BUFFER_SECONDS = 300;

    public Rs256TokenService(JWTAuth jwtAuth) {
        this.jwtAuth = jwtAuth;
    }

    @Override
    public Future<String> getToken() {
        long now = Instant.now().getEpochSecond();
        String currentToken = cachedToken.get();
        long currentExp = expirationTime.get();

        // High-performance read: Return cached token if it's still valid (with buffer)
        // This avoids locking and ensures instant response for 99% of requests
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
                .setIgnoreExpiration(false); // Ensure expiration is set in claims

        JsonObject claims = new JsonObject()
                .put("sub", "ReactiveAPI-Client"); // Subject

        String newToken = jwtAuth.generateToken(claims, options);

        // Update cache
        cachedToken.set(newToken);
        expirationTime.set(now + expiresInSeconds);

        System.out.println("Generated new JWT Token. Expires at: " + (now + expiresInSeconds));

        return Future.succeededFuture(newToken);
    }
}
