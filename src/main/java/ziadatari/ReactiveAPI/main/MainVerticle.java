package ziadatari.ReactiveAPI.main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class MainVerticle extends AbstractVerticle {

  private MySQLPool client;

  @Override
  public void start(Promise<Void> startPromise) {
    // 1. Configure Database
    MySQLConnectOptions connectOptions = new MySQLConnectOptions()
      .setPort(3306)
      .setHost("localhost")
      .setDatabase("testdb")
      .setUser("root")
      .setPassword("password");

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);
    client = MySQLPool.pool(vertx, connectOptions, poolOptions);

    // 2. Create Router
    Router router = Router.router(vertx);

    // Essential for reading JSON bodies in POST requests
    router.route().handler(BodyHandler.create());

    // 3. Define Routes (The API endpoints)
    router.get("/api/users").handler(this::getAllUsers);
    router.post("/api/users").handler(this::createUser);

    // 4. Start Server
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8888, http -> {
        if (http.succeeded()) {
          startPromise.complete();
          System.out.println("HTTP server started on port 8888");
        } else {
          startPromise.fail(http.cause());
        }
      });
  }

  // --- Handlers (The Logic) ---

  private void getAllUsers(RoutingContext ctx) {
    // Reactive SQL query
    client
      .query("SELECT * FROM users")
      .execute(ar -> {
        if (ar.succeeded()) {
          RowSet<Row> result = ar.result();
          JsonArray response = new JsonArray();
          for (Row row : result) {
            response.add(new JsonObject()
              .put("id", row.getInteger("id"))
              .put("username", row.getString("username")));
          }
          ctx.json(response); // Automatically sends 200 OK + JSON
        } else {
          System.out.println("Failure: " + ar.cause().getMessage());
          ctx.fail(500);
        }
      });
  }

  private void createUser(RoutingContext ctx) {
    JsonObject body = ctx.body().asJsonObject();
    String username = body.getString("username");

    // Prepared statement to prevent SQL injection
    client
      .preparedQuery("INSERT INTO users (username) VALUES (?)")
      .execute(io.vertx.sqlclient.Tuple.of(username), ar -> {
        if (ar.succeeded()) {
          ctx.response().setStatusCode(201).end("Created");
        } else {
          ctx.fail(500);
        }
      });
  }
}
