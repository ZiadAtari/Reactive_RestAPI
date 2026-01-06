package ziadatari.ReactiveAPI.main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import ziadatari.ReactiveAPI.web.HttpVerticle;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {

    // DB password handler
    String dbPassword = System.getenv("DB_PASSWORD");
    if (dbPassword == null || dbPassword.isEmpty()) {
      System.out.println("DB_PASSWORD IS NULL");
      dbPassword = "secret";
    }

    // Config
    // Hardcoded for testing purposes
    JsonObject dbconfig = new JsonObject()
      .put("host", "localhost")
      .put("port", 3306)
      .put("database", "payroll_db")
      .put("user", "root")
      .put("password", dbPassword);

    JsonObject appConfig = new JsonObject()
      .put("http.port", 8888)
      .put("url", "http://localhost:8888")
      .put("db", dbconfig);

    //DeploymentOptions allows passing config to child verticle
    DeploymentOptions options = new DeploymentOptions().setConfig(appConfig);

    // Deployment
    // deploying httpverticle that is async
    vertx.deployVerticle(HttpVerticle.class.getName(), options)
      .onSuccess(id -> {
        System.out.println("------------------------------------------------------------");
        System.out.println("APPLICATION STARTED SUCCESSFULLY");
        System.out.println("Deployment ID: " + id);
        System.out.println("REST API URL: " + appConfig.getString("url"));
        System.out.println("------------------------------------------------------------");
        startPromise.complete();
      })
      .onFailure(err -> {
        // Failure
        // if server fails to bind, faile whole program
        System.err.println("Failed to deploy verticle: " + err.getMessage());
        startPromise.fail(err);
      });

  }
}
