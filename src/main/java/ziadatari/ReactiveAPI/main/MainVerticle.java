package ziadatari.ReactiveAPI.main;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;

import io.vertx.core.json.JsonObject;
import ziadatari.ReactiveAPI.web.HttpVerticle;

/**
 * Main entry point for the Reactive REST API application.
 * Responsible for initializing configuration and deploying the application's
 * verticles.
 */
public class MainVerticle extends AbstractVerticle {

  /**
   * Starts the MainVerticle.
   * Initializes database and application configurations, and deploys the
   * EmployeeVerticle
   * and HttpVerticle in a specific order.
   *
   * @param startPromise a promise to signal the success or failure of the
   *                     deployment
   */
  @Override
  public void start(Promise<Void> startPromise) {

    // Retrieve database password from environment variables for security.
    // Defaults to 'secret' if not provided (for development purposes).
    String dbPassword = System.getenv("DB_PASSWORD");
    if (dbPassword == null || dbPassword.isEmpty()) {
      System.out.println("LOG: DB_PASSWORD environment variable is not set. Using default.");
      dbPassword = "secret";
    }

    // Database connection configuration
    JsonObject dbconfig = new JsonObject()
        .put("host", "localhost")
        .put("port", 3306)
        .put("database", "payroll_db")
        .put("user", "root")
        .put("password", dbPassword);

    // Main application configuration including HTTP port and base URL
    JsonObject appConfig = new JsonObject()
        .put("http.port", 8888)
        .put("url", "http://localhost:8888")
        .put("db", dbconfig);

    // DeploymentOptions allows passing config to child verticles
    DeploymentOptions options = new DeploymentOptions().setConfig(appConfig);

    // Sequential deployment:
    // 1. Deploy EmployeeVerticle first (handles DB operations and business logic)
    vertx.deployVerticle(ziadatari.ReactiveAPI.repository.EmployeeVerticle.class.getName(), options)
        .flatMap(id -> {
          // 2. Then deploy HttpVerticle (handles incoming HTTP requests)
          // Only proceeds if EmployeeVerticle deployment was successful.
          return vertx.deployVerticle(HttpVerticle.class.getName(), options);
        })
        .onSuccess(id -> {
          // Log success and complete the start promise
          System.out.println("------------------------------------------------------------");
          System.out.println("APPLICATION STARTED SUCCESSFULLY");
          System.out.println("REST API URL: " + appConfig.getString("url"));
          System.out.println("------------------------------------------------------------");
          startPromise.complete();
        })
        .onFailure(err -> {
          // Log failure details and fail the start promise
          System.err.println("CRITICAL: Failed to deploy verticles: " + err.getMessage());
          startPromise.fail(err);
        });

  }
}
