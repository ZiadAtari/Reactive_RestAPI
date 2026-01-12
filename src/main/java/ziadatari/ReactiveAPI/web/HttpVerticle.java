package ziadatari.ReactiveAPI.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.client.WebClient;

/**
 * Verticle responsible for running the HTTP server.
 * It sets up the web router, registers middleware (Rate Limiting, IP
 * Verification),
 * and defines the API endpoints.
 */
public class HttpVerticle extends AbstractVerticle {

  /**
   * Starts the HTTP server.
   * Configures the routing logic and wires dependencies like the WebClient.
   *
   * @param startPromise a promise to signal success or failure of server startup
   */
  @Override
  public void start(Promise<Void> startPromise) {

    // --- WEB CLIENT ---
    // Shared client for making external HTTP calls (e.g., verification service)
    WebClient webClient = WebClient.create(vertx);

    // --- CONTROLLER ---
    EmployeeController controller = new EmployeeController(vertx);

    // --- ROUTER & MIDDLEWARE ---
    Router router = Router.router(vertx);

    // 1. BodyHandler: Essential for reading JSON bodies from incoming requests.
    router.route().handler(BodyHandler.create());

    // 2. RateLimitHandler: Limits requests per IP (e.g., 100 reqs per 1000ms
    // window).
    router.route().handler(new RateLimitHandler(vertx, 100, 1000));

    // 3. VerificationHandler: Performs IP-based authorization against an external
    // service.
    router.route().handler(new VerificationHandler(webClient));

    // --- API ROUTES ---
    router.get("/employees").handler(controller::getAll);
    router.post("/employees").handler(controller::create);
    router.put("/employees/:id").handler(controller::update);
    router.delete("/employees/:id").handler(controller::delete);

    // Create and start the HTTP server
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(config().getInteger("http.port"), http -> {
          if (http.succeeded()) {
            System.out.println("HTTP server started on port " + http.result().actualPort());
            startPromise.complete();
          } else {
            System.err.println("CRITICAL: HTTP server failed to start: " + http.cause().getMessage());
            startPromise.fail(http.cause());
          }
        });

  }
}
