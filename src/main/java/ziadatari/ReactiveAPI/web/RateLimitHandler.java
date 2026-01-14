package ziadatari.ReactiveAPI.web;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.LocalMap;
import io.vertx.ext.web.RoutingContext;
import ziadatari.ReactiveAPI.exception.ErrorCode;
import ziadatari.ReactiveAPI.exception.GlobalErrorHandler;
import ziadatari.ReactiveAPI.exception.ServiceException;

/**
 * Middleware handler for implementing rate limiting.
 * Uses a fixed-window algorithm with atomic updates to ensure safety across
 * multi-threaded Event Loops. Data is shared via Vert.x SharedData.
 */
public class RateLimitHandler implements Handler<RoutingContext> {

    private final Vertx vertx;
    private final int limit;
    private final long periodMs;

    /**
     * Constructs the handler.
     *
     * @param vertx    the Vertx instance
     * @param limit    max requests allowed per window
     * @param periodMs duration of the window in milliseconds
     */
    public RateLimitHandler(Vertx vertx, int limit, long periodMs) {
        this.vertx = vertx;
        this.limit = limit;
        this.periodMs = periodMs;
    }

    /**
     * Handles the rate limiting logic for each incoming request.
     * Uses atomic 'replace' operations in a retry loop to prevent race conditions
     * without blocking threads.
     *
     * @param ctx the routing context
     */
    @Override
    public void handle(RoutingContext ctx) {
        // 1. Identify caller by IP address
        String ip = ctx.request().remoteAddress().host();

        // 2. Access thread-safe SharedData Map
        LocalMap<String, JsonObject> map = vertx.sharedData().getLocalMap("rate.limit");

        // 3. Atomic Update Loop
        // We use Compare-and-Swap (CAS) semantics via map.replace() to handle
        // concurrency.
        // If another thread updates the map for this IP while we are calculating,
        // map.replace() returns false, and we loop again to get the fresh data.
        boolean updated = false;
        while (!updated) {
            JsonObject data = map.get(ip);
            long now = System.currentTimeMillis();

            // Case A: New window or first-time requester
            if (data == null || now > data.getLong("reset")) {
                JsonObject newData = new JsonObject()
                        .put("count", 1)
                        .put("reset", now + periodMs);

                if (data == null) {
                    updated = (map.putIfAbsent(ip, newData) == null);
                } else {
                    updated = map.replace(ip, data, newData);
                }

                if (updated) {
                    ctx.next();
                }
            }
            // Case B: Existing window
            else {
                int count = data.getInteger("count");
                long reset = data.getLong("reset");

                // Threshold check
                if (count >= limit) {
                    long waitMs = reset - now;
                    System.out.println("RATE LIMIT EXCEEDED for IP: " + ip +
                            " (Count: " + count + "/" + limit + " Reset in: " + waitMs + "ms)");

                    GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.TOO_MANY_REQUESTS));
                    updated = true; // Stop loop after rejection
                }
                // Under threshold: increment counter atomically
                else {
                    JsonObject newData = data.copy().put("count", count + 1);
                    if (map.replace(ip, data, newData)) {
                        updated = true;
                        ctx.next();
                    }
                }
            }
        }
    }
}
