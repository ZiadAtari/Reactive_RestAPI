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
import io.vertx.ext.healthchecks.HealthCheckHandler;

import io.vertx.micrometer.PrometheusScrapingHandler;

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
    // Initialize Circuit Breakers first
    // 1. Login Circuit Breaker (Protecting Auth Pipeline)
    // Timeout: 1000ms (BCrypt is slow), Reset: 2000ms, Failures: 5
    CustomCircuitBreaker loginCB = new CustomCircuitBreaker(vertx, "auth-login", 1000, 2000, 5);
    AuthController authController = new AuthController(vertx, loginCB);

    // --- ROUTER & MIDDLEWARE ---
    Router router = Router.router(vertx);

    // 1. BodyHandler: Essential for reading JSON bodies from incoming requests.
    router.route().handler(BodyHandler.create());

    // --- METRICS ---
    router.route("/metrics").handler(PrometheusScrapingHandler.create());

    // --- HEALTH CHECKS ---
    HealthCheckHandler healthCheckHandler = HealthCheckHandler.create(vertx);
    healthCheckHandler.register("server-live", promise -> promise.complete(io.vertx.ext.healthchecks.Status.OK()));
    router.get("/health/live").handler(healthCheckHandler);

    // 2. RateLimitHandler: Limits requests per IP (100 reqs per 1000ms window).
    router.route().handler(new RateLimitHandler(vertx, 100, 1000));

    // 3. Routing and Middleware setup
    // Verification Circuit Breakers (Split for V1 and V3 isolation)
    CustomCircuitBreaker v1VerificationCB = new CustomCircuitBreaker(vertx, "v1-verify", 500, 800, 5);
    CustomCircuitBreaker v3VerificationCB = new CustomCircuitBreaker(vertx, "v3-verify", 500, 800, 5);

    // V1 Middlewares: No Auth verification calling /v1/ip
    router.route("/v1/*").handler(new VerificationHandler(webClient, v1VerificationCB, "/v1/ip", false));

    // V3 Middlewares: Auth verification calling /v3/ip
    router.route("/v3/*").handler(new VerificationHandler(webClient, v3VerificationCB, "/v3/ip", true));

    // JWT Auth Handler for protected routes
    JwtAuthHandler jwtAuthHandler = new JwtAuthHandler(vertx, config());

    // --- API ROUTES ---

    // Login
    router.post("/login").handler(authController::login);

    // V3 (Authenticated)
    router.get("/v3/employees").handler(controller::getAll);
    // Protected Mutations
    router.post("/v3/employees").handler(jwtAuthHandler).handler(ctx -> {
      logger.debug("Executing POST /v3/employees (Auth passed)");
      controller.create(ctx);
    });
    router.put("/v3/employees/:id").handler(jwtAuthHandler).handler(ctx -> {
      logger.debug("Executing PUT /v3/employees (Auth passed)");
      controller.update(ctx);
    });
    router.delete("/v3/employees/:id").handler(jwtAuthHandler).handler(ctx -> {
      logger.debug("Executing DELETE /v3/employees (Auth passed)");
      controller.delete(ctx);
    });

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
