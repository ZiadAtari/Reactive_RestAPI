package ziadatari.ReactiveAPI.web;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.exception.ServiceException;

public class RateLimitHandler implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final int limit;
    private final long periodMs;

    public RateLimitHandler(Vertx vertx, int limit, long periodMs) {
        this.vertx = vertx;
        this.limit = limit;
        this.periodMs = periodMs;
    }

    @Override
    public void handle(RoutingContext ctx) {
        String ip = ctx.request().remoteAddress().host();
        LocalMap<String, JsonObject> map = vertx.sharedData().getLocalMap("rate.limit");

        JsonObject data = map.computeIfAbsent(ip, k -> new JsonObject()
                .put("count", 0)
                .put("reset", System.currentTimeMillis() + periodMs));

        long reset = data.getLong("reset");
        int count = data.getInteger("count");
        long now = System.currentTimeMillis();

        if (now > reset) {
            // Reset window
            count = 1;
            reset = now + periodMs;
            data.put("count", count);
            data.put("reset", reset);
            map.put(ip, data);
            ctx.next();
        } else {
            if (count >= limit) {
                System.out.println("RATE LIMIT EXCEEDED for IP: " + ip + " (Current count: " + count + ")");
                // Use centralized error handling format
                GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.TOO_MANY_REQUESTS));
            } else {
                data.put("count", count + 1);
                map.put(ip, data);
                ctx.next();
            }
        }
    }
}
