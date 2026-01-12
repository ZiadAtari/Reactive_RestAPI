package ziadatari.ReactiveAPI.web;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.exception.ServiceException;

public class VerificationHandler implements Handler<RoutingContext> {

    private final WebClient webClient;

    public VerificationHandler(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String ip = ctx.request().remoteAddress().host();

        webClient.get(8080, "localhost", "/ip")
                .addQueryParam("address", ip)
                .send()
                .onSuccess(response -> {
                    JsonObject body = null;
                    try {
                        body = response.bodyAsJsonObject();
                    } catch (Exception e) {
                        // ignore
                    }

                    if (body != null && "Success".equals(body.getString("message"))) {
                        ctx.next();
                    } else if (body != null && body.getString("message", "").startsWith("Failure")) {
                        GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.IP_VERIFICATION_FAILED));
                    } else {
                        GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                                "Verification service unavailable"));
                    }
                })
                .onFailure(err -> {
                    GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.SERVICE_UNAVAILABLE,
                            "Verification service error: " + err.getMessage()));
                });
    }
}
