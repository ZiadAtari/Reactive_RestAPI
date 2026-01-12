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

    /**
     * Constructs the handler with a shared WebClient.
     *
     * @param webClient client to make external requests
     */
    public VerificationHandler(WebClient webClient) {
        this.webClient = webClient;
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

        // Asynchronous call to verification service
        webClient.get(8080, "localhost", "/ip")
                .addQueryParam("address", ip)
                .send()
                .onSuccess(response -> {
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
                        // Service error or unexpected response
                        GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                                "Verification service unavailable"));
                    }
                })
                .onFailure(err -> {
                    // Connectivity issues with the verification service
                    GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                            "Verification service error: " + err.getMessage()));
                });
    }
}
