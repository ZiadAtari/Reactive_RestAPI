package ziadatari.ReactiveAPI.auth;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.exception.ServiceException;
import ziadatari.ReactiveAPI.web.CustomCircuitBreaker;

/**
 * Middleware handler for verifying the client's IP address against an external
 * service.
 * Acts as a basic security/authorization filter.
 */
public class VerificationHandler implements Handler<RoutingContext> {

    private final WebClient webClient;
    private final CustomCircuitBreaker circuitBreaker;
    private final String verificationPath;
    private final String host;
    private final int port;
    private final boolean requireAuth;

    /**
     * Constructs the handler with dependencies.
     *
     * @param webClient        client to make external requests
     * @param circuitBreaker   circuit breaker to protect against external failures
     * @param verificationPath the path to call on the demo service (e.g. /v1/ip or
     *                         /v3/ip)
     * @param host             the hostname of the verification service
     * @param port             the port of the verification service
     * @param requireAuth      whether to include a JWT token in the request
     */
    public VerificationHandler(WebClient webClient, CustomCircuitBreaker circuitBreaker,
            String verificationPath, String host, int port, boolean requireAuth) {
        this.webClient = webClient;
        this.circuitBreaker = circuitBreaker;
        this.verificationPath = verificationPath;
        this.host = host;
        this.port = port;
        this.requireAuth = requireAuth;
    }

    /**
     * Handles IP verification.
     * <p>
     * Flow:
     * 1. Extract IP address.
     * 2. If authentication is required (e.g., v3 routes), fetch a token via Event
     * Bus.
     * 3. Proceed to execute the verification against the external service.
     * </p>
     *
     * @param ctx the routing context
     */
    @Override
    public void handle(RoutingContext ctx) {
        String ip = ctx.request().remoteAddress().host();

        if (requireAuth) {
            // Fetch token asynchronously via Event Bus from AuthVerticle
            ctx.vertx().eventBus().request("auth.token.get", null, reply -> {
                if (reply.succeeded()) {
                    String token = (String) reply.result().body();
                    performVerification(ctx, ip, token);
                } else {
                    GlobalErrorHandler.handle(ctx,
                            new ServiceException(ErrorCode.INTERNAL_SERVER_ERROR,
                                    "Auth Token Error: " + reply.cause().getMessage()));
                }
            });
        } else {
            performVerification(ctx, ip, null);
        }
    }

    /**
     * Executes the external service call wrapped in a Circuit Breaker.
     *
     * @param ctx   the routing context
     * @param ip    the IP address to verify
     * @param token the JWT token (optional)
     */
    private void performVerification(RoutingContext ctx, String ip, String token) {
        circuitBreaker.execute(() -> {
            var request = webClient.get(port, host, verificationPath)
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
            // Circuit Breaker Execution Success (Note: The HTTP request succeeded, but the
            // content might be failure)
            JsonObject body = null;
            try {
                body = response.bodyAsJsonObject();
            } catch (Exception e) {
                // Silent catch: body might be empty or invalid
            }

            if (body != null && "Success".equals(body.getString("message"))) {
                // Verification passed
                ctx.next();
            } else if (body != null && body.getString("message", "").startsWith("Failure")) {
                // Verification explicitly failed (e.g. business logic refusal)
                GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.IP_VERIFICATION_FAILED));
            } else {
                // Unknown response or non-failure message that isn't success
                GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                        "Verification service unavailable (invalid response)"));
            }
        }).onFailure(err -> {
            // Circuit Breaker Failure (Timeout, Open State, or Exception)
            GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                    "Verification service error: " + err.getMessage()));
        });
    }
}
