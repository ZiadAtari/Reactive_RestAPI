package ziadatari.ReactiveAPI.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.auth.RateLimitHandler;
import ziadatari.ReactiveAPI.auth.VerificationHandler;

import io.vertx.ext.web.client.WebClientOptions;

/**
 * Verticle responsible for running the HTTP server.
 * It sets up the web router, registers middleware (Rate Limiting, IP
 * Verification),
 * and defines the API endpoints.
 */
public class HttpVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(HttpVerticle.class);

  public HttpVerticle() {
    // Default constructor
  }

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
    WebClientOptions options = new WebClientOptions()
        .setMaxPoolSize(100)
        .setConnectTimeout(2000)
        .setIdleTimeout(10);
    WebClient webClient = WebClient.create(vertx, options);

    // --- CONTROLLER ---
    EmployeeController controller = new EmployeeController(vertx);

    // --- ROUTER & MIDDLEWARE ---
    Router router = Router.router(vertx);

    // 1. BodyHandler: Essential for reading JSON bodies from incoming requests.
    router.route().handler(BodyHandler.create());

    // 2. RateLimitHandler: Limits requests per IP (100 reqs per 1000ms window).
    router.route().handler(new RateLimitHandler(vertx, 100, 1000));

    // 3. Routing and Middleware setup
    // Config: 2000ms timeout, 5000ms reset, 3 failures max
    CustomCircuitBreaker circuitBreaker = new CustomCircuitBreaker(vertx, 500, 800, 5);

    // V1 Middlewares: No Auth verification calling /v1/ip
    router.route("/v1/*").handler(new VerificationHandler(webClient, circuitBreaker, "/v1/ip", false));

    // V3 Middlewares: Auth verification calling /v3/ip
    router.route("/v3/*").handler(new VerificationHandler(webClient, circuitBreaker, "/v3/ip", true));

    // --- API ROUTES ---
    // V3 (Authenticated)
    router.get("/v3/employees").handler(controller::getAll);
    router.post("/v3/employees").handler(controller::create);
    router.put("/v3/employees/:id").handler(controller::update);
    router.delete("/v3/employees/:id").handler(controller::delete);

    // V1 (Legacy)
    router.get("/v1/employees").handler(controller::getAll);
    router.post("/v1/employees").handler(controller::create);
    router.put("/v1/employees/:id").handler(controller::update);
    router.delete("/v1/employees/:id").handler(controller::delete);

    // Create and start the HTTP server
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(config().getInteger("http.port"), http -> {
          if (http.succeeded()) {
            logger.info("HTTP server started on port {}", http.result().actualPort());
            startPromise.complete();
          } else {
            logger.error("CRITICAL: HTTP server failed to start", http.cause());
            startPromise.fail(http.cause());
          }
        });

  }
}
