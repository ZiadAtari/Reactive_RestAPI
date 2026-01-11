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

    /**
     * Handles the rate limiting logic for each incoming request.
     * It identifies users by IP address and uses a 'Fixed Window' algorithm.
     */
    @Override
    public void handle(RoutingContext ctx) {
        // 1. Identify the caller by their source IP address.
        // In a real-world scenario with a load balancer, you might use
        // 'X-Forwarded-For'.
        String ip = ctx.request().remoteAddress().host();

        // TEST ENV FOR CIRCUIT BREAKER
        // String xForwardedFor = ctx.request().getHeader("X-Forwarded-For");
        // String ip = (xForwardedFor != null) ? xForwardedFor.split(",")[0].trim() :
        // ctx.request().remoteAddress().host();

        // 2. Access Vert.x SharedData LocalMap.
        // This map is thread-safe and shared across all Event Loop threads in this
        // Verticle instance.
        LocalMap<String, JsonObject> map = vertx.sharedData().getLocalMap("rate.limit");

        // 3. Atomic Update Loop (Prevents Race Conditions)
        // Since multiple threads can handle requests from the same IP, we use a loop
        // with
        // map.replace(key, old, new) to ensure we don't overwrite concurrent
        // increments.
        boolean updated = false;
        while (!updated) {
            JsonObject data = map.get(ip);
            long now = System.currentTimeMillis();

            // Case A: IP is new or the current time window has expired.
            if (data == null || now > data.getLong("reset")) {
                JsonObject newData = new JsonObject()
                        .put("count", 1) // Start fresh
                        .put("reset", now + periodMs); // Set next reset boundary

                if (data == null) {
                    // Atomic put-if-absent: returns null if the key was empty.
                    updated = (map.putIfAbsent(ip, newData) == null);
                } else {
                    // Atomic replace: only succeeds if 'data' hasn't changed since we read it.
                    updated = map.replace(ip, data, newData);
                }

                if (updated) {
                    ctx.next(); // Continue to next handler (e.g., Auth or Controller)
                }
            }
            // Case B: We are within an existing window.
            else {
                int count = data.getInteger("count");
                long reset = data.getLong("reset");

                // Check if the threshold (e.g., 25 requests) has been reached.
                if (count >= limit) {
                    long waitMs = reset - now;
                    System.out.println("RATE LIMIT EXCEEDED for IP: " + ip +
                            " (Count: " + count + "/" + limit + " Reset in: " + waitMs + "ms)");

                    // Respond with 429 Too Many Requests using the GlobalErrorHandler.
                    GlobalErrorHandler.handle(ctx, new ServiceException(ErrorCode.TOO_MANY_REQUESTS));
                    updated = true; // Break the loop
                }
                // Threshold not reached, increment the counter.
                else {
                    JsonObject newData = data.copy().put("count", count + 1);
                    // Atomic replace ensures that if another thread incremented 'count'
                    // in parallel, our replace will fail and we will retry the loop.
                    if (map.replace(ip, data, newData)) {
                        updated = true;
                        ctx.next();
                    }
                }
            }
        }
    }
}
