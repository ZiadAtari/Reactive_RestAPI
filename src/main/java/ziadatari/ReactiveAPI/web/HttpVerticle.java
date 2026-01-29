package ziadatari.ReactiveAPI.web;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.openapi.RouterBuilder;
import io.vertx.micrometer.PrometheusScrapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ziadatari.ReactiveAPI.auth.RateLimitHandler;
import ziadatari.ReactiveAPI.auth.VerificationHandler;

/**
 * Verticle responsible for running the HTTP server.
 * Uses OpenAPI 3.0 RouterBuilder for contract-driven routing (v4.5 Update).
 */
public class HttpVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(HttpVerticle.class);

  public HttpVerticle() {
    // Default constructor
  }

  /**
   * Starts the HTTP server using OpenAPI RouterBuilder.
   *
   * @param startPromise a promise to signal success or failure of server startup
   */
  @Override
  public void start(Promise<Void> startPromise) {

    // --- WEB CLIENT ---
    WebClientOptions options = new WebClientOptions()
        .setMaxPoolSize(100)
        .setConnectTimeout(2000)
        .setIdleTimeout(10);
    WebClient webClient = WebClient.create(vertx, options);

    // --- CONTROLLERS ---
    EmployeeController controller = new EmployeeController(vertx);
    CustomCircuitBreaker loginCB = new CustomCircuitBreaker(vertx, "auth-login", 1000, 2000, 5);
    AuthController authController = new AuthController(vertx, loginCB);

    // --- CIRCUIT BREAKERS FOR VERIFICATION ---
    CustomCircuitBreaker v1VerificationCB = new CustomCircuitBreaker(vertx, "v1-verify", 500, 800, 5);
    CustomCircuitBreaker v3VerificationCB = new CustomCircuitBreaker(vertx, "v3-verify", 500, 800, 5);
    String verifyHost = config().getString("verification.host", "localhost");
    int verifyPort = config().getInteger("verification.port", 8080);

    // --- JWT AUTH HANDLER ---
    JwtAuthHandler jwtAuthHandler = new JwtAuthHandler(vertx, config());

    // --- OPENAPI ROUTER BUILDER ---
    RouterBuilder.create(vertx, "openapi.yaml")
        .onSuccess(routerBuilder -> {
          logger.info("OpenAPI specification loaded successfully");

          // --- OPERATION HANDLERS ---
          // Auth (no security required)
          routerBuilder.operation("login").handler(authController::login);

          // V1 (Legacy - no security required)
          routerBuilder.operation("getAllEmployeesV1").handler(controller::getAll);
          routerBuilder.operation("createEmployeeV1").handler(controller::create);
          routerBuilder.operation("updateEmployeeV1").handler(controller::update);
          routerBuilder.operation("deleteEmployeeV1").handler(controller::delete);

          // V3 (Authenticated - JWT required)
          // Note: We apply jwtAuthHandler before the controller handler for each
          // protected operation
          routerBuilder.operation("getAllEmployeesV3").handler(controller::getAll);
          routerBuilder.operation("createEmployeeV3").handler(jwtAuthHandler).handler(controller::create);
          routerBuilder.operation("updateEmployeeV3").handler(jwtAuthHandler).handler(controller::update);
          routerBuilder.operation("deleteEmployeeV3").handler(jwtAuthHandler).handler(controller::delete);

          // Health (defined in spec but simple handler)
          routerBuilder.operation("healthLive").handler(ctx -> {
            ctx.response()
                .putHeader("Content-Type", "application/json")
                .end("{\"outcome\": \"UP\"}");
          });

          // Metrics (operationId from OpenAPI spec)
          routerBuilder.operation("getMetrics").handler(PrometheusScrapingHandler.create());

          // --- BUILD OPENAPI ROUTER ---
          Router apiRouter = routerBuilder.createRouter();

          // --- MAIN ROUTER (for global middleware and infrastructure) ---
          Router mainRouter = Router.router(vertx);

          // 1. BodyHandler: Essential for reading JSON bodies
          mainRouter.route().handler(BodyHandler.create());

          // 2. Swagger UI Static Files (v4.6 Update)
          // Redirect /swagger to /swagger/index.html
          mainRouter.route("/swagger").handler(ctx -> {
            ctx.response()
                .setStatusCode(302)
                .putHeader("Location", "/swagger/index.html")
                .end();
          });
          // Serve static files from classpath
          mainRouter.route("/swagger/*").handler(
              StaticHandler.create("webroot/swagger")
                  .setCachingEnabled(false));

          // 3. Serve OpenAPI spec for Swagger UI
          mainRouter.route("/openapi.yaml").handler(ctx -> {
            ctx.response()
                .putHeader("Content-Type", "application/yaml")
                .sendFile("openapi.yaml");
          });

          // 4. RateLimitHandler: Global rate limiting
          mainRouter.route().handler(new RateLimitHandler(vertx, 100, 1000));

          // 5. Verification Handlers for V1 and V3 paths
          mainRouter.route("/v1/*")
              .handler(new VerificationHandler(webClient, v1VerificationCB, "/v1/ip", verifyHost, verifyPort, false));
          mainRouter.route("/v3/*")
              .handler(new VerificationHandler(webClient, v3VerificationCB, "/v3/ip", verifyHost, verifyPort, true));

          // 6. Mount the OpenAPI Router
          mainRouter.route("/*").subRouter(apiRouter);

          // --- START HTTP SERVER ---
          vertx.createHttpServer()
              .requestHandler(mainRouter)
              .listen(config().getInteger("http.port"), http -> {
                if (http.succeeded()) {
                  logger.info("HTTP server started on port {} (OpenAPI mode)", http.result().actualPort());
                  startPromise.complete();
                } else {
                  logger.error("CRITICAL: HTTP server failed to start", http.cause());
                  startPromise.fail(http.cause());
                }
              });
        })
        .onFailure(err -> {
          logger.error("CRITICAL: Failed to load OpenAPI specification", err);
          startPromise.fail(err);
        });
  }
}
