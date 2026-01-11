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

    // Pool size is 5
    Pool dbPool = MySQLBuilder.pool()
        .with(new PoolOptions().setMaxSize(5))
        .connectingTo(connectOptions)
        .using(vertx)
        .build();

    // Circuit Breaker
    CircuitBreakerOptions cbOptions = new CircuitBreakerOptions()
        .setMaxFailures(5)
        .setTimeout(2000)
        .setFallbackOnFailure(true)
        .setResetTimeout(10000);

    CircuitBreaker circuitBreaker = CircuitBreaker.create("api-circuit-breaker", vertx, cbOptions);

    // CIRCUIT BREAKER LOGGING
    circuitBreaker.openHandler(v -> System.out.println("CIRCUIT BREAKER: OPENED (Service failing or timing out)"));
    circuitBreaker.closeHandler(v -> System.out.println("CIRCUIT BREAKER: CLOSED (Service recovered)"));
    circuitBreaker.halfOpenHandler(v -> System.out.println("CIRCUIT BREAKER: HALF-OPEN (Testing recovery...)"));

    // Wiring: Repo -> service -> controller
    EmployeeRepository repository = new EmployeeRepository(dbPool);
    EmployeeService service = new EmployeeService(repository, circuitBreaker);
    EmployeeController controller = new EmployeeController(service);

    // Router Setup
    Router router = Router.router(vertx);
    // Rate Limiting: 100 requests per minute
    router.route().handler(new RateLimitHandler(vertx, 10, 2000));
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
