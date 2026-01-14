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

    /**
     * Constructs the handler with a shared WebClient and CircuitBreaker.
     *
     * @param webClient      client to make external requests
     * @param circuitBreaker circuit breaker to protect against external failures
     */
    public VerificationHandler(WebClient webClient, CustomCircuitBreaker circuitBreaker) {
        this.webClient = webClient;
        this.circuitBreaker = circuitBreaker;
    }

    /**
     * Handles IP verification.
     * Calls an external service and allows/rejets the request based on the
     * response.
     *
     * @param ctx the routing context
     */
    @Override
    public void handle(RoutingContext ctx) {
        String ip = ctx.request().remoteAddress().host();

        circuitBreaker.execute(() -> webClient.get(8080, "localhost", "/ip")
                .addQueryParam("address", ip)
                .send()
                .map(response -> {
                    // Map 5xx errors to failures so the circuit breaker trips
                    if (response.statusCode() >= 500) {
                        throw new RuntimeException("External Service Error: " + response.statusCode());
                    }
                    return response;
                })).onSuccess(response -> {
                    JsonObject body = null;
                    try {
                        body = response.bodyAsJsonObject();
                    } catch (Exception e) {
                        // Silent catch: body might not be JSON
                    }

                    // Authorization logic based on service response message
                    if (body != null && "Success".equals(body.getString("message"))) {
                        ctx.next(); // Verified: proceed to controller
                    } else if (body != null && body.getString("message", "").startsWith("Failure")) {
                        GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.IP_VERIFICATION_FAILED));
                    } else {
                        // Service returned 200 but unexpected content, treat as UNAVAILABLE or just
                        // allow/block?
                        // Assuming block to be safe
                        GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                                "Verification service unavailable (invalid response)"));
                    }
                }).onFailure(err -> {
                    // Circuit Breaker Open, Timeout, or Connection Error
                    GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                            "Verification service error: " + err.getMessage()));
                });
    }
}
