package ziadatari.ReactiveAPI.web;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import ziadatari.ReactiveAPI.repository.EmployeeRepository;
import ziadatari.ReactiveAPI.service.EmployeeService;

// Repsonsible for Server lifecycle and wiring
public class HttpVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {

    // Config
    JsonObject dbConfig = config().getJsonObject("db");

    // Db pool
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
        .setHost(dbConfig.getString("host"))
        .setPort(dbConfig.getInteger("port"))
        .setDatabase(dbConfig.getString("database"))
        .setUser(dbConfig.getString("user"))
        .setPassword(dbConfig.getString("password"));

    // --- DATABASE CONNECTION POOL ---
    // We use a reactive pool (MySQLBuilder) to handle multiple concurrent queries
    // efficiently.
    // Pool size is 5: This means at most 5 database connections are open at once.
    // Small pool sizes prevent overwhelming the DB, but might cause queuing under
    // heavy load.
    Pool dbPool = MySQLBuilder.pool()
        .with(new PoolOptions().setMaxSize(5))
        .connectingTo(connectOptions)
        .using(vertx)
        .build();

    // --- CIRCUIT BREAKER CONFIGURATION ---
    // The Circuit Breaker protects the system from "cascading failures".
    // If the database is slow or down, the breaker "opens" and immediately returns
    // a fallback
    // instead of letting requests pile up and crash the server.
    CircuitBreakerOptions cbOptions = new CircuitBreakerOptions()
        .setMaxFailures(5) // Open the circuit after 5 consecutive failures
        .setTimeout(200) // If a DB call takes > 200ms, treat it as a failure
        .setFallbackOnFailure(true) // Enable falling back to a default "service unavailable" response
        .setResetTimeout(1000); // Wait 1 second before trying to "close" the circuit again

    CircuitBreaker circuitBreaker = CircuitBreaker.create("api-circuit-breaker", vertx, cbOptions);

    // CIRCUIT BREAKER LIFECYCLE LOGGING
    // Open: Breaker is active, requests are blocked/failed immediately.
    // Half-Open: Breaker allows ONE test request to see if the service recovered.
    // Closed: Normal operation, everything is working.
    circuitBreaker.openHandler(v -> System.out.println("CIRCUIT BREAKER: OPENED (Service failing or timing out)"));
    circuitBreaker.closeHandler(v -> System.out.println("CIRCUIT BREAKER: CLOSED (Service recovered)"));
    circuitBreaker.halfOpenHandler(v -> System.out.println("CIRCUIT BREAKER: HALF-OPEN (Testing recovery...)"));

    // --- DEPENDENCY WIRING ---
    // Repo -> service -> controller
    EmployeeRepository repository = new EmployeeRepository(dbPool);
    EmployeeService service = new EmployeeService(repository, circuitBreaker);
    EmployeeController controller = new EmployeeController(service);

    // --- ROUTER & MIDDLEWARE ---
    Router router = Router.router(vertx);

    // Global Rate Limiting: 25 requests per second (1000ms window)
    // This is the first line of defense, applied even before authentication or body
    // parsing.
    router.route().handler(new RateLimitHandler(vertx, 25, 1000));

    // BodyHandler: Required for parsing JSON bodies in POST/PUT requests.
    router.route().handler(BodyHandler.create());

    // Clean route definitions
    router.get("/employees").handler(controller::getAll);
    router.post("/employees").handler(controller::create);
    router.put("/employees/:id").handler(controller::update);
    router.delete("/employees/:id").handler(controller::delete);

    // Start Server
    vertx.createHttpServer()
        .requestHandler(router)
        .listen(config().getInteger("http.port"), http -> {
          if (http.succeeded()) {
            System.out.println("HTTP server started on port " + http.result().actualPort());
            startPromise.complete();
          } else {
            startPromise.fail(http.cause());
          }
        });

  }

}
