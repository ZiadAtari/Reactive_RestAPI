package ziadatari.ReactiveAPI.web;


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

    // Wiring: Repo -> service -> controller
    EmployeeRepository repository = new EmployeeRepository(dbPool);
    EmployeeService service = new EmployeeService(repository);
    EmployeeController controller = new EmployeeController(service);

    // Router Setup
    Router router = Router.router(vertx);
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
