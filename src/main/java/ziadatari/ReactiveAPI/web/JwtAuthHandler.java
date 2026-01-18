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

    public JwtAuthHandler(Vertx vertx, JsonObject config) {
        String publicKey = config.getString("rsa_public_key");
        if (publicKey == null) {
            throw new IllegalStateException("RSA Public Key not configured");
        }

        // Normalize public key
        publicKey = PemUtils.normalizePem(publicKey, "-----BEGIN PUBLIC KEY-----", "-----END PUBLIC KEY-----");

        JWTAuthOptions options = new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("RS256")
                        .setBuffer(publicKey));

        this.jwtAuth = JWTAuth.create(vertx, options);
    }

    @Override
    public void handle(RoutingContext ctx) {
        String authHeader = ctx.request().getHeader("Authorization");
        logger.debug("JwtAuthHandler hit for path: {}. Auth Header: {}", ctx.request().path(),
                (authHeader != null ? "Present" : "Missing"));

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            logger.warn("Request missing Bearer token for protected route: {}", ctx.request().path());
            GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.UNAUTHORIZED));
            return;
        }

        String token = authHeader.substring(7);

        jwtAuth.authenticate(new JsonObject().put("token", token))
                .onSuccess(user -> {
                    // Token is valid
                    ctx.next();
                })
                .onFailure(err -> {
                    logger.warn("JWT Verification failed: {}", err.getMessage());
                    GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.UNAUTHORIZED));
                });
    }
}
