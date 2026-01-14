package ziadatari.ReactiveAPI.web;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.exception.ServiceException;

/**
 * Middleware handler for verifying the client's IP address against an external
 * service.
 * Acts as a basic security/authorization filter.
 */
public class VerificationHandler implements Handler<RoutingContext> {

    private final WebClient webClient;
    private final CustomCircuitBreaker circuitBreaker;
    private final ziadatari.ReactiveAPI.service.TokenService tokenService;
    private final String verificationPath;
    private final boolean requireAuth;

    /**
     * Constructs the handler with dependencies.
     *
     * @param webClient        client to make external requests
     * @param circuitBreaker   circuit breaker to protect against external failures
     * @param tokenService     service to provide auth tokens (can be null if
     *                         requireAuth is false)
     * @param verificationPath the path to call on the demo service (e.g. /v1/ip or
     *                         /v3/ip)
     * @param requireAuth      whether to include a JWT token in the request
     */
    public VerificationHandler(WebClient webClient, CustomCircuitBreaker circuitBreaker,
            ziadatari.ReactiveAPI.service.TokenService tokenService, String verificationPath, boolean requireAuth) {
        this.webClient = webClient;
        this.circuitBreaker = circuitBreaker;
        this.tokenService = tokenService;
        this.verificationPath = verificationPath;
        this.requireAuth = requireAuth;
    }

    /**
     * Handles IP verification.
     *
     * @param ctx the routing context
     */
    @Override
    public void handle(RoutingContext ctx) {
        String ip = ctx.request().remoteAddress().host();

        if (requireAuth) {
            tokenService.getToken()
                    .onSuccess(token -> performVerification(ctx, ip, token))
                    .onFailure(err -> GlobalErrorHandler.handle(ctx,
                            new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR, "Auth Token Error")));
        } else {
            performVerification(ctx, ip, null);
        }
    }

    private void performVerification(RoutingContext ctx, String ip, String token) {
        circuitBreaker.execute(() -> {
            var request = webClient.get(8080, "localhost", verificationPath)
                    .addQueryParam("address", ip);

            // Inject Bearer Token if available (for /v3/ip)
            if (token != null) {
                request.putHeader("Authorization", "Bearer " + token);
            }

            return request.send()
                    .map(response -> {
                        // Fail Fast logic: Map HTTP 500s to RuntimeExceptions to trip the Circuit
                        // Breaker
                        if (response.statusCode() >= 500) {
                            throw new RuntimeException("External Service Error: " + response.statusCode());
                        }
                        return response;
                    });
        }).onSuccess(response -> {
            JsonObject body = null;
            try {
                body = response.bodyAsJsonObject();
            } catch (Exception e) {
                // Silent catch
            }

            if (body != null && "Success".equals(body.getString("message"))) {
                ctx.next();
            } else if (body != null && body.getString("message", "").startsWith("Failure")) {
                GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.IP_VERIFICATION_FAILED));
            } else {
                GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                        "Verification service unavailable (invalid response)"));
            }
        }).onFailure(err -> {
            GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Verification service error: " + err.getMessage()));
        });
    }
}
